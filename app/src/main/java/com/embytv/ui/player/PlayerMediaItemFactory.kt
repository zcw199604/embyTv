package com.embytv.ui.player

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import com.embytv.domain.model.PlaybackSource
import com.embytv.domain.model.PlaybackTrack
import java.net.URI
import java.util.Locale

object PlayerMediaItemFactory {
    fun create(source: PlaybackSource): MediaItem {
        val subtitles = externalSubtitlesFor(source).map { it.toSubtitleConfiguration() }
        return MediaItem.Builder()
            .setMediaId(source.itemId)
            .setUri(source.streamUrl)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(source.title)
                    .build(),
            )
            .setSubtitleConfigurations(subtitles)
            .build()
    }

    fun externalSubtitlesFor(source: PlaybackSource): List<PlayerExternalSubtitle> =
        source.details.subtitleTracks.mapNotNull { it.toExternalSubtitle() }
}

data class PlayerExternalSubtitle(
    val url: String,
    val mimeType: String,
    val language: String?,
    val label: String,
    val selectionFlags: Int,
)

private fun PlaybackTrack.toExternalSubtitle(): PlayerExternalSubtitle? {
    val url = externalUrl?.trim()?.takeIf { isExternal && it.isNotBlank() } ?: return null
    val mimeType = codec.toSubtitleMimeType() ?: url.toSubtitleMimeTypeFromUrl() ?: return null
    return PlayerExternalSubtitle(
        url = url,
        mimeType = mimeType,
        language = language.toMedia3LanguageTag(),
        label = label,
        selectionFlags = buildSubtitleSelectionFlags(),
    )
}

private fun PlayerExternalSubtitle.toSubtitleConfiguration(): MediaItem.SubtitleConfiguration =
    MediaItem.SubtitleConfiguration.Builder(Uri.parse(url))
        .setMimeType(mimeType)
        .setLanguage(language)
        .setLabel(label)
        .setSelectionFlags(selectionFlags)
        .build()

private fun PlaybackTrack.buildSubtitleSelectionFlags(): Int {
    var flags = 0
    if (isDefault) flags = flags or C.SELECTION_FLAG_DEFAULT
    if (isForced) flags = flags or C.SELECTION_FLAG_FORCED
    return flags
}

private fun String?.toSubtitleMimeType(): String? =
    when (this?.trim()?.lowercase(Locale.US)) {
        "srt", "subrip" -> MimeTypes.APPLICATION_SUBRIP
        "vtt", "webvtt" -> MimeTypes.TEXT_VTT
        "ass", "ssa" -> MimeTypes.TEXT_SSA
        else -> null
    }

private fun String.toSubtitleMimeTypeFromUrl(): String? {
    val path = runCatching { URI(this).path }.getOrNull()
        ?: substringBefore('?').substringBefore('#')
    val extension = path.substringAfterLast('/').substringAfterLast('.', missingDelimiterValue = "")
    return extension.toSubtitleMimeType()
}

private fun String?.toMedia3LanguageTag(): String? {
    val normalized = this?.trim()
        ?.replace('_', '-')
        ?.takeIf { it.isNotBlank() }
        ?: return null
    val key = normalized.lowercase(Locale.US)
    return when {
        key in setOf("chi", "zho", "zh", "zh-cn", "zh-hans", "chs", "cmn") ||
            key.startsWith("zh-hans-") ||
            key.startsWith("zh-cn-") ||
            key == "zh-sg" -> "zh-CN"
        key in setOf("zh-tw", "zh-hant", "cht", "zh-hk", "zh-mo") ||
            key.startsWith("zh-hant-") -> "zh-TW"
        key == "eng" || key == "en" -> "en"
        key == "jpn" || key == "ja" -> "ja"
        key == "kor" || key == "ko" -> "ko"
        else -> normalized.toCanonicalLanguageTag()
    }
}

private fun String.toCanonicalLanguageTag(): String =
    split('-')
        .filter { it.isNotBlank() }
        .mapIndexed { index, part ->
            when {
                index == 0 -> part.lowercase(Locale.US)
                part.length == 4 -> part.lowercase(Locale.US).replaceFirstChar { it.uppercaseChar() }
                part.length == 2 || part.length == 3 -> part.uppercase(Locale.US)
                else -> part
            }
        }
        .joinToString("-")
