package com.candiy.candiyhc.network

import android.content.Context
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import com.candiy.candiyhc.network.GzipRequestInterceptor

object ApiClient {
    private var apiService: ApiService? = null
    private var baseUrl: String = "http://13.209.15.125/"  // 기본값 설정
    private lateinit var client: OkHttpClient

    fun init(context: Context, timeoutSeconds: Long = 90) {
        // OkHttpClient는 context를 필요로 하므로 여기서 초기화
        client = OkHttpClient.Builder()
            .connectTimeout(timeoutSeconds, TimeUnit.SECONDS)
            .readTimeout(timeoutSeconds, TimeUnit.SECONDS)
            .writeTimeout(timeoutSeconds, TimeUnit.SECONDS)
            .addInterceptor(GzipRequestInterceptor(context))
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .client(client)
            .build()

        apiService = retrofit.create(ApiService::class.java)
    }

    // Moshi 객체 생성
    val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory()) // Kotlin 데이터 클래스 지원
        .build()
//
//    // OkHttpClient 객체 생성 (타임아웃 설정)
//    val client = OkHttpClient.Builder()
//        .connectTimeout(30, TimeUnit.SECONDS)  // 연결 타임아웃 30초
//        .readTimeout(30, TimeUnit.SECONDS)     // 읽기 타임아웃 30초
//        .writeTimeout(30, TimeUnit.SECONDS)    // 쓰기 타임아웃 30초
//        .addInterceptor(GzipRequestInterceptor(this.context))  // Gzip 인터셉터 추가
//        .build()
//
//
//    // API 서비스 초기화 (선택적)
//    private fun init() {
//        if (apiService == null) {
//            val retrofit = Retrofit.Builder()
//                .baseUrl(baseUrl)
//                .addConverterFactory(MoshiConverterFactory.create(moshi))
//                .client(client)
//                .build()
//
//            apiService = retrofit.create(ApiService::class.java)
//        }
//    }

    // baseUrl을 설정하고, 초기화할 때 사용하는 함수
    fun setBaseUrl(newBaseUrl: String, context: Context) {
        baseUrl = newBaseUrl
        init(context)  // baseUrl이 변경될 때마다 초기화
    }

    // API 서비스 객체 반환
    fun getApiService(context: Context): ApiService {
        if (apiService == null) {
            init(context)  // 아직 초기화되지 않았다면 자동으로 초기화
        }
        return apiService!!
    }
}