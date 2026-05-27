package com.embytv.ui.player

enum class PlayerQuickPanel {
    Audio,
    Subtitles,
    Danmaku,
}

data class PlayerOsdState(
    val visible: Boolean = true,
    val isPlaying: Boolean = true,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val danmakuEnabled: Boolean = true,
    val danmakuPaused: Boolean = false,
    val selectedQuickPanel: PlayerQuickPanel? = null,
) {
    val progressFraction: Float
        get() = if (durationMs <= 0L) {
            0f
        } else {
            (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
        }
}

sealed interface PlayerOsdAction {
    data object UserInteraction : PlayerOsdAction
    data object Hide : PlayerOsdAction
    data object BackPressed : PlayerOsdAction
    data object TogglePlayPause : PlayerOsdAction
    data object ToggleDanmaku : PlayerOsdAction
    data class SelectQuickPanel(val panel: PlayerQuickPanel?) : PlayerOsdAction
    data class ProgressChanged(val positionMs: Long, val durationMs: Long) : PlayerOsdAction
}

data class PlayerOsdResult(
    val state: PlayerOsdState,
    val exitPlayer: Boolean = false,
)

object PlayerOsdReducer {
    fun reduce(state: PlayerOsdState, action: PlayerOsdAction): PlayerOsdResult =
        when (action) {
            PlayerOsdAction.UserInteraction -> PlayerOsdResult(state.copy(visible = true))
            PlayerOsdAction.Hide -> PlayerOsdResult(state.copy(visible = false, selectedQuickPanel = null))
            PlayerOsdAction.BackPressed -> {
                if (state.visible) {
                    PlayerOsdResult(state.copy(visible = false, selectedQuickPanel = null))
                } else {
                    PlayerOsdResult(state, exitPlayer = true)
                }
            }
            PlayerOsdAction.TogglePlayPause -> {
                val isPlaying = !state.isPlaying
                PlayerOsdResult(
                    state.copy(
                        visible = true,
                        isPlaying = isPlaying,
                        danmakuPaused = state.danmakuEnabled && !isPlaying,
                    ),
                )
            }
            PlayerOsdAction.ToggleDanmaku -> {
                val enabled = !state.danmakuEnabled
                PlayerOsdResult(
                    state.copy(
                        visible = true,
                        danmakuEnabled = enabled,
                        danmakuPaused = !enabled || !state.isPlaying,
                        selectedQuickPanel = PlayerQuickPanel.Danmaku,
                    ),
                )
            }
            is PlayerOsdAction.SelectQuickPanel -> {
                PlayerOsdResult(state.copy(visible = true, selectedQuickPanel = action.panel))
            }
            is PlayerOsdAction.ProgressChanged -> {
                PlayerOsdResult(
                    state.copy(
                        positionMs = action.positionMs.coerceAtLeast(0L),
                        durationMs = action.durationMs.coerceAtLeast(0L),
                    ),
                )
            }
        }
}
