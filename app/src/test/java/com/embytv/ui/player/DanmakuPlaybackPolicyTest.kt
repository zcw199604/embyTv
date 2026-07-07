package com.embytv.ui.player

import org.junit.Assert.assertEquals
import org.junit.Test

class DanmakuPlaybackPolicyTest {
    @Test
    fun playbackCommandPausesWhenDisabledOrPaused() {
        val settings = DanmakuOverlaySettings()

        assertEquals(
            DanmakuPlaybackCommand.Pause,
            DanmakuPlaybackPolicy.commandForPlayback(settings, enabled = false, paused = false),
        )
        assertEquals(
            DanmakuPlaybackCommand.Pause,
            DanmakuPlaybackPolicy.commandForPlayback(settings, enabled = true, paused = true),
        )
    }

    @Test
    fun playbackCommandStartsWithNormalizedConfigWhenEnabledAndPlaying() {
        val settings = DanmakuOverlaySettings(
            opacity = 1.6f,
            textSizeScale = 2.4f,
            displayArea = DanmakuDisplayArea.Top,
        )

        val command = DanmakuPlaybackPolicy.commandForPlayback(
            settings = settings,
            enabled = true,
            paused = false,
        ) as DanmakuPlaybackCommand.Start

        assertEquals(1f, command.settings.opacity, 0.001f)
        assertEquals(1.6f, command.settings.textSizeScale, 0.001f)
        assertEquals(DanmakuDisplayArea.Top, command.settings.displayArea)
        assertEquals(1.6f, command.config.textSizeScale, 0.001f)
        assertEquals(DanmakuDisplayArea.Top.screenPart, command.config.screenPart, 0.001f)
    }

    @Test
    fun playbackCommandFallsBackWhenSettingsContainNaNValues() {
        val settings = DanmakuOverlaySettings(
            opacity = Float.NaN,
            textSizeScale = Float.NaN,
            displayArea = DanmakuDisplayArea.Full,
        )

        val command = DanmakuPlaybackPolicy.commandForPlayback(
            settings = settings,
            enabled = true,
            paused = false,
        ) as DanmakuPlaybackCommand.Start

        assertEquals(1f, command.settings.opacity, 0.001f)
        assertEquals(1.15f, command.settings.textSizeScale, 0.001f)
        assertEquals(1.15f, command.config.textSizeScale, 0.001f)
    }

    @Test
    fun playbackConfigKeyIgnoresOpacityOnlyChanges() {
        val key = DanmakuOverlaySettings(
            opacity = 0.2f,
            textSizeScale = 1.15f,
            displayArea = DanmakuDisplayArea.Full,
        ).playbackConfigKey()
        val opacityOnly = DanmakuOverlaySettings(
            opacity = 1f,
            textSizeScale = 1.15f,
            displayArea = DanmakuDisplayArea.Full,
        ).playbackConfigKey()

        assertEquals(key, opacityOnly)
    }

    @Test
    fun playbackConfigKeyTracksTextSizeAndDisplayAreaChanges() {
        val key = DanmakuOverlaySettings(
            textSizeScale = 1.15f,
            displayArea = DanmakuDisplayArea.Full,
        ).playbackConfigKey()
        val textSizeChanged = DanmakuOverlaySettings(
            textSizeScale = 1.4f,
            displayArea = DanmakuDisplayArea.Full,
        ).playbackConfigKey()
        val displayAreaChanged = DanmakuOverlaySettings(
            textSizeScale = 1.15f,
            displayArea = DanmakuDisplayArea.Top,
        ).playbackConfigKey()

        assertEquals(false, key == textSizeChanged)
        assertEquals(false, key == displayAreaChanged)
    }

    @Test
    fun syncCommandClearsAndSeeksToRequestedPosition() {
        assertEquals(
            DanmakuSyncCommand.ClearAndSeek(positionMs = 42_500L),
            DanmakuPlaybackPolicy.commandForSeek(positionMs = 42_500L),
        )
    }

    @Test
    fun syncCommandClampsNegativeSeekToStart() {
        assertEquals(
            DanmakuSyncCommand.ClearAndSeek(positionMs = 0L),
            DanmakuPlaybackPolicy.commandForSeek(positionMs = -300L),
        )
    }
}
