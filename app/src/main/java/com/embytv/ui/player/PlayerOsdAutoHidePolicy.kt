package com.embytv.ui.player

data class PlayerOsdAutoHideSnapshot(
    val visible: Boolean,
    val status: PlaybackEngineStatus,
    val quickPanelOpen: Boolean = false,
)

object PlayerOsdAutoHidePolicy {
    const val AUTO_HIDE_DELAY_MS: Long = 5_000L

    fun shouldScheduleAutoHide(snapshot: PlayerOsdAutoHideSnapshot): Boolean {
        if (!snapshot.visible) return false
        if (snapshot.quickPanelOpen) return false
        return when (snapshot.status) {
            PlaybackEngineStatus.Playing,
            PlaybackEngineStatus.Paused,
            -> true
            PlaybackEngineStatus.Loading,
            PlaybackEngineStatus.Buffering,
            PlaybackEngineStatus.Ended,
            is PlaybackEngineStatus.Error,
            -> false
        }
    }
}

internal fun PlayerOsdState.toAutoHideSnapshot(): PlayerOsdAutoHideSnapshot =
    PlayerOsdAutoHideSnapshot(
        visible = visible,
        status = status,
        quickPanelOpen = selectedQuickPanel != null,
    )
