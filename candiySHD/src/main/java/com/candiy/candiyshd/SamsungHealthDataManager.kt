package com.candiy.candiyshd

import android.app.Activity
import android.content.Context
import com.candiy.candiyshd.data.enums.SamsungHealthDataType
import com.candiy.candiyshd.data.model.SamsungDeviceInfo
import com.candiy.candiyshd.data.model.SamsungHealthSnapshot
import com.candiy.candiyshd.data.model.SamsungHeartRateSample
import com.candiy.candiyshd.data.model.SamsungOxygenSaturationSample
import com.candiy.candiyshd.data.model.SamsungRawHealthChange
import com.candiy.candiyshd.data.model.SamsungRawHealthChangeDump
import com.candiy.candiyshd.data.model.SamsungRawHealthDataDump
import com.candiy.candiyshd.data.model.SamsungRawHealthDataPoint
import com.candiy.candiyshd.data.model.SamsungRawStepAggregate
import com.candiy.candiyshd.data.model.SamsungReadStats
import com.candiy.candiyshd.data.model.SamsungReadWindow
import com.candiy.candiyshd.data.model.SamsungSleepStage
import com.candiy.candiyshd.data.model.SamsungSleepSummary
import com.candiy.candiyshd.data.model.SamsungStepSummary
import com.candiy.candiyshd.permissions.SamsungHealthPermissions
import com.google.gson.GsonBuilder
import com.samsung.android.sdk.health.data.HealthDataService
import com.samsung.android.sdk.health.data.HealthDataStore
import com.samsung.android.sdk.health.data.data.Change
import com.samsung.android.sdk.health.data.data.DataPoint
import com.samsung.android.sdk.health.data.data.Field
import com.samsung.android.sdk.health.data.data.HealthDataPoint
import com.samsung.android.sdk.health.data.data.entries.HeartRate
import com.samsung.android.sdk.health.data.data.entries.OxygenSaturation
import com.samsung.android.sdk.health.data.data.entries.SleepSession
import com.samsung.android.sdk.health.data.device.DeviceGroup
import com.samsung.android.sdk.health.data.error.HealthDataException
import com.samsung.android.sdk.health.data.error.ResolvablePlatformException
import com.samsung.android.sdk.health.data.permission.Permission
import com.samsung.android.sdk.health.data.request.AggregateRequest
import com.samsung.android.sdk.health.data.request.ChangedDataRequest
import com.samsung.android.sdk.health.data.request.DataType
import com.samsung.android.sdk.health.data.request.DataTypes
import com.samsung.android.sdk.health.data.request.InstantTimeFilter
import com.samsung.android.sdk.health.data.request.LocalTimeFilter
import com.samsung.android.sdk.health.data.request.LocalTimeGroup
import com.samsung.android.sdk.health.data.request.LocalTimeGroupUnit
import com.samsung.android.sdk.health.data.request.Ordering
import com.samsung.android.sdk.health.data.request.ReadDataRequest
import com.samsung.android.sdk.health.data.request.ReadSourceFilter
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime

class SamsungHealthDataManager(context: Context) {
    private val applicationContext = context.applicationContext
    private val healthDataStore: HealthDataStore = HealthDataService.getStore(applicationContext)
    private val gson = GsonBuilder().setPrettyPrinting().create()

    private data class PagedRead(
        val dataPoints: List<HealthDataPoint>,
        val pages: Int,
        val hitPageCap: Boolean
    ) {
        fun stats() = SamsungReadStats(
            pages = pages,
            pointCount = dataPoints.size,
            hitPageCap = hitPageCap,
            truncatedByLimit = false
        )
    }

    fun initConnection(onResult: (Boolean) -> Unit) {
        onResult(true)
    }

