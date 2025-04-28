package com.candiy.candiyhc.network

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
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

    // Retrofit 객체 생성
    val retrofit = Retrofit.Builder()
        .baseUrl("http://192.168.0.20:3000/")
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .client(client)  // OkHttpClient 적용
        .build()

    val apiService = retrofit.create(ApiService::class.java)
}