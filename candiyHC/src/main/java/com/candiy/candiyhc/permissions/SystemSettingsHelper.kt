package com.candiy.candiyhc.permissions
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.widget.Toast

object SystemSettingsHelper {

    /**
     * 시스템에 배터리 최적화 예외 요청하고,
     * 이후 앱 설정 화면으로 이동할 수 있게 안내하는 다이얼로그를 띄웁니다.
     *
     * @param activity 현재 액티비티 컨텍스트
     */
    fun promptBatteryOptimizationAndAppSettings(activity: Activity) {
        val pm = activity.getSystemService(PowerManager::class.java)
        val packageName = activity.packageName

        Log.d("SystemSettingsHelper", "promptBatteryOptimizationAndAppSettings: $packageName")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                try {
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:$packageName")
                    }
                    activity.startActivity(intent)
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(
                        activity,
                        "배터리 최적화 예외 설정 화면을 열 수 없습니다.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        // 앱 설정 안내 다이얼로그 띄우기
        androidx.appcompat.app.AlertDialog.Builder(activity)
            .setTitle("앱 설정 안내")
            .setMessage(
                "정상적인 알림 수신과 권한 유지(예: '사용하지 않는 앱 권한 제거' 비활성화, 알림 권한 허용)를 위해\n" +
                        "앱 설정 화면에서 관련 설정을 확인해주세요."
            )
            .setPositiveButton("앱 설정으로 이동") { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:$packageName")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                activity.startActivity(intent)
            }
            .setNegativeButton("나중에", null)
            .show()
    }
}