    /**
     * Builds the seven-day read window used by [readRawDataAsJson].
     *
     * Seven calendar days: midnight six days back through [now]. Calendar boundaries are used
     * rather than a rolling 168 hours so the range lines up with the DAILY grouping
     * [readRawStepAggregates] already applies — otherwise a day's step total and that day's
     * heart rate samples would describe two different 24-hour spans.
     *
     * Sleep starts a day earlier. A session is matched on its start time, so a night that
     * began before [SamsungReadWindow.start] is dropped whole rather than clipped; reading
     * from one day back recovers it. The `startTime.minusDays(6)` in [readSnapshot] was an
     * ad-hoc version of the same correction.
     */
    fun sevenDayWindow(now: LocalDateTime = LocalDateTime.now()): SamsungReadWindow {
        val start = now.toLocalDate().minusDays(6).atStartOfDay()

        return SamsungReadWindow(
            start = start,
            end = now,
            sleepStart = start.minusDays(1)
        )
    }

    /**
     * Walks every page of a read request and returns the full result.
     *
     * [com.samsung.android.sdk.health.data.response.DataResponse.pageToken] is non-null while
     * more data remains. Reading only `dataList` yields a silently truncated prefix — no error,
     * no warning — which is invisible for a one-day window but not for a seven-day one.
     */
    private suspend fun readAllPages(
        newBuilder: () -> ReadDataRequest.DualTimeBuilder<HealthDataPoint>
    ): PagedRead {
        val dataPoints = mutableListOf<HealthDataPoint>()
        var pageToken: String? = null
        var pages = 0

        do {
            // The builder carries request state, so every page needs a fresh one.
            val builder = newBuilder()
            pageToken?.let { builder.setPageToken(it) }

            val response = healthDataStore.readData(builder.build())
            dataPoints += response.dataList
            pageToken = response.pageToken
            pages++
        } while (pageToken != null && pages < MAX_PAGES)

        return PagedRead(dataPoints, pages, hitPageCap = pageToken != null)
    }

    private fun readBuilder(
        dataType: DataType.Readable<HealthDataPoint, ReadDataRequest.DualTimeBuilder<HealthDataPoint>>,
        startTime: LocalDateTime,
        endTime: LocalDateTime,
        watchOnly: Boolean
    ): () -> ReadDataRequest.DualTimeBuilder<HealthDataPoint> = {
        val builder = dataType.readDataRequestBuilder
            .setLocalTimeFilter(LocalTimeFilter.of(startTime, endTime))
            .setOrdering(Ordering.ASC)

        if (watchOnly) {
            builder.setSourceFilter(ReadSourceFilter.fromDeviceType(DeviceGroup.WATCH))
        }
        builder
    }

    suspend fun getGrantedPermissions(
        dataTypes: Set<SamsungHealthDataType> = SamsungHealthPermissions.defaultDataTypes,
    ): Set<Permission> {
        return healthDataStore.getGrantedPermissions(
            SamsungHealthPermissions.requiredPermissions(dataTypes)
        )
    }

    suspend fun hasAllPermissions(
        dataTypes: Set<SamsungHealthDataType> = SamsungHealthPermissions.defaultDataTypes
    ): Boolean {
        val requiredPermissions = SamsungHealthPermissions.requiredPermissions(dataTypes)
        return getGrantedPermissions(dataTypes).containsAll(requiredPermissions)
    }

    suspend fun requestPermissions(
        activity: Activity,
        dataTypes: Set<SamsungHealthDataType> = SamsungHealthPermissions.defaultDataTypes
    ): Set<Permission> {
        return healthDataStore.requestPermissions(
            SamsungHealthPermissions.requiredPermissions(dataTypes),
            activity
        )
    }

    suspend fun readSnapshot(
        startTime: LocalDateTime = LocalDateTime.now().minusDays(1),
        endTime: LocalDateTime = LocalDateTime.now(),
        watchOnly: Boolean = true
    ): SamsungHealthSnapshot {
        return SamsungHealthSnapshot(
            steps = readStepSummary(startTime, endTime),
            heartRates = readHeartRates(startTime, endTime, watchOnly),
            oxygenSaturations = readOxygenSaturations(startTime, endTime, watchOnly),
            sleepSessions = readSleepSessions(startTime.minusDays(6), endTime, watchOnly)
        )
    }

