package com.embytv.ui.player

import com.kuaishou.akdanmaku.DanmakuConfig

sealed interface DanmakuPlaybackCommand {
    data class Start(
        val settings: DanmakuOverlaySettings,
        val config: DanmakuConfig,
    ) : DanmakuPlaybackCommand

    data object Pause : DanmakuPlaybackCommand
}

sealed interface DanmakuSyncCommand {
    data class ClearAndSeek(val positionMs: Long) : DanmakuSyncCommand
}

data class DanmakuPlaybackConfigKey(
    val textSizeScale: Float,
    val displayArea: DanmakuDisplayArea,
)

object DanmakuPlaybackPolicy {
    fun commandForPlayback(
        settings: DanmakuOverlaySettings,
        enabled: Boolean,
        paused: Boolean,
    ): DanmakuPlaybackCommand {
        if (!enabled || paused) return DanmakuPlaybackCommand.Pause
        val normalized = settings.normalized()
        return DanmakuPlaybackCommand.Start(
            settings = normalized,
            config = normalized.toDanmakuConfig(),
        )
    }

    fun commandForSeek(positionMs: Long): DanmakuSyncCommand =
        DanmakuSyncCommand.ClearAndSeek(positionMs = positionMs.coerceAtLeast(0L))
}

fun DanmakuOverlaySettings.playbackConfigKey(): DanmakuPlaybackConfigKey =
    normalized().let { settings ->
        DanmakuPlaybackConfigKey(
            textSizeScale = settings.textSizeScale,
            displayArea = settings.displayArea,
        )
    }

fun DanmakuOverlaySettings.toDanmakuConfig(): DanmakuConfig =
    DanmakuConfig(
        textSizeScale = textSizeScale,
        screenPart = displayArea.screenPart,
        allowOverlap = false,
    )
