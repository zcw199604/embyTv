package com.embytv.ui.player

enum class PlayerQuickPanel {
    Audio,
    Subtitles,
    Danmaku,
    Speed,
}

val SupportedPlaybackSpeeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)

sealed interface PlaybackEngineStatus {
    data object Loading : PlaybackEngineStatus
    data object Playing : PlaybackEngineStatus
    data object Paused : PlaybackEngineStatus
    data object Buffering : PlaybackEngineStatus
    data object Ended : PlaybackEngineStatus
    data class Error(val message: String) : PlaybackEngineStatus
}

enum class DanmakuDisplayArea(val screenPart: Float) {
    Top(0.35f),
    Full(0.82f),
}

data class DanmakuOverlaySettings(
    val opacity: Float = 1f,
    val textSizeScale: Float = 1.15f,
    val displayArea: DanmakuDisplayArea = DanmakuDisplayArea.Full,
) {
    fun normalized(): DanmakuOverlaySettings =
        copy(
            opacity = opacity.finiteOrDefault(1f).coerceIn(0.2f, 1f),
            textSizeScale = textSizeScale.finiteOrDefault(1.15f).coerceIn(0.8f, 1.6f),
        )
}

private fun Float.finiteOrDefault(defaultValue: Float): Float =
    if (isFinite()) this else defaultValue

data class SeekPreviewState(
    val targetPositionMs: Long,
    val deltaMs: Long,
    val speedLabel: String,
    val thumbnailUrl: String? = null,
    val originPositionMs: Long = targetPositionMs - deltaMs,
)

data class PlayerDetailOverlayState(
    val itemId: String? = null,
    val isLoading: Boolean = false,
    val detail: com.embytv.domain.model.EmbyMediaDetail? = null,
    val playbackDetails: com.embytv.domain.model.PlaybackDetails? = null,
    val errorMessage: String? = null,
)

data class PlayerOsdState(
    val visible: Boolean = true,
    val interactionRevision: Int = 0,
    val status: PlaybackEngineStatus = PlaybackEngineStatus.Loading,
    val isPlaying: Boolean = true,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val bufferedFraction: Float = 0f,
    val danmakuEnabled: Boolean = true,
    val danmakuPaused: Boolean = false,
    val danmakuSettings: DanmakuOverlaySettings = DanmakuOverlaySettings(),
    val selectedQuickPanel: PlayerQuickPanel? = null,
    val feedbackMessage: String? = null,
    val audioTracks: List<com.embytv.domain.model.PlayerTrackOption> = emptyList(),
    val subtitleTracks: List<com.embytv.domain.model.PlayerTrackOption> = emptyList(),
    val subtitleDisabled: Boolean = false,
    val playbackSpeed: Float = 1.0f,
    val seekPreview: SeekPreviewState? = null,
    val detailOverlay: PlayerDetailOverlayState = PlayerDetailOverlayState(),
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
    data class SetDanmakuEnabled(val enabled: Boolean) : PlayerOsdAction
    data object ClearFeedback : PlayerOsdAction
    data class PlaybackStatusChanged(val status: PlaybackEngineStatus) : PlayerOsdAction
    data class UnsupportedAction(val message: String) : PlayerOsdAction
    data class SelectQuickPanel(val panel: PlayerQuickPanel?) : PlayerOsdAction
    data class TracksChanged(
        val audioTracks: List<com.embytv.domain.model.PlayerTrackOption>,
        val subtitleTracks: List<com.embytv.domain.model.PlayerTrackOption>,
    ) : PlayerOsdAction
    data class SelectTrack(val option: com.embytv.domain.model.PlayerTrackOption) : PlayerOsdAction
    data class DisableSubtitles(val feedbackMessage: String? = null) : PlayerOsdAction
    data class SelectPlaybackSpeed(val speed: Float, val feedbackMessage: String? = null) : PlayerOsdAction
    data class SeekPreviewRequested(
        val deltaMs: Long,
        val thumbnailUrl: String? = null,
    ) : PlayerOsdAction
    data object SeekPreviewCommitted : PlayerOsdAction
    data object SeekPreviewCancelled : PlayerOsdAction
    data class UpdateDanmakuSettings(
        val opacity: Float? = null,
        val textSizeScale: Float? = null,
        val displayArea: DanmakuDisplayArea? = null,
    ) : PlayerOsdAction
    data class DetailOverlayLoading(val itemId: String) : PlayerOsdAction
    data class DetailOverlayLoaded(
        val itemId: String,
        val detail: com.embytv.domain.model.EmbyMediaDetail,
        val playbackDetails: com.embytv.domain.model.PlaybackDetails,
    ) : PlayerOsdAction
    data class DetailOverlayFailed(val itemId: String, val message: String) : PlayerOsdAction
    data class ProgressChanged(
        val positionMs: Long,
        val durationMs: Long,
        val bufferedFraction: Float = 0f,
    ) : PlayerOsdAction
}