    /**
     * Dumps a read window as JSON.
     *
     * [limitPerType] is a debugging aid, not a page size: it trims the result *after* every page
     * has been read. Leave it null for a complete export — a non-null value that bites is
     * reported as `truncatedByLimit` in the dump's `stats` block.
     */
    suspend fun readRawDataAsJson(
        window: SamsungReadWindow = sevenDayWindow(),
        watchOnly: Boolean = true,
        limitPerType: Int? = null
    ): String {
        val heartRates = readPagedHeartRates(window.start, window.end, watchOnly)
        val oxygenSaturations = readPagedOxygenSaturations(window.start, window.end, watchOnly)
        val sleepSessions = readPagedSleepSessions(window.sleepStart, window.end, watchOnly)
        val steps = readRawStepAggregates(window.start, window.end)

        val deviceIds = listOf(heartRates, oxygenSaturations, sleepSessions)
            .flatMap { paged -> paged.dataPoints }
            .mapNotNull { dataPoint -> dataPoint.dataSource?.deviceId }
            .toSet()

        val dump = SamsungRawHealthDataDump(
            startTime = window.start.toString(),
            endTime = window.end.toString(),
            watchOnly = watchOnly,
            note = "Steps are exposed by Samsung Health Data SDK as aggregate TOTAL. Heart rate, " +
                "blood oxygen, and sleep are raw HealthDataPoint dumps. Sleep is read from " +
                "${window.sleepStart} so sessions that began before the window are not dropped.",
            stats = mapOf(
                "heartRates" to heartRates.stats().withLimit(limitPerType),
                "oxygenSaturations" to oxygenSaturations.stats().withLimit(limitPerType),
                "sleepSessions" to sleepSessions.stats().withLimit(limitPerType)
            ),
            devices = readDevices(deviceIds),
            steps = steps.applyLimit(limitPerType),
            heartRates = heartRates.toRawDataPoints(DataTypes.HEART_RATE).applyLimit(limitPerType),
            oxygenSaturations = oxygenSaturations.toRawDataPoints(DataTypes.BLOOD_OXYGEN)
                .applyLimit(limitPerType),
            sleepSessions = sleepSessions.toRawDataPoints(DataTypes.SLEEP).applyLimit(limitPerType)
        )

        return gson.toJson(dump)
    }

    suspend fun readRawChangesAsJson(
        startTime: Instant = Instant.now().minus(Duration.ofDays(7)),
        endTime: Instant = Instant.now(),
        limitPerType: Int? = null
    ): String {
        val dump = SamsungRawHealthChangeDump(
            startTime = startTime.toString(),
            endTime = endTime.toString(),
            note = "Samsung Health Data SDK exposes sync deltas through readChanges(). There is no SDK push callback in this test module.",
            heartRates = readRawHeartRateChanges(startTime, endTime).applyLimit(limitPerType),
            oxygenSaturations = readRawOxygenSaturationChanges(startTime, endTime).applyLimit(limitPerType),
            sleepSessions = readRawSleepChanges(startTime, endTime).applyLimit(limitPerType)
        )

        return gson.toJson(dump)
    }

    suspend fun readStepSummary(
        startTime: LocalDateTime,
        endTime: LocalDateTime
    ): SamsungStepSummary {
        val request: AggregateRequest<Long> = DataType.StepsType.TOTAL.requestBuilder
            .setLocalTimeFilterWithGroup(
                LocalTimeFilter.of(startTime, endTime),
                LocalTimeGroup.of(LocalTimeGroupUnit.DAILY, 1)
            )
            .setOrdering(Ordering.ASC)
            .build()

        val total = healthDataStore.aggregateData(request)
            .dataList
            .sumOf { aggregatedData -> aggregatedData.value ?: 0L }

        return SamsungStepSummary(
            count = total,
            startTime = startTime.toString(),
            endTime = endTime.toString()
        )
    }

