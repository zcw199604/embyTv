package com.embytv.ui.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerLifecyclePlaybackPolicyTest {
    @Test
    fun pauseReportsPausedAndStopsPlayerAndDanmaku() {
        val effects = PlayerLifecyclePlaybackPolicy.onPause()

        assertEquals(
            listOf(
                PlayerLifecyclePlaybackEffect.ReportPauseChanged(isPaused = true),
                PlayerLifecyclePlaybackEffect.PausePlayer,
                PlayerLifecyclePlaybackEffect.PauseDanmaku,
            ),
            effects,
        )
    }

    @Test
    fun resumeDoesNotRestartPlayerWhenLatestOsdStateIsPaused() {
        val effects = PlayerLifecyclePlaybackPolicy.onResume(
            PlayerLifecyclePlaybackSnapshot(
                isPlaying = false,
                danmakuEnabled = true,
                danmakuPaused = true,
                danmakuSettings = DanmakuOverlaySettings(),
            ),
        )

        assertEquals(
            listOf(
                PlayerLifecyclePlaybackEffect.ApplyDanmaku(DanmakuPlaybackCommand.Pause),
            ),
            effects,
        )
    }

    @Test
    fun resumeRestartsPlayerAndDanmakuOnlyWhenLatestOsdStateIsPlaying() {
        val effects = PlayerLifecyclePlaybackPolicy.onResume(
            PlayerLifecyclePlaybackSnapshot(
                isPlaying = true,
                danmakuEnabled = true,
                danmakuPaused = false,
                danmakuSettings = DanmakuOverlaySettings(opacity = 0.8f),
            ),
        )

        assertEquals(PlayerLifecyclePlaybackEffect.PlayPlayer, effects[0])
        assertEquals(PlayerLifecyclePlaybackEffect.ReportPauseChanged(isPaused = false), effects[1])
        assertTrue(effects[2] is PlayerLifecyclePlaybackEffect.ApplyDanmaku)
        assertTrue((effects[2] as PlayerLifecyclePlaybackEffect.ApplyDanmaku).command is DanmakuPlaybackCommand.Start)
    }
}
