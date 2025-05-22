package com.candiy.candiyhc.network.model.request
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class HealthDataUploadRequest(
    val app: String,
//    val data: String,
    val data: Map<String, Any>,
    val start: String,
    val end: String? = null,

    @Json(name = "last_modified_time")
    val lastModifiedTime: String,

    @Json(name = "data_id")val dataId: String
)

