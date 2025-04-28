package com.candiy.candiyhc.network.model.response

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class JSendResponse<T>(
    val status: String,
    val data: T?,
    val message: String? = null
)

@JsonClass(generateAdapter = true)
data class ErrorResponse(
    val error: String // The error message you get, like "Not authorized".
)
