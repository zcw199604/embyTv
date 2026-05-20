package com.embytv.core.network

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

object NetworkModule {
    fun createOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        return OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()
    }

    fun tokenQueryInterceptor(accessToken: String?): Interceptor = Interceptor { chain ->
        val original = chain.request()
        if (accessToken.isNullOrBlank()) {
            return@Interceptor chain.proceed(original)
        }
        val url = original.url.newBuilder()
            .addQueryParameter("api_key", accessToken)
            .build()
        chain.proceed(original.newBuilder().url(url).build())
    }
}
