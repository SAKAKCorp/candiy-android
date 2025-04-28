package com.candiy.candiyhc.network

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    private var apiService: ApiService? = null

    // Moshi 객체 생성
    val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory()) // Kotlin 데이터 클래스 지원
        .build()

    // OkHttpClient 객체 생성 (타임아웃 설정)
    val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)  // 연결 타임아웃 30초
        .readTimeout(30, TimeUnit.SECONDS)     // 읽기 타임아웃 30초
        .writeTimeout(30, TimeUnit.SECONDS)    // 쓰기 타임아웃 30초
        .build()

    private var baseUrl: String = "http://13.209.15.125/"  // 기본값 설정

    // API 서비스 초기화 (선택적)
    private fun init() {
        if (apiService == null) {
            val retrofit = Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .client(client)
                .build()

            apiService = retrofit.create(ApiService::class.java)
        }
    }

    // baseUrl을 설정하고, 초기화할 때 사용하는 함수
    fun setBaseUrl(newBaseUrl: String) {
        baseUrl = newBaseUrl
        init()  // baseUrl이 변경될 때마다 초기화
    }

    // API 서비스 객체 반환
    fun getApiService(): ApiService {
        if (apiService == null) {
            init()  // 아직 초기화되지 않았다면 자동으로 초기화
        }
        return apiService!!
    }
}