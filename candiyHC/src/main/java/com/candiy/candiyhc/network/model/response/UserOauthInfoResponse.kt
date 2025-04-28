package com.candiy.candiyhc.network.model.response

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class User(
    val _id: String,
    val created_at: String,
    val end_user_id: String,
    val updated_at: String
)

@JsonClass(generateAdapter = true)
data class UserOauthInfoResponse(
    val user: User
)

@JsonClass(generateAdapter = true)
data class UserInfoResponse(
    val user: User? = null,
    val uid: String? = null,
    val secret: String? = null
)