package com.embytv.ui.player

data class PlayerLifecyclePlaybackSnapshot(
    val isPlaying: Boolean,
    val danmakuEnabled: Boolean,
    val danmakuPaused: Boolean,
    val danmakuSettings: DanmakuOverlaySettings,
)

sealed interface PlayerLifecyclePlaybackEffect {
    data object PlayPlayer : PlayerLifecyclePlaybackEffect
    data object PausePlayer : PlayerLifecyclePlaybackEffect
    data object PauseDanmaku : PlayerLifecyclePlaybackEffect
    data class ReportPauseChanged(val isPaused: Boolean) : PlayerLifecyclePlaybackEffect
    data class ApplyDanmaku(val command: DanmakuPlaybackCommand) : PlayerLifecyclePlaybackEffect
}

object PlayerLifecyclePlaybackPolicy {
    fun onResume(snapshot: PlayerLifecyclePlaybackSnapshot): List<PlayerLifecyclePlaybackEffect> =
        buildList {
            if (snapshot.isPlaying) {
                add(PlayerLifecyclePlaybackEffect.PlayPlayer)
                add(PlayerLifecyclePlaybackEffect.ReportPauseChanged(isPaused = false))
            }
            add(
                PlayerLifecyclePlaybackEffect.ApplyDanmaku(
                    DanmakuPlaybackPolicy.commandForPlayback(
                        settings = snapshot.danmakuSettings,
                        enabled = snapshot.danmakuEnabled,
                        paused = snapshot.danmakuPaused,
                    ),
                ),
            )
        }

    fun onPause(): List<PlayerLifecyclePlaybackEffect> =
        listOf(
            PlayerLifecyclePlaybackEffect.ReportPauseChanged(isPaused = true),
            PlayerLifecyclePlaybackEffect.PausePlayer,
            PlayerLifecyclePlaybackEffect.PauseDanmaku,
        )
}
