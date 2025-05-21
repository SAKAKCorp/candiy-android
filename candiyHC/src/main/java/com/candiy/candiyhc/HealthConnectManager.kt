package com.candiy.candiyhc

import android.content.Context
import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.lifecycle.LifecycleOwner
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.candiy.candiyhc.data.di.AppContainer
import com.candiy.candiyhc.data.enums.Connections
import com.candiy.candiyhc.data.enums.DataTypes
import com.candiy.candiyhc.network.ApiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.Duration
import java.util.concurrent.TimeUnit


class HealthConnectManager(val context: Context) {
    val healthConnectClient = HealthConnectClient.getOrCreate(context)
    private val REQUEST_BLUETOOTH_PERMISSION = 1001  // 권한 요청 코드
    val appContainer = AppContainer(context.applicationContext)
    val userRepository = appContainer.userRepository

    // candiyHC 초기화 메소드
    fun initConnection(
        onResult: (Boolean) -> Unit
    ) {
        // TODO: 초기화 로직 추가
        Log.d("candiyHC", "Initializing candiyHC SDK")
        onResult(true)
    }

//    fun checkBluetoothPermission(context: Context, onPermissionGranted: () -> Unit) {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
//            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
//        ) {
//            // 권한이 없는 경우 권한 요청
//            ActivityCompat.requestPermissions(
//                context as Activity,
//                arrayOf(Manifest.permission.BLUETOOTH_CONNECT),
//                REQUEST_BLUETOOTH_PERMISSION
//            )
//        } else {
//            // 권한이 이미 있는 경우 바로 콜백 실행
//            onPermissionGranted()
//        }
//    }


    // BLE 장치 스캔 시작
    fun startDeviceScan(type: Connections, onResult: (Boolean) -> Unit) {
        // TODO: 실제 장치 검색 로직
        Log.d("candiyHC", "Starting device scan for type: $type")
        onResult(true)
    }

    // 심박수 데이터 읽는 함수
    suspend fun readHeartRates(): List<HeartRateRecord> {
        val endTime = Instant.now()
        val startTime = endTime.minus(Duration.ofDays(30))

        val request = ReadRecordsRequest(
            recordType = HeartRateRecord::class,
            timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
        )
        return healthConnectClient.readRecords(request).records
    }

    // 걸음수 데이터 읽는 함수
    suspend fun readStepCounts(): List<StepsRecord> {
        val endTime = Instant.now()
        val startTime = endTime.minus(Duration.ofDays(30))

        val request = ReadRecordsRequest(
            recordType = StepsRecord::class,
            timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
        )
        return healthConnectClient.readRecords(request).records
    }

    // 혈액 속 산소 포화도 데이터 읽는 함수
    suspend fun readOxygenSaturations(): List<OxygenSaturationRecord> {
        val endTime = Instant.now()
        val startTime = endTime.minus(Duration.ofDays(30))

        val request = ReadRecordsRequest(
            recordType = OxygenSaturationRecord::class,
            timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
        )
        return healthConnectClient.readRecords(request).records
    }

    // 수면 데이터 읽는 함수
    suspend fun readSleepSessions(): List<SleepSessionRecord> {
        val endTime = Instant.now()
        val startTime = endTime.minus(Duration.ofDays(30))

        val request = ReadRecordsRequest(
            recordType = SleepSessionRecord::class,
            timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
        )
        return healthConnectClient.readRecords(request).records
    }

