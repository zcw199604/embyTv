package com.embytv.ui.player

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class PlayerScreenDetailOverlayContractTest {
    @Test
    fun detailOverlayLoadFlowIsScopedToCurrentPlaybackItem() {
        val source = File("src/main/java/com/embytv/ui/player/PlayerScreen.kt").readText()

        assertTrue(source.contains("currentItemId = playbackSource.itemId"))
        assertTrue(source.contains("overlayItemId = osdState.detailOverlay.itemId"))
        assertTrue(source.contains("PlayerOsdAction.DetailOverlayLoading(playbackSource.itemId)"))
        assertTrue(source.contains("PlayerOsdAction.DetailOverlayLoaded(playbackSource.itemId, it.mediaDetail, it.playbackDetails)"))
        assertTrue(source.contains("PlayerOsdAction.DetailOverlayFailed(playbackSource.itemId, it.message ?: detailsFailedMessage)"))
    }
}
