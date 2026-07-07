package com.embytv.ui.player

import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import androidx.media3.common.TrackGroup
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerQuickPanelFocusPolicyTest {
    @Test
    fun requestsFocusWhenPanelOpensWithFocusableOptions() {
        val previous = PlayerQuickPanelFocusSnapshot(
            visible = true,
            selectedQuickPanel = null,
            focusableOptionCount = 0,
        )
        val current = PlayerQuickPanelFocusSnapshot(
            visible = true,
            selectedQuickPanel = PlayerQuickPanel.Speed,
            focusableOptionCount = 6,
        )

        assertTrue(PlayerQuickPanelFocusPolicy.shouldRequestPanelFocus(previous, current))
    }

    @Test
    fun requestsFocusWhenSwitchingBetweenPanels() {
        val previous = PlayerQuickPanelFocusSnapshot(
            visible = true,
            selectedQuickPanel = PlayerQuickPanel.Audio,
            focusableOptionCount = 2,
        )
        val current = PlayerQuickPanelFocusSnapshot(
            visible = true,
            selectedQuickPanel = PlayerQuickPanel.Subtitles,
            focusableOptionCount = 3,
        )

        assertTrue(PlayerQuickPanelFocusPolicy.shouldRequestPanelFocus(previous, current))
    }

    @Test
    fun doesNotRequestFocusForSameOpenPanelInteraction() {
        val previous = PlayerQuickPanelFocusSnapshot(
            visible = true,
            selectedQuickPanel = PlayerQuickPanel.Danmaku,
            focusableOptionCount = 9,
        )
        val current = PlayerQuickPanelFocusSnapshot(
            visible = true,
            selectedQuickPanel = PlayerQuickPanel.Danmaku,
            focusableOptionCount = 9,
        )

        assertFalse(PlayerQuickPanelFocusPolicy.shouldRequestPanelFocus(previous, current))
    }

    @Test
    fun requestsFocusWhenSamePanelReceivesDelayedOptions() {
        val previous = PlayerQuickPanelFocusSnapshot(
            visible = true,
            selectedQuickPanel = PlayerQuickPanel.Audio,
            focusableOptionCount = 0,
        )
        val current = PlayerQuickPanelFocusSnapshot(
            visible = true,
            selectedQuickPanel = PlayerQuickPanel.Audio,
            focusableOptionCount = 2,
        )

        assertTrue(PlayerQuickPanelFocusPolicy.shouldRequestPanelFocus(previous, current))
    }

    @Test
    fun doesNotRequestFocusWhenPanelHasNoFocusableOptions() {
        val current = PlayerQuickPanelFocusSnapshot(
            visible = true,
            selectedQuickPanel = PlayerQuickPanel.Audio,
            focusableOptionCount = 0,
        )

        assertFalse(PlayerQuickPanelFocusPolicy.shouldRequestPanelFocus(previous = null, current = current))
    }

    @Test
    fun countsFocusableOptionsForEachPanel() {
        val state = PlayerOsdState(
            selectedQuickPanel = PlayerQuickPanel.Subtitles,
            subtitleTracks = listOf(track("s1"), track("s2")),
        )

        assertTrue(PlayerQuickPanelFocusPolicy.focusableOptionCount(state) == 3)
    }

    @Test
    fun countsEverySubtitleTrackAsFocusableForLargeMultiLanguageMedia() {
        val state = PlayerOsdState(
            selectedQuickPanel = PlayerQuickPanel.Subtitles,
            subtitleTracks = (1..8).map { track("s$it") },
        )

        assertTrue(PlayerQuickPanelFocusPolicy.focusableOptionCount(state) == 9)
    }

    private fun track(id: String) =
        com.embytv.domain.model.PlayerTrackOption(
            id = id,
            label = id,
            type = com.embytv.domain.model.PlayerTrackType.Subtitle,
            trackGroup = TrackGroup(
                id,
                Format.Builder()
                    .setId(id)
                    .setSampleMimeType(MimeTypes.APPLICATION_SUBRIP)
                    .build(),
            ),
            trackIndex = 0,
            selected = false,
        )
}
