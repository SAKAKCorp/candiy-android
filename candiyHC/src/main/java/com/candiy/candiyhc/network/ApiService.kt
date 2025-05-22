package com.candiy.candiyhc.network

import com.candiy.candiyhc.network.model.request.CreateUserRequest
import com.candiy.candiyhc.network.model.request.HealthDataUploadRequest
import com.candiy.candiyhc.network.model.response.AccessTokenResponse
import com.candiy.candiyhc.network.model.response.JSendResponse
import com.candiy.candiyhc.network.model.response.UserInfoResponse
import com.squareup.moshi.Json
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.Path
import retrofit2.http.Query


data class StepListWrapper(
    @Json(name = "steps") val steps: List<HealthDataUploadRequest>
)

data class HeartRateListWrapper(
    @Json(name = "heart_rates") val heartRates: List<HealthDataUploadRequest>
)

data class OxygenSaturationListWrapper(
    @Json(name = "oxygen_saturations") val oxygenSaturations: List<HealthDataUploadRequest>
)

data class SleepListWrapper(
    @Json(name = "sleeps") val sleeps: List<HealthDataUploadRequest>
)
interface ApiService {
    @POST("api/steps")
    suspend fun uploadStepHealthData(
        @Header("Authorization") authorization: String,
        @Body body: StepListWrapper
    ): Response<ResponseBody>

    @POST("api/heart_rates")
    suspend fun uploadHeartRateData(
        @Header("Authorization") authorization: String,
        @Body body: HeartRateListWrapper
    ): Response<ResponseBody>

    @POST("api/oxygen_saturations")
    suspend fun uploadOxygenSaturationData(
        @Header("Authorization") authorization: String,
        @Body body: OxygenSaturationListWrapper
    ): Response<ResponseBody>

    @POST("api/sleeps")
    suspend fun uploadSleepData(
        @Header("Authorization") authorization: String,
        @Body body: SleepListWrapper
    ): Response<ResponseBody>

    @GET("api/users")
    suspend fun getUserOauthInfo(
        @Query("end_user_id") endUserId: String
    ): Response<JSendResponse<UserInfoResponse>>

    @POST("api/users")
    suspend fun createUser(
        @Body body: CreateUserRequest
    ): Response<JSendResponse<UserInfoResponse>>

    @FormUrlEncoded
    @POST("oauth/token")
    suspend fun getAccessToken(
        @Header("Authorization") authHeader: String,
        @Field("grant_type") grantType: String = "client_credentials"
    ): Response<AccessTokenResponse>
}

