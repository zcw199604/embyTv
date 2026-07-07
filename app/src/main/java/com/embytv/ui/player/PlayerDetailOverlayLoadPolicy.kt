package com.embytv.ui.player

data class PlayerDetailOverlayLoadSnapshot(
    val currentItemId: String,
    val overlayItemId: String?,
    val shouldDisplayOverlay: Boolean,
    val sessionAvailable: Boolean,
    val isLoading: Boolean,
    val hasDetail: Boolean,
    val hasError: Boolean = false,
)

data class PlayerDetailOverlayVisibilitySnapshot(
    val osdVisible: Boolean,
    val status: PlaybackEngineStatus,
)

object PlayerDetailOverlayVisibilityPolicy {
    fun shouldDisplay(snapshot: PlayerDetailOverlayVisibilitySnapshot): Boolean =
        snapshot.osdVisible ||
            snapshot.status == PlaybackEngineStatus.Paused ||
            snapshot.status == PlaybackEngineStatus.Ended
}

object PlayerDetailOverlayLoadPolicy {
    fun shouldRequestLoad(snapshot: PlayerDetailOverlayLoadSnapshot): Boolean {
        if (!snapshot.shouldDisplayOverlay || !snapshot.sessionAvailable) {
            return false
        }
        if (snapshot.overlayItemId != snapshot.currentItemId) {
            return true
        }
        return !snapshot.isLoading &&
            !snapshot.hasDetail &&
            !snapshot.hasError
    }
}
