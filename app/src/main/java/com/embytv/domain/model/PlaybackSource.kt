package com.embytv.domain.model

data class PlaybackSource(
    val itemId: String,
    val title: String,
    val streamUrl: String,
    val session: EmbySession? = null,
    val deviceId: String? = null,
    val details: PlaybackDetails = PlaybackDetails(),
    val queue: PlaybackQueue? = null,
    val danmaku: List<DanmakuCue> = emptyList(),
)

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
    val playbackSummaryLabel: String
        get() = listOfNotNull(
            "Direct Play",
            container?.uppercase(),
            video?.codec?.uppercase(),
        ).joinToString(" · ")

    val qualityLabel: String
        get() {
            val height = video?.height?.takeIf { it > 0 }?.let { "${it}p" }
            val range = video?.videoRange?.takeIf { it.isNotBlank() && !it.equals("SDR", ignoreCase = true) }
            return listOfNotNull(height, range).joinToString(" · ").ifBlank { "未知画质" }
        }

    val audioLabel: String
        get() = audioTracks.firstOrNull { it.isDefault }?.displayTitle
            ?: audioTracks.firstOrNull()?.displayTitle
            ?: "无音轨信息"

    val subtitleLabel: String
        get() = subtitleTracks.firstOrNull { it.isDefault }?.displayTitle
            ?: subtitleTracks.firstOrNull()?.displayTitle
            ?: "无字幕"
}

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
) {
    val label: String
        get() = displayTitle
            ?: listOfNotNull(codec?.uppercase(), language).joinToString(" ").ifBlank { "Track $index" }
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
