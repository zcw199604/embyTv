package com.embytv.domain.model

import java.net.URI
import java.util.Locale

data class PlaybackSource(
    val itemId: String,
    val title: String,
    val streamUrl: String,
    val playlistItemId: String? = null,
    val session: EmbySession? = null,
    val deviceId: String? = null,
    val details: PlaybackDetails = PlaybackDetails(),
    val queue: PlaybackQueue? = null,
    val danmaku: List<DanmakuCue> = emptyList(),
    val previewThumbnailUrl: String? = null,
    val seekThumbnails: List<SeekThumbnail> = emptyList(),
    val startPositionMs: Long = 0L,
    val contextLabel: String? = null,
)

data class SeekThumbnail(
    val positionMs: Long,
    val imageUrl: String,
)

fun PlaybackSource.previewThumbnailFor(positionMs: Long): String? {
    val normalizedPosition = positionMs.coerceAtLeast(0L)
    return seekThumbnails
        .filter { it.imageUrl.isNotBlank() }
        .sortedBy { it.positionMs }
        .let { thumbnails ->
            thumbnails.lastOrNull { it.positionMs <= normalizedPosition }
                ?: thumbnails.firstOrNull()
        }
        ?.imageUrl
        ?.trim()
        ?: previewThumbnailUrl?.trim()?.takeIf { it.isNotBlank() }
}

data class PlaybackQueue(
    val previous: MediaItemSummary? = null,
    val current: MediaItemSummary,
    val next: MediaItemSummary? = null,
    val autoPlayNext: Boolean = true,
) {
    companion object {
        fun from(items: List<MediaItemSummary>, currentId: String, autoPlayNext: Boolean = true): PlaybackQueue? {
            val index = items.indexOfFirst { it.id == currentId }
            if (index < 0) return null
            return PlaybackQueue(
                previous = items.getOrNull(index - 1),
                current = items[index],
                next = items.getOrNull(index + 1),
                autoPlayNext = autoPlayNext,
            )
        }
    }
}

data class PlayerTrackOption(
    val id: String,
    val label: String,
    val type: PlayerTrackType,
    val trackGroup: androidx.media3.common.TrackGroup,
    val trackIndex: Int,
    val selected: Boolean = false,
)

enum class PlayerTrackType {
    Audio,
    Subtitle,
}

data class PlaybackDetails(
    val playSessionId: String? = null,
    val mediaSourceId: String? = null,
    val container: String? = null,
    val bitrate: Int? = null,
    val video: PlaybackVideoStream? = null,
    val audioTracks: List<PlaybackTrack> = emptyList(),
    val subtitleTracks: List<PlaybackTrack> = emptyList(),
) {
    val bitrateLabel: String?
        get() = bitrate.toMegabitsLabel()

    val playbackSummaryLabel: String
        get() = listOfNotNull(
            "Direct Play",
            container?.uppercase(Locale.US),
            video?.codec?.uppercase(Locale.US),
        ).joinToString(" · ")

    val qualityLabel: String
        get() {
            val height = video?.height?.takeIf { it > 0 }?.let { "${it}p" }
            val range = video?.videoRange?.takeIf { it.isNotBlank() && !it.equals("SDR", ignoreCase = true) }
            return listOfNotNull(height, range, bitrateLabel)
                .joinToString(" · ")
                .ifBlank { "未知画质" }
        }

    val audioLabel: String
        get() = audioTracks.firstOrNull { it.isDefault }?.label
            ?: audioTracks.firstOrNull()?.label
            ?: "无音轨信息"

    val subtitleLabel: String
        get() = subtitleTracks.firstOrNull { it.isDefault }?.label
            ?: subtitleTracks.firstOrNull()?.label
            ?: "无字幕"
}

data class PlaybackOverlayDetails(
    val mediaDetail: EmbyMediaDetail,
    val playbackDetails: PlaybackDetails,
)

