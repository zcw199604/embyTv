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

    fun buildChapterImageUrl(
        serverUrl: String,
        itemId: String,
        chapterIndex: Int,
        tag: String?,
        allowUntagged: Boolean = false,
        profile: EmbyImageProfile? = null,
    ): String? {
        val base = serverUrl.trim().trimEnd('/')
        val path = "$base/Items/${itemId.urlEncode()}/Images/Chapter/${chapterIndex.coerceAtLeast(0)}"
        return when {
            !tag.isNullOrBlank() -> appendImageProfile("$path?tag=${tag.urlEncode()}", profile)
            allowUntagged -> appendImageProfile(path, profile)
            else -> null
        }
    }

    fun buildSubtitleDeliveryUrl(
        serverUrl: String,
        deliveryUrl: String?,
        accessToken: String,
    ): String? {
        val rawUrl = deliveryUrl?.trim()?.takeIf { it.isNotBlank() } ?: return null
        val absoluteUrl = if (rawUrl.startsWith("http://", ignoreCase = true) || rawUrl.startsWith("https://", ignoreCase = true)) {
            rawUrl
        } else {
            "${serverUrl.trim().trimEnd('/')}/${rawUrl.trimStart('/')}"
        }
        if (absoluteUrl.hasQueryParameter("api_key") || accessToken.isBlank()) return absoluteUrl
        return absoluteUrl.appendQueryParameter("api_key", accessToken.urlEncode())
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

private fun String.hasQueryParameter(name: String): Boolean {
    val queryStart = indexOf('?')
    if (queryStart < 0) return false
    val fragmentStart = indexOf('#', startIndex = queryStart + 1).let { index ->
        if (index >= 0) index else length
    }
    return substring(queryStart + 1, fragmentStart)
        .split('&')
        .any { rawParameter ->
            rawParameter.substringBefore('=').equals(name, ignoreCase = true)
        }
}

private fun String.appendQueryParameter(name: String, encodedValue: String): String {
    val fragmentStart = indexOf('#')
    val base = if (fragmentStart >= 0) substring(0, fragmentStart) else this
    val fragment = if (fragmentStart >= 0) substring(fragmentStart) else ""
    val separator = when {
        '?' !in base -> "?"
        base.endsWith("?") || base.endsWith("&") -> ""
        else -> "&"
    }
    return "$base$separator$name=$encodedValue$fragment"
}
