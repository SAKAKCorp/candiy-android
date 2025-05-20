package com.candiy.candiyhc.network

import android.util.Log
import okhttp3.Interceptor
import okhttp3.MediaType
import okhttp3.RequestBody
import okhttp3.Response
import okio.Buffer
import okio.BufferedSink
import okio.GzipSink
import okio.buffer
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import android.content.Context

class GzipRequestInterceptor(private val context: Context) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        var originalRequest = chain.request()
        Log.d("Interceptor", "GzipRequestInterceptor invoked!")

        // /oauth/token 요청은 gzip 제외
        if (originalRequest.url().encodedPath().contains("/oauth/token")) {
            return chain.proceed(originalRequest)
        }

        // /api/users 요청은 gzip 제외
        if (originalRequest.url().encodedPath().contains("/api/users")) {
            return chain.proceed(originalRequest)
        }


        if (originalRequest.body() == null || originalRequest.header("Content-Encoding") != null) {
            return chain.proceed(originalRequest)
        }

        // 원본 요청 본문 크기 로그
        val originalBodySize = originalRequest.body()?.contentLength() ?: 0
        Log.i("Interceptor", "Original request body size: $originalBodySize bytes")


        val compressed = gzip(originalRequest.body()!!)
        val request = originalRequest.newBuilder()
            .header("Content-Encoding", "gzip")
            .method(originalRequest.method(), compressed)
            .build()

        Log.d("Interceptor", "Request URL: ${request.url()}")
        return chain.proceed(request)
    }

    fun gzip(originalBody: RequestBody): RequestBody {
        return object : RequestBody() {
            override fun contentType(): MediaType? = originalBody.contentType()

            override fun writeTo(sink: BufferedSink) {
                // 1. 바이트로 gzip 압축
                val buffer = Buffer()
                val gzipSink = GzipSink(buffer).buffer()
                originalBody.writeTo(gzipSink)
                gzipSink.close()

                // 2. 압축된 데이터를 바이트 배열로 추출
                val compressedBytes = buffer.readByteArray()

                // 3. 로그 및 저장
                Log.i("Interceptor!@", "Compressed request size: ${compressedBytes.size} bytes")
                saveCompressedToFile(compressedBytes)  // 👈 파일 저장 추가

                // 4. 실제 요청에 전달
                sink.write(compressedBytes)
            }

            override fun contentLength(): Long = -1 // 알 수 없으므로 -1로 설정
        }

    }

    private fun saveCompressedToFile(data: ByteArray) {
        try {
            val file = File(context.filesDir, "compressed_body_${System.currentTimeMillis()}.gz")
            val fos = FileOutputStream(file)
            fos.write(data)
            fos.close()
            Log.d("Interceptor", "Compressed file saved: ${file.absolutePath}")
        } catch (e: IOException) {
            Log.e("Interceptor", "Failed to save compressed file", e)
        }
    }

}