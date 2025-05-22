package com.candiy.candiyhc.network.uploader

import com.candiy.candiyhc.network.model.request.HealthDataUploadRequest
import okhttp3.ResponseBody
import retrofit2.Response

interface HealthDataUploadStrategy<T> {
    val dataType: String
    fun wrap(data: List<HealthDataUploadRequest>): T
    suspend fun upload(authHeader: String, body: T): Response<ResponseBody>
}