package com.embytv.ui.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerOsdReducerTest {
    @Test
    fun backHidesVisibleOsdBeforeExit() {
        val state = PlayerOsdState(visible = true)

        val result = PlayerOsdReducer.reduce(state, PlayerOsdAction.BackPressed)

        assertFalse(result.state.visible)
        assertFalse(result.exitPlayer)
    }

    @Test
    fun backExitsWhenOsdAlreadyHidden() {
        val state = PlayerOsdState(visible = false)

        val result = PlayerOsdReducer.reduce(state, PlayerOsdAction.BackPressed)

        assertTrue(result.exitPlayer)
    }

    @Test
    fun interactionShowsOsd() {
        val state = PlayerOsdState(visible = false)

        val result = PlayerOsdReducer.reduce(state, PlayerOsdAction.UserInteraction)

        assertTrue(result.state.visible)
    }

    @Test
    fun unsupportedActionsKeepOsdVisibleAndExposeFeedback() {
        val result = PlayerOsdReducer.reduce(
            PlayerOsdState(visible = true),
            PlayerOsdAction.UnsupportedAction("Audio 暂未支持"),
        )

        assertTrue(result.state.visible)
        assertEquals("Audio 暂未支持", result.state.feedbackMessage)
        assertFalse(result.exitPlayer)
    }

    @Test
    fun clearingFeedbackDoesNotHideOsd() {
        val result = PlayerOsdReducer.reduce(
            PlayerOsdState(visible = true, feedbackMessage = "暂未支持"),
            PlayerOsdAction.ClearFeedback,
        )

        assertTrue(result.state.visible)
        assertEquals(null, result.state.feedbackMessage)
    }

    @Test
    fun togglesPlaybackAndDanmakuState() {
        val paused = PlayerOsdReducer.reduce(
            PlayerOsdState(isPlaying = true, danmakuEnabled = true),
            PlayerOsdAction.TogglePlayPause,
        ).state
        val hiddenDanmaku = PlayerOsdReducer.reduce(paused, PlayerOsdAction.ToggleDanmaku).state

        assertFalse(paused.isPlaying)
        assertTrue(paused.danmakuPaused)
        assertFalse(hiddenDanmaku.danmakuEnabled)
        assertTrue(hiddenDanmaku.danmakuPaused)
    }

    @Test
    fun updatesProgress() {
        val state = PlayerOsdReducer.reduce(
            PlayerOsdState(),
            PlayerOsdAction.ProgressChanged(positionMs = 1_000, durationMs = 4_000, bufferedFraction = 0.5f),
        ).state

        assertEquals(1_000, state.positionMs)
        assertEquals(4_000, state.durationMs)
        assertEquals(0.25f, state.progressFraction)
        assertEquals(0.5f, state.bufferedFraction)
    }

    @Test
    fun clampsBufferedProgress() {
        val state = PlayerOsdReducer.reduce(
            PlayerOsdState(),
            PlayerOsdAction.ProgressChanged(positionMs = -1_000, durationMs = -4_000, bufferedFraction = 1.5f),
        ).state

        assertEquals(0, state.positionMs)
        assertEquals(0, state.durationMs)
        assertEquals(1f, state.bufferedFraction)
    }
}
