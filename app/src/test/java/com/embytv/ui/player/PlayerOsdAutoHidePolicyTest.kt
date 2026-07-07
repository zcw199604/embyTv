package com.embytv.ui.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerOsdAutoHidePolicyTest {
    @Test
    fun usesFiveSecondAutoHideDelay() {
        assertEquals(5_000L, PlayerOsdAutoHidePolicy.AUTO_HIDE_DELAY_MS)
    }

    @Test
    fun schedulesAutoHideForVisibleInteractivePlaybackStates() {
        assertTrue(
            PlayerOsdAutoHidePolicy.shouldScheduleAutoHide(
                PlayerOsdAutoHideSnapshot(visible = true, status = PlaybackEngineStatus.Playing),
            ),
        )
        assertTrue(
            PlayerOsdAutoHidePolicy.shouldScheduleAutoHide(
                PlayerOsdAutoHideSnapshot(visible = true, status = PlaybackEngineStatus.Paused),
            ),
        )
    }

    @Test
    fun doesNotScheduleWhenOsdIsAlreadyHidden() {
        assertFalse(
            PlayerOsdAutoHidePolicy.shouldScheduleAutoHide(
                PlayerOsdAutoHideSnapshot(visible = false, status = PlaybackEngineStatus.Playing),
            ),
        )
    }

    @Test
    fun doesNotScheduleWhenQuickPanelIsOpen() {
        assertFalse(
            PlayerOsdAutoHidePolicy.shouldScheduleAutoHide(
                PlayerOsdAutoHideSnapshot(
                    visible = true,
                    status = PlaybackEngineStatus.Playing,
                    quickPanelOpen = true,
                ),
            ),
        )
    }

    @Test
    fun osdStateSnapshotTreatsSelectedQuickPanelAsOpenPanel() {
        val snapshot = PlayerOsdState(
            visible = true,
            status = PlaybackEngineStatus.Playing,
            selectedQuickPanel = PlayerQuickPanel.Subtitles,
        ).toAutoHideSnapshot()

        assertTrue(snapshot.quickPanelOpen)
        assertFalse(PlayerOsdAutoHidePolicy.shouldScheduleAutoHide(snapshot))
    }

    @Test
    fun keepsBlockingPlaybackStatesVisible() {
        val statuses = listOf(
            PlaybackEngineStatus.Loading,
            PlaybackEngineStatus.Buffering,
            PlaybackEngineStatus.Ended,
            PlaybackEngineStatus.Error("decode failed"),
        )

        statuses.forEach { status ->
            assertFalse(
                PlayerOsdAutoHidePolicy.shouldScheduleAutoHide(
                    PlayerOsdAutoHideSnapshot(visible = true, status = status),
                ),
            )
        }
    }
}
