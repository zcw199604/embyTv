package com.embytv.data.remote

import com.embytv.core.network.NetworkModule
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

interface EmbyApiProvider {
    fun create(baseUrl: String, accessToken: String? = null): EmbyApi
}

class EmbyApiFactory(
    private val okHttpClient: OkHttpClient,
) : EmbyApiProvider {
    override fun create(baseUrl: String, accessToken: String?): EmbyApi {
        val client = okHttpClient.newBuilder()
            .addInterceptor(NetworkModule.tokenQueryInterceptor(accessToken))
            .build()

        return Retrofit.Builder()
            .baseUrl(baseUrl.normalizedBaseUrl())
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(EmbyApi::class.java)
    }
}

internal fun String.normalizedBaseUrl(): String {
    val value = trim()
    require(value.startsWith("http://") || value.startsWith("https://")) {
        "Emby 地址必须以 http:// 或 https:// 开头"
    }
    return if (value.endsWith("/")) value else "$value/"
}
