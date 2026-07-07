package com.embytv.ui.player

import androidx.media3.common.Player
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerPlaybackControllerTest {
    @Test
    fun mapsBufferingReadyAndErrorStatesToOsdActions() {
        val buffering = PlayerPlaybackController.onPlaybackStateChanged(
            playbackState = Player.STATE_BUFFERING,
            isPlaying = false,
            currentPositionMs = 5_000L,
            durationMs = 60_000L,
            shouldAutoPlayNext = false,
        )
        val readyPlaying = PlayerPlaybackController.onPlaybackStateChanged(
            playbackState = Player.STATE_READY,
            isPlaying = true,
            currentPositionMs = 5_000L,
            durationMs = 60_000L,
            shouldAutoPlayNext = false,
        )
        val error = PlayerPlaybackController.onPlayerError("decode failed", fallbackMessage = "Playback failed")

        assertEquals(
            listOf(PlayerOsdAction.PlaybackStatusChanged(PlaybackEngineStatus.Buffering)),
            buffering.actions,
        )
        assertEquals(
            listOf(PlayerOsdAction.PlaybackStatusChanged(PlaybackEngineStatus.Playing)),
            readyPlaying.actions,
        )
        assertEquals(
            listOf(PlayerOsdAction.PlaybackStatusChanged(PlaybackEngineStatus.Error("decode failed"))),
            error.actions,
        )
    }

    @Test
    fun playerErrorWithoutMessageUsesCallerProvidedFallback() {
        val update = PlayerPlaybackController.onPlayerError(
            message = null,
            fallbackMessage = "Playback failed",
        )

        assertEquals(
            listOf(PlayerOsdAction.PlaybackStatusChanged(PlaybackEngineStatus.Error("Playback failed"))),
            update.actions,
        )
    }

    @Test
    fun playerErrorWithBlankMessageUsesCallerProvidedFallback() {
        val update = PlayerPlaybackController.onPlayerError(
            message = "   ",
            fallbackMessage = "Playback failed",
        )

        assertEquals(
            listOf(PlayerOsdAction.PlaybackStatusChanged(PlaybackEngineStatus.Error("Playback failed"))),
            update.actions,
        )
    }

    @Test
    fun endedStateReportsStoppedAndAutoPlayNextWhenAvailable() {
        val update = PlayerPlaybackController.onPlaybackStateChanged(
            playbackState = Player.STATE_ENDED,
            isPlaying = false,
            currentPositionMs = 58_000L,
            durationMs = 60_000L,
            shouldAutoPlayNext = true,
        )

        assertEquals(
            listOf(
                PlayerPlaybackEffect.ReportStopped(positionMs = 60_000L),
                PlayerPlaybackEffect.PlayNext,
            ),
            update.effects,
        )
        assertEquals(
            listOf(PlayerOsdAction.PlaybackStatusChanged(PlaybackEngineStatus.Ended)),
            update.actions,
        )
    }

    @Test
    fun isPlayingFalseWhileBufferingKeepsBufferingStateInsteadOfPaused() {
        val update = PlayerPlaybackController.onIsPlayingChanged(
            isPlaying = false,
            playbackState = Player.STATE_BUFFERING,
        )

        assertEquals(
            listOf(PlayerOsdAction.PlaybackStatusChanged(PlaybackEngineStatus.Buffering)),
            update.actions,
        )
    }

    @Test
    fun isPlayingFalseAfterEndedKeepsEndedStateInsteadOfPaused() {
        val update = PlayerPlaybackController.onIsPlayingChanged(
            isPlaying = false,
            playbackState = Player.STATE_ENDED,
        )

        assertEquals(
            listOf(PlayerOsdAction.PlaybackStatusChanged(PlaybackEngineStatus.Ended)),
            update.actions,
        )
    }

    @Test
    fun renderedFirstFrameWhileNotPlayingSettlesOsdToPaused() {
        val update = PlayerPlaybackController.onRenderedFirstFrame(isPlaying = false)

        assertEquals(
            listOf(PlayerOsdAction.PlaybackStatusChanged(PlaybackEngineStatus.Paused)),
            update.actions,
        )
    }

    @Test
    fun positionDiscontinuitySynchronizesDanmakuAndReportsSeek() {
        val update = PlayerPlaybackController.onPositionDiscontinuity(
            newPositionMs = 42_500L,
            isPlaying = false,
            reason = Player.DISCONTINUITY_REASON_SEEK,
        )

        assertEquals(
            listOf(
                PlayerPlaybackEffect.SyncDanmaku(positionMs = 42_500L),
                PlayerPlaybackEffect.ReportSeek(positionMs = 42_500L, isPaused = true),
            ),
            update.effects,
        )
    }

    @Test
    fun nonSeekPositionDiscontinuitySynchronizesDanmakuWithoutReportingSeek() {
        val update = PlayerPlaybackController.onPositionDiscontinuity(
            newPositionMs = 42_500L,
            isPlaying = true,
            reason = Player.DISCONTINUITY_REASON_AUTO_TRANSITION,
        )

        assertEquals(
            listOf(PlayerPlaybackEffect.SyncDanmaku(positionMs = 42_500L)),
            update.effects,
        )
    }

    @Test
    fun seekAdjustmentDiscontinuityStillReportsSeek() {
        val update = PlayerPlaybackController.onPositionDiscontinuity(
            newPositionMs = 42_500L,
            isPlaying = true,
            reason = Player.DISCONTINUITY_REASON_SEEK_ADJUSTMENT,
        )

        assertEquals(
            listOf(
                PlayerPlaybackEffect.SyncDanmaku(positionMs = 42_500L),
                PlayerPlaybackEffect.ReportSeek(positionMs = 42_500L, isPaused = false),
            ),
            update.effects,
        )
    }

    @Test
    fun progressTickUsesHighFrequencyUiRefreshWithoutForcingReportingInterval() {
        val update = PlayerPlaybackController.onProgressTick(
            positionMs = 1_250L,
            durationMs = 5_000L,
            bufferedFraction = 0.25f,
        )

        assertTrue(PlayerPlaybackController.UI_PROGRESS_INTERVAL_MS <= 250L)
        assertEquals(
            listOf(
                PlayerOsdAction.ProgressChanged(
                    positionMs = 1_250L,
                    durationMs = 5_000L,
                    bufferedFraction = 0.25f,
                ),
            ),
            update.actions,
        )
        assertEquals(emptyList<PlayerPlaybackEffect>(), update.effects)
    }
}