    suspend fun readRawStepAggregates(
        startTime: LocalDateTime,
        endTime: LocalDateTime
    ): List<SamsungRawStepAggregate> {
        val request: AggregateRequest<Long> = DataType.StepsType.TOTAL.requestBuilder
            .setLocalTimeFilterWithGroup(
                LocalTimeFilter.of(startTime, endTime),
                LocalTimeGroup.of(LocalTimeGroupUnit.DAILY, 1)
            )
            .setOrdering(Ordering.ASC)
            .build()

        return healthDataStore.aggregateData(request)
            .dataList
            .map { aggregatedData ->
                SamsungRawStepAggregate(
                    operation = "DataType.StepsType.TOTAL",
                    value = aggregatedData.value,
                    startTime = aggregatedData.startTime?.toString(),
                    endTime = aggregatedData.endTime?.toString(),
                    startLocalDateTime = aggregatedData.getStartLocalDateTime()?.toString(),
                    endLocalDateTime = aggregatedData.getEndLocalDateTime()?.toString()
                )
            }
    }

    private suspend fun readPagedHeartRates(
        startTime: LocalDateTime,
        endTime: LocalDateTime,
        watchOnly: Boolean
    ): PagedRead = readAllPages(readBuilder(DataTypes.HEART_RATE, startTime, endTime, watchOnly))

    suspend fun readHeartRates(
        startTime: LocalDateTime,
        endTime: LocalDateTime,
        watchOnly: Boolean = true
    ): List<SamsungHeartRateSample> {
        return readPagedHeartRates(startTime, endTime, watchOnly)
            .dataPoints
            .flatMap { dataPoint -> dataPoint.toHeartRateSamples() }
    }

    suspend fun readRawHeartRateDataPoints(
        startTime: LocalDateTime,
        endTime: LocalDateTime,
        watchOnly: Boolean = true
    ): List<SamsungRawHealthDataPoint> {
        return readPagedHeartRates(startTime, endTime, watchOnly).toRawDataPoints(DataTypes.HEART_RATE)
    }

    suspend fun readRawHeartRateChanges(
        startTime: Instant,
        endTime: Instant
    ): List<SamsungRawHealthChange> {
        return readRawChanges(
            dataTypeName = DataTypes.HEART_RATE.name,
            fields = DataTypes.HEART_RATE.allFields,
            request = DataTypes.HEART_RATE.changedDataRequestBuilder
                .setChangeTimeFilter(InstantTimeFilter.of(startTime, endTime))
                .build()
        )
    }

    private suspend fun readPagedOxygenSaturations(
        startTime: LocalDateTime,
        endTime: LocalDateTime,
        watchOnly: Boolean
    ): PagedRead = readAllPages(readBuilder(DataTypes.BLOOD_OXYGEN, startTime, endTime, watchOnly))

    suspend fun readOxygenSaturations(
        startTime: LocalDateTime,
        endTime: LocalDateTime,
        watchOnly: Boolean = true
    ): List<SamsungOxygenSaturationSample> {
        return readPagedOxygenSaturations(startTime, endTime, watchOnly)
            .dataPoints
            .flatMap { dataPoint -> dataPoint.toOxygenSaturationSamples() }
    }

    suspend fun readRawOxygenSaturationDataPoints(
        startTime: LocalDateTime,
        endTime: LocalDateTime,
        watchOnly: Boolean = true
    ): List<SamsungRawHealthDataPoint> {
        return readPagedOxygenSaturations(startTime, endTime, watchOnly)
            .toRawDataPoints(DataTypes.BLOOD_OXYGEN)
    }

