package com.embytv.domain.model

data class PlaybackSource(
    val itemId: String,
    val title: String,
    val streamUrl: String,
    val danmaku: List<DanmakuCue> = emptyList(),
)

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
