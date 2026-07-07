package com.embytv.ui.player

data class PlayerQuickPanelFocusSnapshot(
    val visible: Boolean,
    val selectedQuickPanel: PlayerQuickPanel?,
    val focusableOptionCount: Int,
)

object PlayerQuickPanelFocusPolicy {
    fun shouldRequestPanelFocus(
        previous: PlayerQuickPanelFocusSnapshot?,
        current: PlayerQuickPanelFocusSnapshot,
    ): Boolean {
        if (!current.visible || current.selectedQuickPanel == null || current.focusableOptionCount <= 0) {
            return false
        }
        if (previous?.selectedQuickPanel != current.selectedQuickPanel) {
            return true
        }
        return previous.focusableOptionCount <= 0 && current.focusableOptionCount > 0
    }

    fun focusableOptionCount(state: PlayerOsdState): Int =
        when (state.selectedQuickPanel) {
            PlayerQuickPanel.Audio -> state.audioTracks.size
            PlayerQuickPanel.Subtitles -> 1 + state.subtitleTracks.size
            PlayerQuickPanel.Speed -> SupportedPlaybackSpeeds.size
            PlayerQuickPanel.Danmaku -> DanmakuQuickPanelLayoutPolicy.TvDefault.rows.sumOf { it.size }
            null -> 0
        }
}
