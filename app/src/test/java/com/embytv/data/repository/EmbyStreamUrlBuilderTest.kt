package com.embytv.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EmbyStreamUrlBuilderTest {
    private val builder = EmbyStreamUrlBuilder()

    @Test
    fun buildVideoStreamUrl_trimsTrailingSlashAndAddsToken() {
        val url = builder.buildVideoStreamUrl(
            serverUrl = "http://127.0.0.1:8096/",
            itemId = "abc 123",
            accessToken = "token-value",
        )

        assertEquals(
            "http://127.0.0.1:8096/Videos/abc+123/stream?Static=true&api_key=token-value",
            url,
        )
    }

    @Test
    fun buildPrimaryImageUrl_returnsNullWithoutTag() {
        assertTrue(
            builder.buildPrimaryImageUrl(
                serverUrl = "http://127.0.0.1:8096",
                itemId = "item",
                tag = null,
            ) == null,
        )
    }
}
