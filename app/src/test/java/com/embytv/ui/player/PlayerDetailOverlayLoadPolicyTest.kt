package com.embytv.ui.player

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerDetailOverlayLoadPolicyTest {
    @Test
    fun displaysDetailsWhenOsdIsVisible() {
        val snapshot = PlayerDetailOverlayVisibilitySnapshot(
            osdVisible = true,
            status = PlaybackEngineStatus.Playing,
        )

        assertTrue(PlayerDetailOverlayVisibilityPolicy.shouldDisplay(snapshot))
    }

    @Test
    fun displaysDetailsWhenPlayerIsPausedEvenIfOsdIsHidden() {
        val snapshot = PlayerDetailOverlayVisibilitySnapshot(
            osdVisible = false,
            status = PlaybackEngineStatus.Paused,
        )

        assertTrue(PlayerDetailOverlayVisibilityPolicy.shouldDisplay(snapshot))
    }

    @Test
    fun displaysDetailsWhenPlaybackEndedEvenIfOsdIsHidden() {
        val snapshot = PlayerDetailOverlayVisibilitySnapshot(
            osdVisible = false,
            status = PlaybackEngineStatus.Ended,
        )

        assertTrue(PlayerDetailOverlayVisibilityPolicy.shouldDisplay(snapshot))
    }

    @Test
    fun hidesDetailsWhenPlayerIsPlayingAndOsdIsHidden() {
        val snapshot = PlayerDetailOverlayVisibilitySnapshot(
            osdVisible = false,
            status = PlaybackEngineStatus.Playing,
        )

        assertFalse(PlayerDetailOverlayVisibilityPolicy.shouldDisplay(snapshot))
    }

    @Test
    fun requestsDetailsWhenOsdIsVisibleAndSessionIsAvailable() {
        val snapshot = PlayerDetailOverlayLoadSnapshot(
            currentItemId = "item-1",
            overlayItemId = null,
            shouldDisplayOverlay = true,
            sessionAvailable = true,
            isLoading = false,
            hasDetail = false,
        )

        assertTrue(PlayerDetailOverlayLoadPolicy.shouldRequestLoad(snapshot))
    }

    @Test
    fun requestsDetailsWhenPlayerIsPausedEvenIfOsdIsHidden() {
        val snapshot = PlayerDetailOverlayLoadSnapshot(
            currentItemId = "item-1",
            overlayItemId = null,
            shouldDisplayOverlay = true,
            sessionAvailable = true,
            isLoading = false,
            hasDetail = false,
        )

        assertTrue(PlayerDetailOverlayLoadPolicy.shouldRequestLoad(snapshot))
    }

    @Test
    fun skipsDetailsWhenOverlayIsNotDisplayed() {
        val snapshot = PlayerDetailOverlayLoadSnapshot(
            currentItemId = "item-1",
            overlayItemId = null,
            shouldDisplayOverlay = false,
            sessionAvailable = true,
            isLoading = false,
            hasDetail = false,
        )

        assertFalse(PlayerDetailOverlayLoadPolicy.shouldRequestLoad(snapshot))
    }

    @Test
    fun skipsDetailsWithoutSessionContext() {
        val snapshot = PlayerDetailOverlayLoadSnapshot(
            currentItemId = "item-1",
            overlayItemId = null,
            shouldDisplayOverlay = true,
            sessionAvailable = false,
            isLoading = false,
            hasDetail = false,
        )

        assertFalse(PlayerDetailOverlayLoadPolicy.shouldRequestLoad(snapshot))
    }

    @Test
    fun skipsDetailsWhenAlreadyLoadingOrLoaded() {
        val loading = PlayerDetailOverlayLoadSnapshot(
            currentItemId = "item-1",
            overlayItemId = "item-1",
            shouldDisplayOverlay = true,
            sessionAvailable = true,
            isLoading = true,
            hasDetail = false,
            hasError = false,
        )
        val loaded = loading.copy(isLoading = false, hasDetail = true)

        assertFalse(PlayerDetailOverlayLoadPolicy.shouldRequestLoad(loading))
        assertFalse(PlayerDetailOverlayLoadPolicy.shouldRequestLoad(loaded))
    }

    @Test
    fun skipsDetailsAfterFailureUntilPlaybackItemChanges() {
        val failed = PlayerDetailOverlayLoadSnapshot(
            currentItemId = "item-1",
            overlayItemId = "item-1",
            shouldDisplayOverlay = true,
            sessionAvailable = true,
            isLoading = false,
            hasDetail = false,
            hasError = true,
        )

        assertFalse(PlayerDetailOverlayLoadPolicy.shouldRequestLoad(failed))
    }

    @Test
    fun requestsDetailsForNewPlaybackItemEvenWhenPreviousItemLoadedOrFailed() {
        val loadedPreviousItem = PlayerDetailOverlayLoadSnapshot(
            currentItemId = "item-2",
            overlayItemId = "item-1",
            shouldDisplayOverlay = true,
            sessionAvailable = true,
            isLoading = false,
            hasDetail = true,
            hasError = false,
        )
        val failedPreviousItem = loadedPreviousItem.copy(
            hasDetail = false,
            hasError = true,
        )

        assertTrue(PlayerDetailOverlayLoadPolicy.shouldRequestLoad(loadedPreviousItem))
        assertTrue(PlayerDetailOverlayLoadPolicy.shouldRequestLoad(failedPreviousItem))
    }
}
