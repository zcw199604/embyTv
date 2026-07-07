package com.embytv.ui.home

import com.embytv.domain.model.ServerConfig
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class HomeConnectionErrorMessageTest {
    @Test
    fun loginErrorMessageIncludesHttpStatusEndpointAndUsername() {
        val message = loginErrorMessage(
            error = HttpException(Response.error<String>(401, "Unauthorized".toResponseBody())),
            config = ServerConfig(
                baseUrl = "http://10.10.10.100:60099/",
                username = "user",
                password = "secret",
                deviceId = "device-1",
            ),
        )

        assertTrue(message.contains("HTTP 401"))
        assertTrue(message.contains("用户名或密码错误"))
        assertTrue(message.contains("http://10.10.10.100:60099"))
        assertTrue(message.contains("用户名：user"))
        assertTrue(!message.contains("secret"))
    }
}
