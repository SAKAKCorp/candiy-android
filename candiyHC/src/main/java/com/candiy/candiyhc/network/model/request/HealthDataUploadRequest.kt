package com.candiy.candiyhc.network.model.request
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class HealthDataUploadRequest(
    val app: String,
    val data: String,
    val start: String,
    val end: String? = null,
    @Json(name = "data_id")val dataId: String
)