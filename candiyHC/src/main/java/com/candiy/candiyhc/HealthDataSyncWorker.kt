package com.candiy.candiyhc

import android.content.Context
import android.util.Log
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.candiy.candiyhc.data.di.AppContainer
import com.candiy.candiyhc.data.enums.DataTypes
import com.candiy.candiyhc.data.local.entity.HealthDataEntity
import com.candiy.candiyhc.data.local.model.HeartRateData
import com.candiy.candiyhc.data.local.model.HeartRateSample
import com.candiy.candiyhc.data.local.model.StepData
import com.candiy.candiyhc.network.model.request.HealthDataUploadRequest
import com.candiy.candiyhc.network.ApiClient
import com.candiy.candiyhc.network.ApiClient.moshi
import com.candiy.candiyhc.network.uploader.HealthDataUploader
import com.candiy.candiyhc.network.uploader.strategy.HeartRateUploadStrategy
import com.candiy.candiyhc.network.uploader.strategy.OxygenSaturationUploadStrategy
import com.candiy.candiyhc.network.uploader.strategy.SleepUploadStrategy
import com.candiy.candiyhc.network.uploader.strategy.StepUploadStrategy
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Types
import java.time.Instant

class HealthDataSyncWorker(context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {

    val healthConnectManager = HealthConnectManager(applicationContext)
    val appContainer = AppContainer(applicationContext)
    val healthDataRepository = appContainer.itemsRepository
    val userRepository = appContainer.userRepository
    private val apiService = ApiClient.getApiService(context)

    override suspend fun doWork(): Result {
        // UserManager에서 endUserId 가져오기
        val userManager = UserManager(apiService, applicationContext)  // applicationContext 사용
        val endUserId = userManager.getEndUserId()
        val deviceModel = userManager.getDeviceModel()

        // endUserId가 null인 경우 early return
        if (endUserId == null) {
            Log.e("HealthWorker", "endUserId is null, aborting work.")
            return Result.failure()
        }
        val user = userRepository.getUserByEndUserIdAndDeviceModel(endUserId, deviceModel)
        val userId = user?.id
        Log.d("user id??", "${userId}")
        if (userId == null) {
            Log.e("HealthWorker", "userId is null, aborting work.")
            return Result.failure()
        }
        Log.d("HealthWorker", "endUserId: ${endUserId}, userId: ${userId}, device: ${deviceModel}")

        val dataTypes = inputData.getStringArray("data_types")?.mapNotNull {
            try {
                DataTypes.valueOf(it)
            } catch (e: Exception) {
                null
            }
        }?.toSet() ?: emptySet()

        val token = inputData.getString("token")
        Log.d("HealthWorker", "token: ${token}")

        if (token == null) {
            Log.e("HealthWorker", "token is null, aborting work.")
            return Result.failure()
        }

        return try {
            Log.d("HealthWorker", "doWork() called at ${System.currentTimeMillis()}")

            // 각 타입에 맞는 데이터를 처리(Room DB에 저장)
            val actions = mapOf(
                DataTypes.STEPS to suspend {
                    val steps = healthConnectManager.readStepCounts()
                    handleStepData(steps, userId)
                },
                DataTypes.HEART_RATE to suspend {
                    val heartRates = healthConnectManager.readHeartRates()
                    handleHeartRateData(heartRates, userId)
                },
                DataTypes.OXYGEN_SATURATION to suspend {
                    val oxygenSaturations = healthConnectManager.readOxygenSaturations()
                    handleOxygenSaturationData(
                        oxygenSaturations,
                        userId
                    )
                },
                DataTypes.SLEEP to suspend {
                    val sleepSessions = healthConnectManager.readSleepSessions()
                    handleSleepSessionData(sleepSessions, userId)
                }
            )

            for (dataType in dataTypes) {
                actions[dataType]?.invoke()
            }
            // Delta Sync: `lastSyncedAt` 이후 데이터만 업로드
            val lastSyncedAt = user.lastSyncedAt
            val pendingData = healthDataRepository.getPendingUploadData(lastSyncedAt, userId)

            Log.d("HealthWorker", "lastSyncedAt: ${lastSyncedAt}, pendingData: ${pendingData}")

            if (pendingData.isNotEmpty()) {
                val authorizationHeader = "Bearer $token"

                for (type in dataTypes) {
                    val filteredData =
                        pendingData.filter { DataTypes.valueOf(it.type.uppercase()) == type }
                    val type2 = Types.newParameterizedType(
                        Map::class.java,
                        String::class.java,
                        Any::class.java
                    )
                    val jsonAdapter: JsonAdapter<Map<String, Any>> = moshi.adapter(type2)
                    val requests = filteredData.map {
                        val parsedData: Map<String, Any> = jsonAdapter.fromJson(it.data) ?: emptyMap()

                        HealthDataUploadRequest(
                            app = it.app,
                            data = parsedData,
                            start = it.start,
                            end = it.end,
                            dataId = it.metadataId,
                            lastModifiedTime = it.lastModifiedTime.toString(),
                        )

                    }
                    Log.d("requests", "${requests}")

                    // TODO: deviceModel 및 manufacturer 정보 포함하여 서버에 업로드 요청에 추가하기
                    // Strategy 선택
                    val strategy = when (type) {
                        DataTypes.STEPS -> StepUploadStrategy(apiService)
                        DataTypes.HEART_RATE -> HeartRateUploadStrategy(apiService)
                        DataTypes.SLEEP -> SleepUploadStrategy(apiService)
                        DataTypes.OXYGEN_SATURATION -> OxygenSaturationUploadStrategy(apiService)
                        else -> {
                            Log.w("Uploader", "Unsupported data type: $type")
                            continue
                        }
                    }

                    // 공통 업로더 호출
                    HealthDataUploader.uploadData(
                        auth = authorizationHeader,
                        data = requests,
                        strategy = strategy,
                        onMarkUploaded = { uploaded ->
                            healthDataRepository.markAsUploaded(
                                uploaded.map { it.dataId },
                                System.currentTimeMillis(),
                                userId
                            )
                            userRepository.updateLastSyncedAt(endUserId, Instant.now().toString())
                        }
                    )
                    Log.d("uploadData", "uploadData end")
                }

            }
            Result.success()

        } catch (e: Exception) {
            e.printStackTrace()
            // 오류 발생 시
            Result.failure()
        }

    }

    // TODO: 리팩토링 필요(공통 코드 많음)
    // 데이터 타입에 맞게 처리하는 함수
    suspend fun handleStepData(records: List<StepsRecord>, userId: Long) {

        records.forEach { record ->
            val jsonAdapter = moshi.adapter(StepData::class.java)
            val stepData = StepData(count = record.count.toInt())
            val jsonData = jsonAdapter.toJson(stepData)

            // 중복 체크
            val exists = healthDataRepository.checkIfExists(
                userId,
                record.metadata.dataOrigin.packageName,
                record.metadata.id,
                record.metadata.lastModifiedTime.toString()
            )

            if (!exists) {
                val entity = HealthDataEntity(
                    userId = userId,
                    type = DataTypes.STEPS.toString(),
                    app = record.metadata.dataOrigin.packageName,
                    data = jsonData,
                    metadataId = record.metadata.id,
                    start = record.startTime.toString(),
                    end = record.endTime.toString(),
                    lastModifiedTime = record.metadata.lastModifiedTime.toString(),
                    updatedAt = Instant.now().toEpochMilli(),
                    isUploaded = false
                )
                try{
                    healthDataRepository.insertIfNew(record.metadata.id, entity.updatedAt, entity)
                }catch (e: Exception) {
                    Log.e("candiyHC", "Failed to insert STEP data: ${e.message}")
                }
            }
        }
        Log.d("candiyHC", "STEP entity inserted successfully into RoomDB")
    }

    suspend fun handleHeartRateData(
        records: List<HeartRateRecord>,
        userId: Long
    ) {
        records.forEach { record ->
            val samples = record.samples.map { sample ->
                HeartRateSample(
                    time = sample.time.toString(),
                    beatsPerMinute = sample.beatsPerMinute
                )
            }
            val heartRateData = HeartRateData(samples = samples)

            val jsonAdapter = moshi.adapter(HeartRateData::class.java)
            val jsonData = jsonAdapter.toJson(heartRateData)

            // 중복 체크
            val exists = healthDataRepository.checkIfExists(
                userId,
                record.metadata.dataOrigin.packageName,
                record.metadata.id,
                record.metadata.lastModifiedTime.toString()
            )

            if (!exists) {
                val entity = HealthDataEntity(
                    userId = userId,
                    type = DataTypes.HEART_RATE.toString(),
                    app = record.metadata.dataOrigin.packageName,
                    data = jsonData,
                    metadataId = record.metadata.id,
                    start = record.startTime.toString(),
                    end = record.endTime.toString(),
                    lastModifiedTime = record.metadata.lastModifiedTime.toString(),
                    updatedAt = Instant.now().toEpochMilli(),
                    isUploaded = false
                )
                try{
                    healthDataRepository.insertIfNew(record.metadata.id, entity.updatedAt, entity)
                }catch (e: Exception) {
                    Log.e("candiyHC", "Failed to insert HeartRate data: ${e.message}")
                }
            }
        }
        Log.d("candiyHC", "HeartRate entity inserted successfully into RoomDB")
    }


    suspend fun handleOxygenSaturationData(
        records: List<OxygenSaturationRecord>,
        userId: Long
    ) {
        records.forEach { record ->
            val countData = mapOf("percentage" to record.percentage.value)
            val jsonAdapter = moshi.adapter(Map::class.java)
            val jsonData = jsonAdapter.toJson(countData)

            // 중복 체크
            val exists = healthDataRepository.checkIfExists(
                userId,
                record.metadata.dataOrigin.packageName,
                record.metadata.id,
                record.metadata.lastModifiedTime.toString()
            )
            if (!exists) {
                val entity = HealthDataEntity(
                    userId = userId,
                    type = DataTypes.OXYGEN_SATURATION.toString(),
                    app = record.metadata.dataOrigin.packageName,
                    data = jsonData,
                    metadataId = record.metadata.id,
                    start = record.time.toString(),
                    lastModifiedTime = record.metadata.lastModifiedTime.toString(),
                    updatedAt = Instant.now().toEpochMilli(),
                    isUploaded = false
                )
                try{
                    healthDataRepository.insertIfNew(record.metadata.id, entity.updatedAt, entity)
                }catch (e: Exception) {
                    Log.e("candiyHC", "Failed to insert OxygenSaturation data: ${e.message}")
                }
            }
        }
        Log.d("candiyHC", "OxygenSaturation entity inserted successfully into RoomDB")
    }

    suspend fun handleSleepSessionData(
        records: List<SleepSessionRecord>,
        userId: Long
    ) {
        records.forEach { record ->
            val stagesList = record.stages.map { stage ->
                mapOf(
                    "startTime" to stage.startTime.toString(),
                    "endTime" to stage.endTime.toString(),
                    "stage" to stage.stage
                )
            }
            val totalSleep =
                healthConnectManager.readResultSleepSession(record.startTime, record.endTime)
                    .toString()

            val countData = mapOf(
                "sleep_duration_total" to totalSleep, // actual sleep time
                "stages" to stagesList,
                "notes" to (record.notes ?: ""),
                "title" to (record.title ?: ""),
            )

            val jsonAdapter = moshi.adapter(Map::class.java)
            val jsonData = jsonAdapter.toJson(countData)


            // 중복 체크
            val exists = healthDataRepository.checkIfExists(
                userId,
                record.metadata.dataOrigin.packageName,
                record.metadata.id,
                record.metadata.lastModifiedTime.toString()
            )
            if (!exists) {
                val entity = HealthDataEntity(
                    userId = userId,
                    type = DataTypes.SLEEP.toString(),
                    app = record.metadata.dataOrigin.packageName,
                    data = jsonData,
                    metadataId = record.metadata.id,
                    start = record.startTime.toString(),
                    end = record.endTime.toString(),
                    lastModifiedTime = record.metadata.lastModifiedTime.toString(),
                    updatedAt = Instant.now().toEpochMilli(),
                    isUploaded = false
                )
                try{
                    healthDataRepository.insertIfNew(record.metadata.id, entity.updatedAt, entity)
                }catch (e: Exception) {
                    Log.e("candiyHC", "Failed to insert Sleep data: ${e.message}")
                }              }
        }
        Log.d("candiyHC", "Sleep entity inserted successfully into RoomDB")
    }
}
