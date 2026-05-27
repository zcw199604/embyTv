package com.embytv.data.repository

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class EmbyStreamUrlBuilder {
    fun buildVideoStreamUrl(
        serverUrl: String,
        itemId: String,
        accessToken: String,
        staticStream: Boolean = true,
    ): String {
        val base = serverUrl.trim().trimEnd('/')
        val encodedItemId = itemId.urlEncode()
        val encodedToken = accessToken.urlEncode()
        return "$base/Videos/$encodedItemId/stream?Static=$staticStream&api_key=$encodedToken"
    }

    fun buildPrimaryImageUrl(
        serverUrl: String,
        itemId: String,
        tag: String?,
    ): String? = buildImageUrl(serverUrl, itemId, "Primary", tag)

    fun buildThumbImageUrl(
        serverUrl: String,
        itemId: String,
        tag: String?,
    ): String? = buildImageUrl(serverUrl, itemId, "Thumb", tag)

    fun buildBackdropImageUrl(
        serverUrl: String,
        itemId: String,
        tag: String?,
    ): String? {
        if (tag.isNullOrBlank()) return null
        val base = serverUrl.trim().trimEnd('/')
        return "$base/Items/${itemId.urlEncode()}/Images/Backdrop/0?tag=${tag.urlEncode()}"
    }

    private fun buildImageUrl(
        serverUrl: String,
        itemId: String,
        imageType: String,
        tag: String?,
    ): String? {
        if (tag.isNullOrBlank()) return null
        val base = serverUrl.trim().trimEnd('/')
        return "$base/Items/${itemId.urlEncode()}/Images/$imageType?tag=${tag.urlEncode()}"
    }
}

private fun String.urlEncode(): String =
    URLEncoder.encode(this, StandardCharsets.UTF_8.name())
