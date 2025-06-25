package com.candiy.candiyhc.network.uploader

import android.content.Context
import android.util.Log
import com.candiy.candiyhc.network.ApiClient
import com.candiy.candiyhc.network.ApiService
import com.candiy.candiyhc.network.model.request.HealthDataUploadRequest

object HealthDataUploader {

    private lateinit var api: ApiService

    fun init(context: Context) {
        api = ApiClient.getApiService(context)
    }

    suspend fun <T> uploadDataInChunks(
        auth: String,
        data: List<HealthDataUploadRequest>,
        strategy: HealthDataUploadStrategy<T>,
        onMarkUploaded: suspend (List<HealthDataUploadRequest>) -> Unit,
        onTokenExpired: suspend () -> String?

    ) {

        var currentAuth = auth

        data.chunked(50).forEach { chunk ->
            val body = strategy.wrap(chunk)

            // 최대 2회 시도: 1차 시도 → 실패 시 토큰 재발급 → 재시도
            repeat(2) { attempt ->
                val response = strategy.upload(currentAuth, body)

                val errorMsg = response.errorBody()?.string()?.lowercase() ?: ""

                val tokenExpired = !response.isSuccessful &&
                        (errorMsg.contains("not authorized") || errorMsg.contains("unauthorized"))

                if (response.isSuccessful) {
                    Log.d("Uploader", "${strategy.dataType} chunk uploaded successfully")
                    onMarkUploaded(chunk)
                    return@repeat
                } else if (tokenExpired && attempt == 0) {
                    Log.w("Uploader", "Token expired during ${strategy.dataType} upload. Retrying with refreshed token.")

                    // 새 토큰 받아오기
                    val newToken = onTokenExpired()
                    if (newToken == null) {
                        Log.e("Uploader", "Token refresh failed. Upload aborted.")
                        return
                    }
                    currentAuth = "Bearer $newToken"
                } else {
                    Log.e("Uploader", "${strategy.dataType} chunk upload failed: $errorMsg")
                    return@repeat
                }
            }
        }
    }

}
