package com.embytv.data.repository

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

enum class EmbyImageProfile(
    val maxWidth: Int,
    val maxHeight: Int,
    val quality: Int,
) {
    Poster(maxWidth = 500, maxHeight = 750, quality = 85),
    Thumb(maxWidth = 640, maxHeight = 360, quality = 85),
    Backdrop(maxWidth = 960, maxHeight = 540, quality = 85),
    DetailPoster(maxWidth = 780, maxHeight = 1170, quality = 90),
    DetailBackdrop(maxWidth = 1280, maxHeight = 720, quality = 90),
}

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
        profile: EmbyImageProfile? = null,
    ): String? = buildImageUrl(serverUrl, itemId, "Primary", tag, allowUntagged, profile)

    fun buildThumbImageUrl(
        serverUrl: String,
        itemId: String,
        tag: String?,
        allowUntagged: Boolean = false,
        profile: EmbyImageProfile? = null,
    ): String? = buildImageUrl(serverUrl, itemId, "Thumb", tag, allowUntagged, profile)

    fun buildBackdropImageUrl(
        serverUrl: String,
        itemId: String,
        tag: String?,
        allowUntagged: Boolean = false,
        profile: EmbyImageProfile? = null,
    ): String? {
        val base = serverUrl.trim().trimEnd('/')
        val path = "$base/Items/${itemId.urlEncode()}/Images/Backdrop/0"
        return when {
            !tag.isNullOrBlank() -> appendImageProfile("$path?tag=${tag.urlEncode()}", profile)
            allowUntagged -> appendImageProfile(path, profile)
            else -> null
        }
    }

    private fun buildImageUrl(
        serverUrl: String,
        itemId: String,
        imageType: String,
        tag: String?,
        allowUntagged: Boolean,
        profile: EmbyImageProfile?,
    ): String? {
        val base = serverUrl.trim().trimEnd('/')
        val path = "$base/Items/${itemId.urlEncode()}/Images/$imageType"
        return when {
            !tag.isNullOrBlank() -> appendImageProfile("$path?tag=${tag.urlEncode()}", profile)
            allowUntagged -> appendImageProfile(path, profile)
            else -> null
        }
    }

    private fun appendImageProfile(url: String, profile: EmbyImageProfile?): String {
        if (profile == null) return url
        val separator = if ("?" in url) "&" else "?"
        return "$url${separator}MaxWidth=${profile.maxWidth}&MaxHeight=${profile.maxHeight}&Quality=${profile.quality}"
    }
}

private fun String.urlEncode(): String =
    URLEncoder.encode(this, StandardCharsets.UTF_8.name())
