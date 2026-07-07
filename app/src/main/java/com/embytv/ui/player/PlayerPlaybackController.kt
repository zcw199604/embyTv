package com.embytv.ui.player

import androidx.media3.common.Player

sealed interface PlayerPlaybackEffect {
    data class ReportStopped(val positionMs: Long) : PlayerPlaybackEffect
    data class ReportSeek(val positionMs: Long, val isPaused: Boolean) : PlayerPlaybackEffect
    data class SyncDanmaku(val positionMs: Long) : PlayerPlaybackEffect
    data object PlayNext : PlayerPlaybackEffect
}

data class PlayerPlaybackUpdate(
    val actions: List<PlayerOsdAction> = emptyList(),
    val effects: List<PlayerPlaybackEffect> = emptyList(),
)

object PlayerPlaybackController {
    const val UI_PROGRESS_INTERVAL_MS = 250L

    fun onPlaybackStateChanged(
        playbackState: Int,
        isPlaying: Boolean,
        currentPositionMs: Long,
        durationMs: Long,
        shouldAutoPlayNext: Boolean,
    ): PlayerPlaybackUpdate =
        when (playbackState) {
            Player.STATE_BUFFERING -> actionUpdate(PlaybackEngineStatus.Buffering)
            Player.STATE_READY -> actionUpdate(if (isPlaying) PlaybackEngineStatus.Playing else PlaybackEngineStatus.Paused)
            Player.STATE_ENDED -> PlayerPlaybackUpdate(
                actions = listOf(PlayerOsdAction.PlaybackStatusChanged(PlaybackEngineStatus.Ended)),
                effects = buildList {
                    add(PlayerPlaybackEffect.ReportStopped(endPositionMs(currentPositionMs, durationMs)))
                    if (shouldAutoPlayNext) {
                        add(PlayerPlaybackEffect.PlayNext)
                    }
                },
            )
            Player.STATE_IDLE -> PlayerPlaybackUpdate()
            else -> PlayerPlaybackUpdate()
        }

    fun onIsPlayingChanged(isPlaying: Boolean, playbackState: Int): PlayerPlaybackUpdate =
        when {
            playbackState == Player.STATE_BUFFERING -> actionUpdate(PlaybackEngineStatus.Buffering)
            playbackState == Player.STATE_ENDED -> actionUpdate(PlaybackEngineStatus.Ended)
            isPlaying -> actionUpdate(PlaybackEngineStatus.Playing)
            else -> actionUpdate(PlaybackEngineStatus.Paused)
        }

    fun onPlayerError(message: String?, fallbackMessage: String): PlayerPlaybackUpdate =
        actionUpdate(PlaybackEngineStatus.Error(message?.takeIf { it.isNotBlank() } ?: fallbackMessage))

    fun onPositionDiscontinuity(
        newPositionMs: Long,
        isPlaying: Boolean,
        reason: Int,
    ): PlayerPlaybackUpdate {
        val position = newPositionMs.coerceAtLeast(0L)
        return PlayerPlaybackUpdate(
            effects = buildList {
                add(PlayerPlaybackEffect.SyncDanmaku(position))
                if (reason.shouldReportSeek()) {
                    add(PlayerPlaybackEffect.ReportSeek(positionMs = position, isPaused = !isPlaying))
                }
            },
        )
    }

    fun onPlaybackSpeedChanged(speed: Float): PlayerPlaybackUpdate =
        PlayerPlaybackUpdate(actions = listOf(PlayerOsdAction.SelectPlaybackSpeed(speed)))

    fun onRenderedFirstFrame(isPlaying: Boolean): PlayerPlaybackUpdate =
        actionUpdate(if (isPlaying) PlaybackEngineStatus.Playing else PlaybackEngineStatus.Paused)

    fun onProgressTick(
        positionMs: Long,
        durationMs: Long,
        bufferedFraction: Float,
    ): PlayerPlaybackUpdate =
        PlayerPlaybackUpdate(
            actions = listOf(
                PlayerOsdAction.ProgressChanged(
                    positionMs = positionMs,
                    durationMs = durationMs,
                    bufferedFraction = bufferedFraction,
                ),
            ),
        )

    private fun actionUpdate(status: PlaybackEngineStatus): PlayerPlaybackUpdate =
        PlayerPlaybackUpdate(actions = listOf(PlayerOsdAction.PlaybackStatusChanged(status)))

    private fun endPositionMs(currentPositionMs: Long, durationMs: Long): Long =
        (durationMs.takeIf { it > 0L } ?: currentPositionMs).coerceAtLeast(0L)

    private fun Int.shouldReportSeek(): Boolean =
        this == Player.DISCONTINUITY_REASON_SEEK ||
            this == Player.DISCONTINUITY_REASON_SEEK_ADJUSTMENT
}