    suspend fun readResultSleepSession(startTime: Instant, endTime: Instant): Duration? {
        val response = healthConnectClient.aggregate(
            AggregateRequest(
                metrics = setOf(SleepSessionRecord.SLEEP_DURATION_TOTAL),
                timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
            )
        )
        // 전체 수면 시간 집계 값 가져오기
        return response[SleepSessionRecord.SLEEP_DURATION_TOTAL]
    }
    fun observeWork(lifecycleOwner: LifecycleOwner, onStateChanged: (WorkInfo.State) -> Unit, onComplete: (Boolean) -> Unit) {
        val workManager = WorkManager.getInstance(context)
        workManager.getWorkInfosForUniqueWorkLiveData("HealthDataSyncWork")
            .observe(lifecycleOwner) { workInfos ->
                val workInfo = workInfos?.firstOrNull()
                workInfo?.state?.let { state ->
                    when (state) {
                        WorkInfo.State.ENQUEUED, WorkInfo.State.RUNNING -> onStateChanged(WorkInfo.State.RUNNING)
                        WorkInfo.State.SUCCEEDED -> {
                            onStateChanged(WorkInfo.State.SUCCEEDED)
                            onComplete(true)
                        }
                        WorkInfo.State.FAILED, WorkInfo.State.CANCELLED -> {
                            onStateChanged(WorkInfo.State.FAILED)
                            onComplete(false)
                        }
                        else -> {}
                    }
                }
            }
    }
    fun startWork(
        dataTypes: Set<DataTypes>,
        token: String,
        lifecycleOwner: LifecycleOwner,
        onStateChanged: (WorkInfo.State) -> Unit,
        onComplete: (Boolean) -> Unit
    ) {
        val workManager = WorkManager.getInstance(context)
        val inputData = Data.Builder()
            .putStringArray("data_types", dataTypes.map { it.name }.toTypedArray())
            .putString("token", token)
            .build()

        val oneTimeWorkRequest = OneTimeWorkRequestBuilder<HealthDataSyncWorker>()
            .setInputData(inputData)
            .build()

        Log.d("startWork", "start")


        workManager.enqueueUniqueWork(
            "HealthDataSyncWork_OneTime",
            ExistingWorkPolicy.KEEP,
            oneTimeWorkRequest
        )

        // 작업 상태 관찰 (로딩 UI 업데이트용)
        workManager.getWorkInfoByIdLiveData(oneTimeWorkRequest.id)
            .observe(lifecycleOwner) { workInfo ->
                workInfo?.let {
                    onStateChanged(it.state)

                    if (it.state == WorkInfo.State.SUCCEEDED) {
                        Log.d("startWork", "WorkInfo SUCCEEDED")

                        onComplete(true)
                    } else if (it.state == WorkInfo.State.FAILED || it.state == WorkInfo.State.CANCELLED) {
                        Log.d("startWork", "WorkInfo FAILED")

                        onComplete(false)
                    }
                }
            }

        // 주기 작업 (백그라운드 15분마다)
        val periodicWorkRequest = PeriodicWorkRequestBuilder<HealthDataSyncWorker>(15, TimeUnit.MINUTES)
            .setInputData(inputData)
            .build()

        workManager.enqueueUniquePeriodicWork(
            "HealthDataSyncWork_Periodic",
            ExistingPeriodicWorkPolicy.KEEP,
            periodicWorkRequest
        )
    }


    fun syncHealthData(
        type: Connections,
        dataTypes: Set<DataTypes>,
        apiKey: String,
        endUserId: String,
        deviceManufacturer: String,
        deviceModel: String,
        lifecycleOwner: LifecycleOwner,
        onStateChanged: (WorkInfo.State) -> Unit,
        onComplete: (Boolean) -> Unit
    ) {
        Log.d(
            "candiyHC",
            "Starting real-time data streaming for type: $type with data types: $dataTypes"
        )
        val userManager = UserManager(ApiClient.getApiService(context), context)

        // apiKey가 유효한지 먼저 확인
        if (!apiKey.isNullOrEmpty()) {
            CoroutineScope(Dispatchers.IO).launch {
                // apiKey가 유효함
                Log.d("API Key", "API Key is valid")

                try {
                    // endUserId를 UserManager에 저장
                    userManager.setEndUserId(endUserId)
                    userManager.setDeviceModel(deviceModel)

                    // token을 가져와서 작업 시작
                    val token = userManager.getOrCreateUserToken(deviceManufacturer).toString()
                    Log.d("Token", "Fetched token: $token")

                    if (type == Connections.BLE) {
                        withContext(Dispatchers.Main) {
                            startWork(dataTypes, token, lifecycleOwner, onStateChanged, onComplete)
                        }
                    } else {
                        TODO("ANT 연결 방식 아직 미구현")
                    }

                } catch (e: Exception) {
                    Log.e("Error", "Error occurred: ${e.message}")
                }
            }
        } else {
            // apiKey가 없거나 유효하지 않음
            Log.e("API Key", "Invalid API Key")
        }

    }

    // 실시간 데이터 스트리밍 중지
    fun stopRealtime(type: Connections) {
        Log.d("candiyHC", "Stopping real-time data streaming for type: $type")
        TODO()
    }

    // 디바이스 연결 해제
    fun disconnect(type: Connections) {
        Log.d("candiyHC", "Disconnecting device for type: $type")
        TODO()
    }


}