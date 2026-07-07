package com.embytv.ui.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerManagerTest {
    @Test
    fun dispatchPublishesReducedStateThroughStateFlow() {
        val manager = PlayerManager()

        manager.dispatch(PlayerOsdAction.PlaybackStatusChanged(PlaybackEngineStatus.Playing))
        manager.dispatch(PlayerOsdAction.SeekPreviewRequested(deltaMs = 10_000))

        assertEquals(PlaybackEngineStatus.Playing, manager.state.value.status)
        assertEquals(10_000L, manager.state.value.seekPreview?.targetPositionMs)
        assertTrue(manager.state.value.visible)
    }

    @Test
    fun consumeExitReturnsTrueOnlyOnce() {
        val manager = PlayerManager(initialState = PlayerOsdState(visible = false))

        manager.dispatch(PlayerOsdAction.BackPressed)

        assertTrue(manager.consumeExitRequested())
        assertEquals(false, manager.consumeExitRequested())
    }

    @Test
    fun requestSeekPreviewUsesThumbnailProviderForTargetPosition() {
        val requestedPositions = mutableListOf<Long>()
        val manager = PlayerManager(
            initialState = PlayerOsdState(positionMs = 20_000, durationMs = 90_000),
            seekThumbnailProvider = { targetPositionMs ->
                requestedPositions += targetPositionMs
                "thumb-$targetPositionMs.jpg"
            },
        )

        manager.requestSeekPreview(deltaMs = 10_000)

        assertEquals(listOf(30_000L), requestedPositions)
        assertEquals(30_000L, manager.state.value.seekPreview?.targetPositionMs)
        assertEquals("thumb-30000.jpg", manager.state.value.seekPreview?.thumbnailUrl)
    }

    @Test
    fun requestSeekPreviewUsesSaturatedTargetForThumbnailProvider() {
        val requestedPositions = mutableListOf<Long>()
        val manager = PlayerManager(
            initialState = PlayerOsdState(positionMs = Long.MAX_VALUE - 5_000L, durationMs = 0L),
            seekThumbnailProvider = { targetPositionMs ->
                requestedPositions += targetPositionMs
                "thumb-$targetPositionMs.jpg"
            },
        )

        manager.requestSeekPreview(deltaMs = 10_000L)

        assertEquals(listOf(Long.MAX_VALUE), requestedPositions)
        assertEquals(Long.MAX_VALUE, manager.state.value.seekPreview?.targetPositionMs)
        assertEquals("thumb-${Long.MAX_VALUE}.jpg", manager.state.value.seekPreview?.thumbnailUrl)
    }

    @Test
    fun requestSeekPreviewIgnoresBlankThumbnailProviderResult() {
        val thumbnails = mutableMapOf(
            30_000L to "thumb-30000.jpg",
            40_000L to "   ",
        )
        val manager = PlayerManager(
            initialState = PlayerOsdState(positionMs = 20_000, durationMs = 90_000),
            seekThumbnailProvider = { targetPositionMs -> thumbnails[targetPositionMs] },
        )

        manager.requestSeekPreview(deltaMs = 10_000)
        manager.requestSeekPreview(deltaMs = 10_000)

        assertEquals(40_000L, manager.state.value.seekPreview?.targetPositionMs)
        assertEquals("thumb-30000.jpg", manager.state.value.seekPreview?.thumbnailUrl)
    }

    @Test
    fun requestSeekPreviewTrimsThumbnailProviderResult() {
        val manager = PlayerManager(
            initialState = PlayerOsdState(positionMs = 20_000, durationMs = 90_000),
            seekThumbnailProvider = { "  thumb-30000.jpg  " },
        )

        manager.requestSeekPreview(deltaMs = 10_000)

        assertEquals("thumb-30000.jpg", manager.state.value.seekPreview?.thumbnailUrl)
    }
}
