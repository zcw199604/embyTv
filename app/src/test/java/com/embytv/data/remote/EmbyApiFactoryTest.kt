package com.embytv.data.remote

import okhttp3.OkHttpClient
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Test

class EmbyApiFactoryTest {
    @Test
    fun createReusesApiForSameBaseUrlAndToken() {
        val factory = EmbyApiFactory(OkHttpClient())

        val first = factory.create("http://emby.test", "token-1")
        val second = factory.create("http://emby.test/", "token-1")

        assertSame(first, second)
    }

    @Test
    fun createUsesDifferentApiForDifferentTokenOrBaseUrl() {
        val factory = EmbyApiFactory(OkHttpClient())

        val first = factory.create("http://emby.test", "token-1")
        val otherToken = factory.create("http://emby.test", "token-2")
        val otherServer = factory.create("http://other.test", "token-1")

        assertNotSame(first, otherToken)
        assertNotSame(first, otherServer)
    }
}
