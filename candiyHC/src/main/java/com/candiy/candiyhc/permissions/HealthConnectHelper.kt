package com.candiy.candiyhc.permissions

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

object HealthConnectHelper {

    private val requiredPermissions = HealthConnectPermissions.requiredPermissions
    private lateinit var healthConnectClient: HealthConnectClient

    // 초기화
    fun init(context: Context) {
        healthConnectClient = HealthConnectClient.getOrCreate(context)
    }

    /**
     * 권한 요청 및 초기화
     * @param activity 권한 요청을 위한 Activity (ComponentActivity 권장)
     * @param onInitialized 권한 허용 후 초기화 완료 시 실행할 콜백
     * @param onPermissionDenied 권한 거부 시 실행할 콜백 (옵션)
     */
    @SuppressLint("RestrictedApi")
    fun requestPermissionsAndInitialize(
        activity: androidx.activity.ComponentActivity,
        onInitialized: () -> Unit,
        onPermissionDenied: (() -> Unit)? = null
    ) {
        init(activity)

        val permissionLauncher = activity.registerForActivityResult(
            PermissionController.createRequestPermissionResultContract()
        ) { grantedPermissions: Set<String> ->
            Log.d("HealthConnectHelper", "Granted permissions: $grantedPermissions")
            if (grantedPermissions.containsAll(requiredPermissions)) {
                initializeAndPrompt(activity, onInitialized)
            } else {
                onPermissionDenied?.invoke()
            }
        }

        activity.lifecycleScope.launch {
            val grantedPermissions = healthConnectClient.permissionController.getGrantedPermissions()
            Log.d("HealthConnectHelper", "Already granted permissions: $grantedPermissions")

            if (grantedPermissions.containsAll(requiredPermissions)) {
                initializeAndPrompt(activity, onInitialized)
            } else {
                permissionLauncher.launch(requiredPermissions)
            }
        }
    }

    private fun initializeAndPrompt(@SuppressLint("RestrictedApi") activity: androidx.core.app.ComponentActivity, onInitialized: () -> Unit) {
        onInitialized()

        // 배터리 최적화 및 앱 설정 안내
        SystemSettingsHelper.promptBatteryOptimizationAndAppSettings(activity)
    }

    fun getClient(): HealthConnectClient {
        if (!::healthConnectClient.isInitialized) {
            throw IllegalStateException("HealthConnectClient가 초기화되지 않았습니다. 먼저 init() 호출하세요.")
        }
        return healthConnectClient
    }
}