data class PlayerOsdResult(
    val state: PlayerOsdState,
    val exitPlayer: Boolean = false,
)

object PlayerOsdReducer {
    fun reduce(state: PlayerOsdState, action: PlayerOsdAction): PlayerOsdResult =
        when (action) {
            PlayerOsdAction.UserInteraction -> PlayerOsdResult(state.withInteraction())
            PlayerOsdAction.Hide -> PlayerOsdResult(
                state.copy(
                    visible = false,
                    selectedQuickPanel = null,
                    seekPreview = null,
                    feedbackMessage = null,
                ),
            )
            PlayerOsdAction.ClearFeedback -> PlayerOsdResult(state.copy(feedbackMessage = null))
            PlayerOsdAction.BackPressed -> {
                if (state.visible) {
                    if (state.selectedQuickPanel != null) {
                        PlayerOsdResult(
                            state.withInteraction().copy(
                                selectedQuickPanel = null,
                                seekPreview = null,
                                feedbackMessage = null,
                            ),
                        )
                    } else {
                        PlayerOsdResult(
                            state.copy(
                                visible = false,
                                selectedQuickPanel = null,
                                seekPreview = null,
                                feedbackMessage = null,
                            ),
                        )
                    }
                } else {
                    PlayerOsdResult(state, exitPlayer = true)
                }
            }
            PlayerOsdAction.TogglePlayPause -> {
                val isPlaying = !state.isPlaying
                PlayerOsdResult(
                    state.withInteraction().copy(
                        isPlaying = isPlaying,
                        status = if (isPlaying) PlaybackEngineStatus.Playing else PlaybackEngineStatus.Paused,
                        danmakuPaused = state.danmakuEnabled && !isPlaying,
                    ),
                )
            }
            PlayerOsdAction.ToggleDanmaku -> {
                val enabled = !state.danmakuEnabled
                PlayerOsdResult(
                    state.withInteraction().copy(
                        danmakuEnabled = enabled,
                        danmakuPaused = !enabled || !state.isPlaying,
                        selectedQuickPanel = PlayerQuickPanel.Danmaku,
                        seekPreview = null,
                        feedbackMessage = null,
                    ),
                )
            }
            is PlayerOsdAction.SetDanmakuEnabled -> {
                PlayerOsdResult(
                    state.withInteraction().copy(
                        danmakuEnabled = action.enabled,
                        danmakuPaused = !action.enabled || !state.isPlaying,
                        selectedQuickPanel = PlayerQuickPanel.Danmaku,
                        seekPreview = null,
                        feedbackMessage = null,
                    ),
                )
            }
            is PlayerOsdAction.PlaybackStatusChanged -> {
                val isPlaying = action.status.playbackIntent(state.isPlaying)
                val isError = action.status is PlaybackEngineStatus.Error
                val isTerminal = isError || action.status == PlaybackEngineStatus.Ended
                val shouldShow = action.status == PlaybackEngineStatus.Buffering ||
                    action.status == PlaybackEngineStatus.Ended ||
                    isError
                PlayerOsdResult(
                    state.copy(
                        visible = if (shouldShow) true else state.visible,
                        interactionRevision = if (shouldShow) state.interactionRevision + 1 else state.interactionRevision,
                        status = action.status,
                        isPlaying = isPlaying,
                        danmakuPaused = state.danmakuEnabled && action.status.shouldPauseDanmaku(isPlaying),
                        selectedQuickPanel = if (isTerminal) null else state.selectedQuickPanel,
                        seekPreview = if (isTerminal) null else state.seekPreview,
                        feedbackMessage = (action.status as? PlaybackEngineStatus.Error)?.message
                            ?: if (isTerminal) null else state.feedbackMessage,
                    ),
                )
            }
            is PlayerOsdAction.UnsupportedAction -> {
                PlayerOsdResult(state.withInteraction().copy(feedbackMessage = action.message))
            }
            is PlayerOsdAction.SelectQuickPanel -> {
                val selectedPanel = if (state.selectedQuickPanel == action.panel) {
                    null
                } else {
                    action.panel
                }
                PlayerOsdResult(
                    state.withInteraction().copy(
                        selectedQuickPanel = selectedPanel,
                        seekPreview = null,
                        feedbackMessage = null,
                    ),
                )
            }
            is PlayerOsdAction.TracksChanged -> {
                val subtitleTracks = if (state.subtitleDisabled) {
                    action.subtitleTracks.map { it.copy(selected = false) }
                } else {
                    action.subtitleTracks
                }
                PlayerOsdResult(
                    state.copy(
                        audioTracks = action.audioTracks,
                        subtitleTracks = subtitleTracks,
                        subtitleDisabled = state.subtitleDisabled,
                    ),
                )
            }
            is PlayerOsdAction.SelectTrack -> {
                val selectedPanel = when (action.option.type) {
                    com.embytv.domain.model.PlayerTrackType.Audio -> PlayerQuickPanel.Audio
                    com.embytv.domain.model.PlayerTrackType.Subtitle -> PlayerQuickPanel.Subtitles
                }
                PlayerOsdResult(
                    state.withInteraction().copy(
                        selectedQuickPanel = selectedPanel,
                        audioTracks = state.audioTracks.selectOption(
                            option = action.option,
                            shouldSelect = action.option.type == com.embytv.domain.model.PlayerTrackType.Audio,
                        ),
                        subtitleTracks = state.subtitleTracks.selectOption(
                            option = action.option,
                            shouldSelect = action.option.type == com.embytv.domain.model.PlayerTrackType.Subtitle,
                        ),
                        subtitleDisabled = if (action.option.type == com.embytv.domain.model.PlayerTrackType.Subtitle) {
                            false
                        } else {
                            state.subtitleDisabled
                        },
                        seekPreview = null,
                        feedbackMessage = null,
                    ),
                )
            }
            is PlayerOsdAction.DisableSubtitles -> {
                PlayerOsdResult(
                    state.withInteraction().copy(
                        selectedQuickPanel = PlayerQuickPanel.Subtitles,
                        subtitleDisabled = true,
                        subtitleTracks = state.subtitleTracks.map { it.copy(selected = false) },
                        seekPreview = null,
                        feedbackMessage = action.feedbackMessage,
                    ),
                )
            }
            is PlayerOsdAction.SelectPlaybackSpeed -> {
                val speed = action.speed.nearestSupportedPlaybackSpeed()
                if (action.feedbackMessage == null) {
                    PlayerOsdResult(state.copy(playbackSpeed = speed))
                } else {
                    PlayerOsdResult(
                        state.withInteraction().copy(
                            selectedQuickPanel = PlayerQuickPanel.Speed,
                            playbackSpeed = speed,
                            seekPreview = null,
                            feedbackMessage = action.feedbackMessage,
                        ),
                    )
                }
            }
            is PlayerOsdAction.SeekPreviewRequested -> {
                val target = state.seekTarget(action.deltaMs)
                val origin = state.seekPreview?.originPositionMs ?: state.positionMs
                val seekDelta = target.saturatingDeltaFrom(origin)
                PlayerOsdResult(
                    state.withInteraction().copy(
                        seekPreview = SeekPreviewState(
                            targetPositionMs = target,
                            deltaMs = action.deltaMs,
                            speedLabel = seekDelta.toSeekLabel(),
                            thumbnailUrl = action.thumbnailUrl ?: state.seekPreview?.thumbnailUrl,
                            originPositionMs = origin,
                        ),
                        feedbackMessage = seekDelta.toSeekLabel(),
                    ),
                )
            }
            PlayerOsdAction.SeekPreviewCommitted -> {
                val target = state.seekPreview?.targetPositionMs ?: state.positionMs
                PlayerOsdResult(state.withInteraction().copy(positionMs = target))
            }
            PlayerOsdAction.SeekPreviewCancelled -> {
                PlayerOsdResult(state.copy(seekPreview = null))
            }
            is PlayerOsdAction.UpdateDanmakuSettings -> {
                val settings = state.danmakuSettings.copy(
                    opacity = action.opacity ?: state.danmakuSettings.opacity,
                    textSizeScale = action.textSizeScale ?: state.danmakuSettings.textSizeScale,
                    displayArea = action.displayArea ?: state.danmakuSettings.displayArea,
                ).normalized()
                PlayerOsdResult(
                    state.withInteraction().copy(
                        danmakuSettings = settings,
                        selectedQuickPanel = PlayerQuickPanel.Danmaku,
                        seekPreview = null,
                        feedbackMessage = null,
                    ),
                )
            }
            is PlayerOsdAction.DetailOverlayLoading -> {
                PlayerOsdResult(
                    state.copy(detailOverlay = PlayerDetailOverlayState(itemId = action.itemId, isLoading = true)),
                )
            }
            is PlayerOsdAction.DetailOverlayLoaded -> {
                if (!state.detailOverlay.acceptsResultFor(action.itemId)) {
                    return PlayerOsdResult(state)
                }
                PlayerOsdResult(
                    state.copy(
                        detailOverlay = PlayerDetailOverlayState(
                            itemId = action.itemId,
                            detail = action.detail,
                            playbackDetails = action.playbackDetails,
                        ),
                    ),
                )
            }
            is PlayerOsdAction.DetailOverlayFailed -> {
                if (!state.detailOverlay.acceptsResultFor(action.itemId)) {
                    return PlayerOsdResult(state)
                }
                PlayerOsdResult(
                    state.copy(
                        detailOverlay = PlayerDetailOverlayState(
                            itemId = action.itemId,
                            errorMessage = action.message,
                        ),
                        feedbackMessage = action.message,
                    ),
                )
            }
            is PlayerOsdAction.ProgressChanged -> {
                val durationMs = action.durationMs.coerceAtLeast(0L)
                val positionMs = action.positionMs.coerceAtLeast(0L).let { position ->
                    if (durationMs > 0L) position.coerceAtMost(durationMs) else position
                }
                val progressFraction = if (durationMs <= 0L) {
                    0f
                } else {
                    (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
                }
                PlayerOsdResult(
                    state.copy(
                        positionMs = positionMs,
                        durationMs = durationMs,
                        bufferedFraction = action.bufferedFraction.toStableBufferedFraction(progressFraction),
                    ),
                )
            }
        }
}

private fun PlayerOsdState.withInteraction(): PlayerOsdState =
    copy(visible = true, interactionRevision = interactionRevision + 1)

private fun PlayerDetailOverlayState.acceptsResultFor(itemId: String): Boolean =
    this.itemId == null || this.itemId == itemId

private fun PlaybackEngineStatus.playbackIntent(current: Boolean): Boolean =
    when (this) {
        PlaybackEngineStatus.Playing -> true
        PlaybackEngineStatus.Paused,
        PlaybackEngineStatus.Ended,
        is PlaybackEngineStatus.Error,
        -> false
        PlaybackEngineStatus.Loading,
        PlaybackEngineStatus.Buffering,
        -> current
    }

private fun PlaybackEngineStatus.shouldPauseDanmaku(isPlaying: Boolean): Boolean =
    when (this) {
        PlaybackEngineStatus.Buffering,
        PlaybackEngineStatus.Loading,
        -> true
        else -> !isPlaying
    }

private fun Float.toStableBufferedFraction(progressFraction: Float): Float =
    if (isFinite()) coerceIn(0f, 1f).coerceAtLeast(progressFraction) else progressFraction

private fun List<com.embytv.domain.model.PlayerTrackOption>.selectOption(
    option: com.embytv.domain.model.PlayerTrackOption,
    shouldSelect: Boolean,
): List<com.embytv.domain.model.PlayerTrackOption> {
    if (!shouldSelect) return this
    return map { track -> track.copy(selected = track.id == option.id) }
}

private fun PlayerOsdState.seekTarget(deltaMs: Long): Long {
    val basePosition = seekPreview?.targetPositionMs ?: positionMs
    val requested = basePosition.saturatingAdd(deltaMs)
    val minimum = 0L
    val maximum = durationMs.takeIf { it > 0L } ?: requested.coerceAtLeast(minimum)
    return requested.coerceIn(minimum, maximum)
}

private fun Long.saturatingAdd(delta: Long): Long =
    when {
        delta > 0L && this > Long.MAX_VALUE - delta -> Long.MAX_VALUE
        delta < 0L && this < Long.MIN_VALUE - delta -> Long.MIN_VALUE
        else -> this + delta
    }

private fun Long.saturatingDeltaFrom(origin: Long): Long =
    when {
        origin < 0L && this > Long.MAX_VALUE + origin -> Long.MAX_VALUE
        origin > 0L && this < Long.MIN_VALUE + origin -> Long.MIN_VALUE
        else -> this - origin
    }

private fun Long.toSeekLabel(): String {
    val sign = if (this >= 0L) "+" else "-"
    val seconds = kotlin.math.abs(this / 1_000L)
    return "${sign}${seconds}s"
}

fun Float.nearestSupportedPlaybackSpeed(): Float =
    if (isFinite()) {
        SupportedPlaybackSpeeds.minBy { kotlin.math.abs(it - this) }
    } else {
        1.0f
    }

fun Float.toSpeedLabel(): String =
    if (this == 1.0f || this % 1.0f == 0f) {
        "${this.toInt()}x"
    } else {
        "${this}x"
    }
