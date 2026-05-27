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
        allowUntagged: Boolean = false,
    ): String? = buildImageUrl(serverUrl, itemId, "Primary", tag, allowUntagged)

    fun buildThumbImageUrl(
        serverUrl: String,
        itemId: String,
        tag: String?,
        allowUntagged: Boolean = false,
    ): String? = buildImageUrl(serverUrl, itemId, "Thumb", tag, allowUntagged)

    fun buildBackdropImageUrl(
        serverUrl: String,
        itemId: String,
        tag: String?,
        allowUntagged: Boolean = false,
    ): String? {
        val base = serverUrl.trim().trimEnd('/')
        val path = "$base/Items/${itemId.urlEncode()}/Images/Backdrop/0"
        return when {
            !tag.isNullOrBlank() -> "$path?tag=${tag.urlEncode()}"
            allowUntagged -> path
            else -> null
        }
    }

    private fun buildImageUrl(
        serverUrl: String,
        itemId: String,
        imageType: String,
        tag: String?,
        allowUntagged: Boolean,
    ): String? {
        val base = serverUrl.trim().trimEnd('/')
        val path = "$base/Items/${itemId.urlEncode()}/Images/$imageType"
        return when {
            !tag.isNullOrBlank() -> "$path?tag=${tag.urlEncode()}"
            allowUntagged -> path
            else -> null
        }
    }
}

private fun String.urlEncode(): String =
    URLEncoder.encode(this, StandardCharsets.UTF_8.name())