    suspend fun readRawOxygenSaturationChanges(
        startTime: Instant,
        endTime: Instant
    ): List<SamsungRawHealthChange> {
        return readRawChanges(
            dataTypeName = DataTypes.BLOOD_OXYGEN.name,
            fields = DataTypes.BLOOD_OXYGEN.allFields,
            request = DataTypes.BLOOD_OXYGEN.changedDataRequestBuilder
                .setChangeTimeFilter(InstantTimeFilter.of(startTime, endTime))
                .build()
        )
    }

    private suspend fun readPagedSleepSessions(
        startTime: LocalDateTime,
        endTime: LocalDateTime,
        watchOnly: Boolean
    ): PagedRead = readAllPages(readBuilder(DataTypes.SLEEP, startTime, endTime, watchOnly))

    suspend fun readSleepSessions(
        startTime: LocalDateTime,
        endTime: LocalDateTime,
        watchOnly: Boolean = true
    ): List<SamsungSleepSummary> {
        return readPagedSleepSessions(startTime, endTime, watchOnly)
            .dataPoints
            .map { dataPoint -> dataPoint.toSleepSummary() }
    }

    suspend fun readRawSleepDataPoints(
        startTime: LocalDateTime,
        endTime: LocalDateTime,
        watchOnly: Boolean = true
    ): List<SamsungRawHealthDataPoint> {
        return readPagedSleepSessions(startTime, endTime, watchOnly).toRawDataPoints(DataTypes.SLEEP)
    }

    suspend fun readRawSleepChanges(
        startTime: Instant,
        endTime: Instant
    ): List<SamsungRawHealthChange> {
        return readRawChanges(
            dataTypeName = DataTypes.SLEEP.name,
            fields = DataTypes.SLEEP.allFields,
            request = DataTypes.SLEEP.changedDataRequestBuilder
                .setChangeTimeFilter(InstantTimeFilter.of(startTime, endTime))
                .build()
        )
    }

    suspend fun readChangedSleepData(lastSyncTime: Instant, endTime: Instant = Instant.now()) =
        healthDataStore.readChanges(
            DataTypes.SLEEP.changedDataRequestBuilder
                .setChangeTimeFilter(InstantTimeFilter.of(lastSyncTime, endTime))
                .build()
        ).dataList

    fun resolveIfPossible(activity: Activity, throwable: Throwable): Boolean {
        val exception = (throwable as? ResolvablePlatformException) ?: return false
        if (!exception.hasResolution) {
            return false
        }
        exception.resolve(activity)
        return true
    }

    fun userReadableError(throwable: Throwable): String {
        val healthException = throwable as? HealthDataException
        return healthException?.let {
            "Samsung Health error ${it.errorCode}: ${it.errorMessage}"
        } ?: (throwable.message ?: throwable::class.java.simpleName)
    }

    private fun HealthDataPoint.toHeartRateSamples(): List<SamsungHeartRateSample> {
        val seriesData = getValueOrDefault(
            DataType.HeartRateType.SERIES_DATA,
            emptyList()
        )

        if (seriesData.isNotEmpty()) {
            return seriesData.mapIndexed { index, heartRate ->
                SamsungHeartRateSample(
                    beatsPerMinute = heartRate.heartRate,
                    minBeatsPerMinute = heartRate.min,
                    maxBeatsPerMinute = heartRate.max,
                    startTime = heartRate.startTime.toString(),
                    endTime = heartRate.endTime.toString(),
                    sourceAppId = dataSource?.appId,
                    sourceDeviceId = dataSource?.deviceId,
                    uid = "$uid:$index"
                )
            }
        }

        return listOf(
            SamsungHeartRateSample(
                beatsPerMinute = getValueOrDefault(DataType.HeartRateType.HEART_RATE, 0f),
                minBeatsPerMinute = getNullableValue(DataType.HeartRateType.MIN_HEART_RATE),
                maxBeatsPerMinute = getNullableValue(DataType.HeartRateType.MAX_HEART_RATE),
                startTime = startTime.toString(),
                endTime = endTime.toString(),
                sourceAppId = dataSource?.appId,
                sourceDeviceId = dataSource?.deviceId,
                uid = uid
            )
        )
    }

