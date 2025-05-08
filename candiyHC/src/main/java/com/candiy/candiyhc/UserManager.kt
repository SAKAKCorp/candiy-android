package com.candiy.candiyhc

import android.content.Context
import android.util.Base64
import android.util.Log
import com.candiy.candiyhc.network.ApiService
import com.candiy.candiyhc.data.local.HealthDataDatabase
import com.candiy.candiyhc.data.local.entity.UserEntity
import com.candiy.candiyhc.network.model.request.CreateUserRequest

class UserManager(
    private val apiService: ApiService,
    private val context: Context
) {

    private val userDao = HealthDataDatabase.getDatabase(context).userDao()

    private val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

    fun setEndUserId(id: String) {
        prefs.edit().putString("end_user_id", id).apply()
    }

    fun getEndUserId(): String? {
        return prefs.getString("end_user_id", null)
    }

    // deviceModel 저장 및 불러오기 추가
    fun setDeviceModel(deviceModel: String) {
        prefs.edit().putString("device_model", deviceModel).apply()
    }

    fun getDeviceModel(): String? {
        return prefs.getString("device_model", null)
    }

    suspend fun getOrCreateUserToken(deviceManufacturer: String): String {
        var uid: String? = null
        var secret: String? = null
        val endUserId = getEndUserId() ?: throw IllegalArgumentException("end_user_id is not set")
        val deviceModel = getDeviceModel() ?: throw IllegalArgumentException("device_model is not set")


        // TODO: 서버 조회 시 endUserId & deviceAddress
        val response = apiService.getUserOauthInfo(endUserId) // 서버에 유저 있는 지 확인
        Log.d("candiyHC", "getUserOauthInfo response: ${response}")

        if (response.isSuccessful){
            val body = response.body()

            if (body?.status == "success") {
                val userInfo = body.data
                uid = userInfo?.uid
                secret = userInfo?.secret

                saveUserToLocalDb(deviceManufacturer, deviceModel) // room db에 데이터 저장
            } else {
                val errorMessage = body?.message ?: "Unknown error"
                Log.e("UserError", "Error: $errorMessage")
            }
        }
        else {
            Log.d("endUserId", "${endUserId}")
            val createUserRequest = CreateUserRequest(end_user_id = endUserId)
            val createUserResponse = apiService.createUser(createUserRequest)
            if(createUserResponse.isSuccessful){
                val body = createUserResponse.body()
                if (body?.status == "success") {
                    val userInfo = body.data
                    uid = userInfo?.uid
                    secret = userInfo?.secret

                    saveUserToLocalDb(deviceManufacturer, deviceModel)
                    Log.d("teset", "saveUserToLocalDb")
                } else {
                    val errorMessage = body?.message ?: "Unknown error"
                    Log.e("UserError2", "Error: $errorMessage")
                }
            }
        }

        Log.d("candiyHC", "uid: ${uid}, secret: ${secret}")

        var token = ""
        if(uid != null &&  secret != null) {
            token = fetchToken(uid, secret)
        }

        return token
    }

    private suspend fun saveUserToLocalDb(deviceManufacturer: String, deviceModel: String) {
        val endUserId = getEndUserId() ?: throw IllegalArgumentException("end_user_id is not set")

        // 먼저 endUserId로 이미 존재하는 유저가 있는지 확인
        val existingUser = userDao.getUserByEndUserIdAndDeviceModel(endUserId, deviceModel)

        if (existingUser != null) {
            // 이미 존재하는 경우
            Log.d("UserRegistrar", "User already exists in DB with userId: $endUserId")
        } else {
            // 존재하지 않으면 새로 저장
            val entity = UserEntity(
                endUserId = endUserId,
                deviceManufacturer = deviceManufacturer,
                deviceModel = deviceModel,
            )
            userDao.insert(entity)
            Log.d("UserRegistrar", "User saved to local DB with userId: $endUserId")
        }
    }



    suspend fun fetchToken(uid: String, secrete: String): String {
        val credentials = "$uid:$secrete"
        val encoded = Base64.encodeToString(credentials.toByteArray(), Base64.NO_WRAP)
        val authHeader = "Basic $encoded"

        val response = apiService.getAccessToken(authHeader)
        Log.d("token response", "${response.body()?.access_token}")

        return if (response.isSuccessful) {
            response.body()?.access_token ?: throw Exception("Token not found in response")
        } else {
            val errorBody = response.errorBody()?.string()
            throw Exception("Failed to fetch token: ${response.code()} $errorBody")
        }
    }
}
