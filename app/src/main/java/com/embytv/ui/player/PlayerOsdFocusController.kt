package com.embytv.ui.player

data class PlayerOsdFocusSnapshot(
    val playbackItemId: String,
    val visible: Boolean,
    val interactionRevision: Int,
    val selectedQuickPanel: PlayerQuickPanel? = null,
)

object PlayerOsdFocusController {
    fun shouldRequestPrimaryFocus(
        previous: PlayerOsdFocusSnapshot?,
        current: PlayerOsdFocusSnapshot,
    ): Boolean {
        if (!current.visible) return false
        if (previous == null) return true
        if (!previous.visible) return true
        if (previous.playbackItemId != current.playbackItemId) return true
        return previous.selectedQuickPanel != null && current.selectedQuickPanel == null
    }
}