    private fun HealthDataPoint.toOxygenSaturationSamples(): List<SamsungOxygenSaturationSample> {
        val seriesData = getValueOrDefault(
            DataType.BloodOxygenType.SERIES_DATA,
            emptyList()
        )

        if (seriesData.isNotEmpty()) {
            return seriesData.mapIndexed { index, oxygenSaturation ->
                SamsungOxygenSaturationSample(
                    percentage = oxygenSaturation.oxygenSaturation,
                    minPercentage = oxygenSaturation.min,
                    maxPercentage = oxygenSaturation.max,
                    startTime = oxygenSaturation.startTime.toString(),
                    endTime = oxygenSaturation.endTime.toString(),
                    sourceAppId = dataSource?.appId,
                    sourceDeviceId = dataSource?.deviceId,
                    uid = "$uid:$index"
                )
            }
        }

        return listOf(
            SamsungOxygenSaturationSample(
                percentage = getValueOrDefault(DataType.BloodOxygenType.OXYGEN_SATURATION, 0f),
                minPercentage = getNullableValue(DataType.BloodOxygenType.MIN_OXYGEN_SATURATION),
                maxPercentage = getNullableValue(DataType.BloodOxygenType.MAX_OXYGEN_SATURATION),
                startTime = startTime.toString(),
                endTime = endTime.toString(),
                sourceAppId = dataSource?.appId,
                sourceDeviceId = dataSource?.deviceId,
                uid = uid
            )
        )
    }

    private fun HealthDataPoint.toSleepSummary(): SamsungSleepSummary {
        val sessions = getValueOrDefault(
            DataType.SleepType.SESSIONS,
            emptyList()
        )

        val stages = sessions.flatMap { session ->
            session.stages.orEmpty().map { stage ->
                SamsungSleepStage(
                    startTime = stage.startTime.toString(),
                    endTime = stage.endTime.toString(),
                    stage = stage.stage.name
                )
            }
        }

        return SamsungSleepSummary(
            startTime = startTime.toString(),
            endTime = endTime.toString(),
            duration = getNullableValue(DataType.SleepType.DURATION)?.toString(),
            sleepScore = getNullableValue(DataType.SleepType.SLEEP_SCORE),
            stages = stages,
            sourceAppId = dataSource?.appId,
            sourceDeviceId = dataSource?.deviceId,
            uid = uid
        )
    }

    private suspend fun readRawChanges(
        dataTypeName: String,
        fields: List<Field<*>>,
        request: ChangedDataRequest<HealthDataPoint>
    ): List<SamsungRawHealthChange> {
        return healthDataStore.readChanges(request)
            .dataList
            .map { change ->
                change.toRawChange(dataTypeName, fields)
            }
    }

    private fun Change<HealthDataPoint>.toRawChange(
        dataTypeName: String,
        fields: List<Field<*>>
    ): SamsungRawHealthChange {
        return SamsungRawHealthChange(
            dataType = dataTypeName,
            changeType = changeType.name,
            changeTime = changeTime?.toString(),
            deleteDataUid = deleteDataUid,
            upsertDataPoint = upsertDataPoint?.toRawDataPoint(dataTypeName, fields)
        )
    }

