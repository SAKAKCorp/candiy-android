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

    suspend fun <T> uploadData(
        auth: String,
        data: List<HealthDataUploadRequest>,
        strategy: HealthDataUploadStrategy<T>,
        onMarkUploaded: suspend (List<HealthDataUploadRequest>) -> Unit
    ) {
        val body = strategy.wrap(data)
        val response = strategy.upload(auth, body)
        if (response.isSuccessful) {
            Log.d("Uploader", "${strategy.dataType} uploaded successfully")
            onMarkUploaded(data)
        } else {
            Log.e("Uploader", "${strategy.dataType} upload failed: ${response.errorBody()?.string()}")
        }
    }

    suspend fun <T> uploadDataInChunks(
        auth: String,
        data: List<HealthDataUploadRequest>,
        strategy: HealthDataUploadStrategy<T>,
        onMarkUploaded: suspend (List<HealthDataUploadRequest>) -> Unit
    ) {
        data.chunked(50).forEach { chunk ->
            val body = strategy.wrap(chunk)
            val response = strategy.upload(auth, body)
            if (response.isSuccessful) {
                Log.d("Uploader", "${strategy.dataType} chunk uploaded successfully")
                onMarkUploaded(chunk)
            } else {
                Log.e("Uploader", "${strategy.dataType} chunk upload failed: ${response.errorBody()?.string()}")
            }
        }
    }

}
