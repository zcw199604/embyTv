package com.embytv.ui.player

import com.embytv.domain.model.MediaItemSummary
import com.embytv.domain.model.PlaybackQueue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerQueueNavigationPolicyTest {
    @Test
    fun disablesBothButtonsWhenQueueIsMissing() {
        val state = PlayerQueueNavigationPolicy.resolve(
            queue = null,
            noPreviousReason = "没有上一集",
            noNextReason = "没有下一集",
        )

        assertFalse(state.previous.enabled)
        assertFalse(state.next.enabled)
        assertEquals("没有上一集", state.previous.disabledReason)
        assertEquals("没有下一集", state.next.disabledReason)
        assertNull(state.previous.target)
        assertNull(state.next.target)
    }

    @Test
    fun disablesMissingDirectionsWhenQueueHasOnlyCurrentItem() {
        val state = PlayerQueueNavigationPolicy.resolve(
            queue = PlaybackQueue(current = mediaItem("episode-2")),
            noPreviousReason = "没有上一集",
            noNextReason = "没有下一集",
        )

        assertFalse(state.previous.enabled)
        assertFalse(state.next.enabled)
    }

    @Test
    fun enablesDirectionsWithAvailableQueueTargets() {
        val state = PlayerQueueNavigationPolicy.resolve(
            queue = PlaybackQueue(
                previous = mediaItem("episode-1"),
                current = mediaItem("episode-2"),
                next = mediaItem("episode-3"),
            ),
            noPreviousReason = "没有上一集",
            noNextReason = "没有下一集",
        )

        assertTrue(state.previous.enabled)
        assertTrue(state.next.enabled)
        assertEquals(null, state.previous.disabledReason)
        assertEquals(null, state.next.disabledReason)
        assertEquals("episode-1", state.previous.target?.id)
        assertEquals("episode-3", state.next.target?.id)
    }

    @Test
    fun keepsManualNextEnabledWhenAutoPlayNextIsDisabled() {
        val state = PlayerQueueNavigationPolicy.resolve(
            queue = PlaybackQueue(
                current = mediaItem("episode-2"),
                next = mediaItem("episode-3"),
                autoPlayNext = false,
            ),
            noPreviousReason = "没有上一集",
            noNextReason = "没有下一集",
        )

        assertTrue(state.next.enabled)
        assertEquals("episode-3", state.next.target?.id)
        assertNull(state.autoPlayNextTarget)
    }

    @Test
    fun exposesAutoPlayNextTargetOnlyWhenEnabledAndAvailable() {
        val state = PlayerQueueNavigationPolicy.resolve(
            queue = PlaybackQueue(
                current = mediaItem("episode-2"),
                next = mediaItem("episode-3"),
                autoPlayNext = true,
            ),
            noPreviousReason = "没有上一集",
            noNextReason = "没有下一集",
        )

        assertEquals("episode-3", state.autoPlayNextTarget?.id)
    }

    @Test
    fun disablesDirectionsThatPointToCurrentItem() {
        val state = PlayerQueueNavigationPolicy.resolve(
            queue = PlaybackQueue(
                previous = mediaItem("episode-2"),
                current = mediaItem("episode-2"),
                next = mediaItem("episode-2"),
                autoPlayNext = true,
            ),
            noPreviousReason = "没有上一集",
            noNextReason = "没有下一集",
        )

        assertFalse(state.previous.enabled)
        assertFalse(state.next.enabled)
        assertEquals("没有上一集", state.previous.disabledReason)
        assertEquals("没有下一集", state.next.disabledReason)
        assertNull(state.previous.target)
        assertNull(state.next.target)
        assertNull(state.autoPlayNextTarget)
    }

    private fun mediaItem(id: String): MediaItemSummary =
        MediaItemSummary(
            id = id,
            name = id,
            type = "Episode",
            overview = null,
            imageUrl = null,
        )
}