private fun Int?.toMegabitsLabel(): String? =
    this?.takeIf { it > 0 }?.let { String.format(Locale.US, "%.1f Mbps", it / 1_000_000.0) }

data class PlaybackVideoStream(
    val codec: String? = null,
    val width: Int? = null,
    val height: Int? = null,
    val videoRange: String? = null,
)

data class PlaybackTrack(
    val index: Int,
    val codec: String? = null,
    val displayTitle: String? = null,
    val channels: Int? = null,
    val language: String? = null,
    val isDefault: Boolean = false,
    val isForced: Boolean = false,
    val isExternal: Boolean = false,
    val deliveryMethod: String? = null,
    val externalUrl: String? = null,
) {
    val label: String
        get() {
            val normalizedDisplayTitle = displayTitle?.trim()?.takeIf { it.isNotBlank() }
            if (!isExternal && normalizedDisplayTitle != null) return normalizedDisplayTitle
            val languageLabel = language.toDisplayLanguageLabel()
            val codecLabel = if (isExternal) {
                codec.toSubtitleFormatLabel() ?: externalUrl.toSubtitleFormatLabelFromUrl()
            } else {
                codec?.uppercase(Locale.US)
            }
            return if (isExternal) {
                listOfNotNull(
                    languageLabel ?: normalizedDisplayTitle,
                    codecLabel,
                    "External",
                ).joinToString(" · ").ifBlank { "External Track $index" }
            } else {
                listOfNotNull(codecLabel, languageLabel ?: language).joinToString(" ").ifBlank { "Track $index" }
            }
        }
}

private fun String?.toDisplayLanguageLabel(): String? {
    val normalized = this?.trim()
        ?.replace('_', '-')
        ?.takeIf { it.isNotBlank() }
        ?: return null
    val key = normalized.lowercase(Locale.US)
    return when {
        key in setOf("chi", "zho", "zh", "zh-cn", "zh-hans", "chs", "cmn") ||
            key.startsWith("zh-hans-") ||
            key.startsWith("zh-cn-") ||
            key == "zh-sg" -> "Chinese (Simplified)"
        key in setOf("zh-tw", "zh-hant", "cht", "zh-hk", "zh-mo") ||
            key.startsWith("zh-hant-") -> "Chinese (Traditional)"
        key == "eng" || key == "en" || key.startsWith("en-") -> "English"
        key == "jpn" || key == "ja" || key.startsWith("ja-") -> "Japanese"
        key == "kor" || key == "ko" || key.startsWith("ko-") -> "Korean"
        key == "spa" || key == "es" || key.startsWith("es-") -> "Spanish"
        key in setOf("fre", "fra", "fr") || key.startsWith("fr-") -> "French"
        key in setOf("ger", "deu", "de") || key.startsWith("de-") -> "German"
        else -> normalized
    }
}

private fun String?.toSubtitleFormatLabel(): String? =
    when (this?.trim()?.lowercase(Locale.US)) {
        "srt", "subrip" -> "SRT"
        "vtt", "webvtt" -> "VTT"
        "ass", "ssa" -> "ASS"
        else -> this?.takeIf { it.isNotBlank() }?.uppercase(Locale.US)
    }

private fun String?.toSubtitleFormatLabelFromUrl(): String? {
    val url = this?.takeIf { it.isNotBlank() } ?: return null
    val path = runCatching { URI(url).path }.getOrNull()
        ?: url.substringBefore('?').substringBefore('#')
    val extension = path.substringAfterLast('/').substringAfterLast('.', missingDelimiterValue = "")
    return extension.toSubtitleFormatLabel()
}

data class DanmakuCue(
    val id: Long,
    val timeMs: Long,
    val text: String,
    val color: Int = 0xFFFFFF,
    val mode: DanmakuMode = DanmakuMode.Rolling,
)

enum class DanmakuMode {
    Rolling,
    Top,
    Bottom,
}
