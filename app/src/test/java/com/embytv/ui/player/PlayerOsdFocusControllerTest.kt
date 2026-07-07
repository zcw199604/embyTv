package com.embytv.ui.player

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerOsdFocusControllerTest {
    @Test
    fun requestsPrimaryFocusWhenOsdFirstBecomesVisible() {
        val current = PlayerOsdFocusSnapshot(
            playbackItemId = "episode-1",
            visible = true,
            interactionRevision = 0,
        )

        assertTrue(PlayerOsdFocusController.shouldRequestPrimaryFocus(previous = null, current = current))
    }

    @Test
    fun requestsPrimaryFocusWhenOsdReturnsFromHiddenState() {
        val previous = PlayerOsdFocusSnapshot(
            playbackItemId = "episode-1",
            visible = false,
            interactionRevision = 2,
        )
        val current = PlayerOsdFocusSnapshot(
            playbackItemId = "episode-1",
            visible = true,
            interactionRevision = 3,
        )

        assertTrue(PlayerOsdFocusController.shouldRequestPrimaryFocus(previous = previous, current = current))
    }

    @Test
    fun requestsPrimaryFocusWhenPlaybackItemChangesWhileVisible() {
        val previous = PlayerOsdFocusSnapshot(
            playbackItemId = "episode-1",
            visible = true,
            interactionRevision = 4,
        )
        val current = PlayerOsdFocusSnapshot(
            playbackItemId = "episode-2",
            visible = true,
            interactionRevision = 0,
        )

        assertTrue(PlayerOsdFocusController.shouldRequestPrimaryFocus(previous = previous, current = current))
    }

    @Test
    fun doesNotStealFocusForInteractionsInsideVisibleOsd() {
        val previous = PlayerOsdFocusSnapshot(
            playbackItemId = "episode-1",
            visible = true,
            interactionRevision = 4,
        )
        val current = PlayerOsdFocusSnapshot(
            playbackItemId = "episode-1",
            visible = true,
            interactionRevision = 5,
        )

        assertFalse(PlayerOsdFocusController.shouldRequestPrimaryFocus(previous = previous, current = current))
    }

    @Test
    fun requestsPrimaryFocusWhenQuickPanelCloses() {
        val previous = PlayerOsdFocusSnapshot(
            playbackItemId = "episode-1",
            visible = true,
            interactionRevision = 4,
            selectedQuickPanel = PlayerQuickPanel.Subtitles,
        )
        val current = PlayerOsdFocusSnapshot(
            playbackItemId = "episode-1",
            visible = true,
            interactionRevision = 5,
            selectedQuickPanel = null,
        )

        assertTrue(PlayerOsdFocusController.shouldRequestPrimaryFocus(previous = previous, current = current))
    }

    @Test
    fun doesNotRequestPrimaryFocusWhileOsdIsHidden() {
        val previous = PlayerOsdFocusSnapshot(
            playbackItemId = "episode-1",
            visible = true,
            interactionRevision = 4,
        )
        val current = PlayerOsdFocusSnapshot(
            playbackItemId = "episode-1",
            visible = false,
            interactionRevision = 4,
        )

        assertFalse(PlayerOsdFocusController.shouldRequestPrimaryFocus(previous = previous, current = current))
    }
}