    private fun HealthDataPoint.toRawDataPoint(
        dataTypeName: String,
        fields: List<Field<*>>
    ): SamsungRawHealthDataPoint {
        return SamsungRawHealthDataPoint(
            dataType = dataTypeName,
            uid = uid,
            clientDataId = clientDataId,
            clientVersion = clientVersion,
            sourceAppId = dataSource?.appId,
            sourceDeviceId = dataSource?.deviceId,
            updateTime = updateTime?.toString(),
            startTime = startTime?.toString(),
            endTime = endTime?.toString(),
            zoneOffset = zoneOffset?.toString(),
            startLocalDateTime = getStartLocalDateTime()?.toString(),
            endLocalDateTime = getEndLocalDateTime()?.toString(),
            fields = fields.associate { field ->
                field.name to rawValueToJsonFriendly(getRawValue(field))
            }
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun HealthDataPoint.getRawValue(field: Field<*>): Any? {
        return try {
            getValue(field as Field<Any>)
        } catch (_: RuntimeException) {
            null
        }
    }

    private fun rawValueToJsonFriendly(value: Any?): Any? {
        return when (value) {
            null -> null
            is String, is Number, is Boolean -> value
            is Instant -> value.toString()
            is LocalDateTime -> value.toString()
            is Duration -> value.toString()
            is Enum<*> -> value.name
            is HeartRate -> mapOf(
                "heartRate" to value.heartRate,
                "min" to value.min,
                "max" to value.max,
                "startTime" to value.startTime.toString(),
                "endTime" to value.endTime.toString()
            )
            is OxygenSaturation -> mapOf(
                "oxygenSaturation" to value.oxygenSaturation,
                "min" to value.min,
                "max" to value.max,
                "startTime" to value.startTime.toString(),
                "endTime" to value.endTime.toString()
            )
            is SleepSession -> mapOf(
                "startTime" to value.startTime.toString(),
                "endTime" to value.endTime.toString(),
                "duration" to value.duration?.toString(),
                "stages" to value.stages.orEmpty().map { stage -> rawValueToJsonFriendly(stage) }
            )
            is SleepSession.SleepStage -> mapOf(
                "startTime" to value.startTime.toString(),
                "endTime" to value.endTime.toString(),
                "stage" to value.stage.name
            )
            is Iterable<*> -> value.map { item -> rawValueToJsonFriendly(item) }
            else -> value.toString()
        }
    }

    private fun <T> HealthDataPoint.getNullableValue(field: com.samsung.android.sdk.health.data.data.Field<T>): T? {
        return try {
            getValue(field)
        } catch (_: RuntimeException) {
            null
        }
    }

    /**
     * Resolves opaque `sourceDeviceId` values to real devices.
     *
     * A data point only carries the id, so two ids in the same dump are indistinguishable —
     * they could be two watches, or a watch and the phone. [DeviceManager.getDevice] answers
     * that; [DeviceManager.getLocalDevice] identifies which one is this phone.
     */
    suspend fun readDevices(deviceIds: Set<String>): Map<String, SamsungDeviceInfo> {
        val deviceManager = healthDataStore.getDeviceManager()
        val localDeviceId = runCatching { deviceManager.getLocalDevice()?.id }.getOrNull()

        return deviceIds.associateWith { deviceId ->
            runCatching {
                val device = deviceManager.getDevice(deviceId)
                SamsungDeviceInfo(
                    id = deviceId,
                    name = device?.name,
                    model = device?.model,
                    manufacturer = device?.manufacturer,
                    deviceType = device?.deviceType?.toString(),
                    isLocalDevice = deviceId == localDeviceId,
                    lookupError = if (device == null) "getDevice returned null" else null
                )
            }.getOrElse { throwable ->
                SamsungDeviceInfo(
                    id = deviceId,
                    name = null,
                    model = null,
                    manufacturer = null,
                    deviceType = null,
                    isLocalDevice = deviceId == localDeviceId,
                    lookupError = userReadableError(throwable)
                )
            }
        }
    }

    private fun PagedRead.toRawDataPoints(dataType: DataType): List<SamsungRawHealthDataPoint> =
        dataPoints.map { dataPoint ->
            dataPoint.toRawDataPoint(dataTypeName = dataType.name, fields = dataType.allFields)
        }

    private fun <T> List<T>.applyLimit(limit: Int?): List<T> =
        if (limit == null) this else take(limit)

    private fun SamsungReadStats.withLimit(limit: Int?): SamsungReadStats =
        copy(truncatedByLimit = limit != null && pointCount > limit)

    private companion object {
        /** Runaway guard: a page token that never clears would otherwise loop forever. */
        const val MAX_PAGES = 200
    }
}
