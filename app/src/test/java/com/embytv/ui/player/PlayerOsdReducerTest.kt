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
            PlayerOsdAction.ProgressChanged(positionMs = 1_000, durationMs = 4_000),
        ).state

        assertEquals(1_000, state.positionMs)
        assertEquals(4_000, state.durationMs)
        assertEquals(0.25f, state.progressFraction)
    }
}
