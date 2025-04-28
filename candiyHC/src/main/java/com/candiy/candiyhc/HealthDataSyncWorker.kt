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
import com.candiy.candiyhc.network.model.request.HealthDataUploadRequest
import com.candiy.candiyhc.network.ApiClient
import com.candiy.candiyhc.network.ApiClient.moshi
import com.candiy.candiyhc.network.HeartRateListWrapper
import com.candiy.candiyhc.network.OxygenSaturationListWrapper
import com.candiy.candiyhc.network.SleepListWrapper
import com.candiy.candiyhc.network.StepListWrapper
import java.time.Instant

class HealthDataSyncWorker(context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {

    val healthConnectManager = HealthConnectManager(applicationContext)
    val appContainer = AppContainer(applicationContext)
    val healthDataRepository = appContainer.itemsRepository
    val userRepository = appContainer.userRepository
    private val apiService = ApiClient.apiService

    override suspend fun doWork(): Result {
        // UserManager에서 endUserId 가져오기
        val userManager = UserManager(apiService, applicationContext)  // applicationContext 사용
        val endUserId = userManager.getEndUserId()

        // endUserId가 null인 경우 early return
        if (endUserId == null) {
            Log.e("HealthWorker", "endUserId is null, aborting work.")
            return Result.failure()
        }
        val user = userRepository.getUserByEndUserId(endUserId)
        val userId = user?.id
        if (userId == null) {
            Log.e("HealthWorker", "userId is null, aborting work.")
            return Result.failure()
        }
        Log.d("HealthWorker", "endUserId: ${endUserId}, userId: ${userId}")

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

            // 각 타입에 맞는 데이터를 처리
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

                dataTypes.forEach { type ->
                    val filteredData =
                        pendingData.filter { DataTypes.valueOf(it.type.uppercase()) == type }

                    val requests = filteredData.map {
                        HealthDataUploadRequest(
                            app = it.app,
                            data = it.data,
                            start = it.start,
                            end = null,
                            dataId = it.metadataId
                        )
                    }

                    val response = when (type) {
                        DataTypes.STEPS -> {
                            val body = StepListWrapper(steps = requests)
                            apiService.uploadStepHealthData(authorizationHeader, body)
                        }

                        DataTypes.HEART_RATE -> {
                            val body = HeartRateListWrapper(heartRates = requests)
                            apiService.uploadHeartRateData(authorizationHeader, body)
                        }

                        DataTypes.OXYGEN_SATURATION -> {
                            val body = OxygenSaturationListWrapper(oxygenSaturations = requests)
                            apiService.uploadOxygenSaturationData(authorizationHeader, body)
                        }

                        DataTypes.SLEEP -> {
                            val body = SleepListWrapper(sleeps = requests)
                            apiService.uploadSleepData(authorizationHeader, body)
                        }

                        else -> {
                            Log.w("Uploader", "Unsupported data type: $type")
                            return@forEach
                        }
                    }


                    if (response.isSuccessful) {
                        healthDataRepository.markAsUploaded(
                            filteredData.map { it.metadataId },
                            System.currentTimeMillis(),
                            userId
                        )
                        Log.d("Upload", "$type uploaded successfully")
                    } else {
                        Log.e("Upload", "$type upload failed: ${response.errorBody()?.string()}")
                    }
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
            val countData = mapOf("count" to record.count)
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
                healthDataRepository.insertIfNew(record.metadata.id, entity.updatedAt, entity)
                Log.d("candiyHC", "step entity insert success")
            }
//            else {
//                Log.d("candiyHC", "Duplicate step data skipped")
//            }
        }
        Log.d("candiyHC", "step entity insert success")
    }

    suspend fun handleHeartRateData(
        records: List<HeartRateRecord>,
        userId: Long
    ) {
        records.forEach { record ->
            val heartRateData = mapOf(
                "samples" to record.samples.map { sample ->
                    mapOf(
                        "time" to sample.time.toString(),
                        "beatsPerMinute" to sample.beatsPerMinute
                    )
                }
            )
            val jsonAdapter = moshi.adapter(Map::class.java)
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
                healthDataRepository.insertIfNew(record.metadata.id, entity.updatedAt, entity)
            }
//            else {
//                Log.d("candiyHC", "Duplicate step data skipped")
//            }
        }
        Log.d("candiyHC", "heartRate entity insert success")
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
                healthDataRepository.insertIfNew(record.metadata.id, entity.updatedAt, entity)
            }
//            else {
//                Log.d("candiyHC", "Duplicate step data skipped")
//            }
        }
        Log.d("candiyHC", "OxygenSaturation entity insert success")
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
            Log.d("totalSleep", "totalSleep: $totalSleep")

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
                healthDataRepository.insertIfNew(record.metadata.id, entity.updatedAt, entity)
            }
//            else {
//                Log.d("candiyHC", "Duplicate step data skipped")
//            }
        }
        Log.d("candiyHC", "OxygenSaturation entity insert success")
    }
}
