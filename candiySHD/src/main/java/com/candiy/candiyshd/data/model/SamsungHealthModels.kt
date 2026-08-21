package com.candiy.candiyshd.data.model

import java.time.LocalDateTime

/**
 * The time range a read covers.
 *
 * [sleepStart] is tracked separately because the SDK matches a sleep session on its *start*
 * time. A night that began at 23:40 the evening before [start] is dropped whole rather than
 * clipped, so sleep has to be read from an earlier point than the other types.
 */
data class SamsungReadWindow(
    val start: LocalDateTime,
    val end: LocalDateTime,
    val sleepStart: LocalDateTime
)

data class SamsungHealthSnapshot(
    val steps: SamsungStepSummary?,
    val heartRates: List<SamsungHeartRateSample>,
    val oxygenSaturations: List<SamsungOxygenSaturationSample>,
    val sleepSessions: List<SamsungSleepSummary>
)

data class SamsungStepSummary(
    val count: Long,
    val startTime: String,
    val endTime: String
)

data class SamsungHeartRateSample(
    val beatsPerMinute: Float,
    val minBeatsPerMinute: Float?,
    val maxBeatsPerMinute: Float?,
    val startTime: String,
    val endTime: String,
    val sourceAppId: String?,
    val sourceDeviceId: String?,
    val uid: String
)

data class SamsungOxygenSaturationSample(
    val percentage: Float,
    val minPercentage: Float?,
    val maxPercentage: Float?,
    val startTime: String,
    val endTime: String,
    val sourceAppId: String?,
    val sourceDeviceId: String?,
    val uid: String
)

data class SamsungSleepSummary(
    val startTime: String,
    val endTime: String,
    val duration: String?,
    val sleepScore: Int?,
    val stages: List<SamsungSleepStage>,
    val sourceAppId: String?,
    val sourceDeviceId: String?,
    val uid: String
)

data class SamsungSleepStage(
    val startTime: String,
    val endTime: String,
    val stage: String
)

data class SamsungRawHealthDataPoint(
    val dataType: String,
    val uid: String,
    val clientDataId: String?,
    val clientVersion: Int?,
    val sourceAppId: String?,
    val sourceDeviceId: String?,
    val updateTime: String?,
    val startTime: String?,
    val endTime: String?,
    val zoneOffset: String?,
    val startLocalDateTime: String?,
    val endLocalDateTime: String?,
    val fields: Map<String, Any?>
)

data class SamsungRawStepAggregate(
    val operation: String,
    val value: Long?,
    val startTime: String?,
    val endTime: String?,
    val startLocalDateTime: String?,
    val endLocalDateTime: String?
)

/**
 * Per-type read diagnostics. The SDK returns partial results when a page token is left
 * unfollowed, so a dump that does not record how many pages were walked cannot be told
 * apart from a dump that is genuinely short.
 */
data class SamsungReadStats(
    val pages: Int,
    val pointCount: Int,
    val hitPageCap: Boolean,
    val truncatedByLimit: Boolean
)

/**
 * Resolved identity of a `sourceDeviceId`. Data points carry only an opaque id, which says
 * nothing about whether a reading came from the phone, a watch, or a third-party accessory.
 */
data class SamsungDeviceInfo(
    val id: String,
    val name: String?,
    val model: String?,
    val manufacturer: String?,
    val deviceType: String?,
    val isLocalDevice: Boolean,
    val lookupError: String?
)

data class SamsungRawHealthDataDump(
    val startTime: String,
    val endTime: String,
    val watchOnly: Boolean,
    val note: String,
    val stats: Map<String, SamsungReadStats>,
    val devices: Map<String, SamsungDeviceInfo>,
    val steps: List<SamsungRawStepAggregate>,
    val heartRates: List<SamsungRawHealthDataPoint>,
    val oxygenSaturations: List<SamsungRawHealthDataPoint>,
    val sleepSessions: List<SamsungRawHealthDataPoint>
)

data class SamsungRawHealthChange(
    val dataType: String,
    val changeType: String,
    val changeTime: String?,
    val deleteDataUid: String?,
    val upsertDataPoint: SamsungRawHealthDataPoint?
)

data class SamsungRawHealthChangeDump(
    val startTime: String,
    val endTime: String,
    val note: String,
    val heartRates: List<SamsungRawHealthChange>,
    val oxygenSaturations: List<SamsungRawHealthChange>,
    val sleepSessions: List<SamsungRawHealthChange>
)
