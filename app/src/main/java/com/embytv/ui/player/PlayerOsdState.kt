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
    val feedbackMessage: String? = null,
    val audioTracks: List<com.embytv.domain.model.PlayerTrackOption> = emptyList(),
    val subtitleTracks: List<com.embytv.domain.model.PlayerTrackOption> = emptyList(),
    val subtitleDisabled: Boolean = false,
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
    data object ClearFeedback : PlayerOsdAction
    data class UnsupportedAction(val message: String) : PlayerOsdAction
    data class SelectQuickPanel(val panel: PlayerQuickPanel?) : PlayerOsdAction
    data class TracksChanged(
        val audioTracks: List<com.embytv.domain.model.PlayerTrackOption>,
        val subtitleTracks: List<com.embytv.domain.model.PlayerTrackOption>,
    ) : PlayerOsdAction
    data object DisableSubtitles : PlayerOsdAction
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
            PlayerOsdAction.ClearFeedback -> PlayerOsdResult(state.copy(feedbackMessage = null))
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
            is PlayerOsdAction.UnsupportedAction -> {
                PlayerOsdResult(state.copy(visible = true, feedbackMessage = action.message))
            }
            is PlayerOsdAction.SelectQuickPanel -> {
                PlayerOsdResult(state.copy(visible = true, selectedQuickPanel = action.panel, feedbackMessage = null))
            }
            is PlayerOsdAction.TracksChanged -> {
                PlayerOsdResult(
                    state.copy(
                        audioTracks = action.audioTracks,
                        subtitleTracks = action.subtitleTracks,
                        subtitleDisabled = state.subtitleDisabled && action.subtitleTracks.none { it.selected },
                    ),
                )
            }
            PlayerOsdAction.DisableSubtitles -> {
                PlayerOsdResult(
                    state.copy(
                        visible = true,
                        selectedQuickPanel = PlayerQuickPanel.Subtitles,
                        subtitleDisabled = true,
                        feedbackMessage = "字幕已关闭",
                    ),
                )
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
