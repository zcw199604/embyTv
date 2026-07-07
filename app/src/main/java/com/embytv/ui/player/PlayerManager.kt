package com.embytv.ui.player

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PlayerManager(
    initialState: PlayerOsdState = PlayerOsdState(),
    private val seekThumbnailProvider: (Long) -> String? = { null },
) {
    private val _state = MutableStateFlow(initialState)
    private var exitRequested = false
    val state: StateFlow<PlayerOsdState> = _state.asStateFlow()

    fun dispatch(action: PlayerOsdAction) {
        val result = PlayerOsdReducer.reduce(_state.value, action)
        _state.value = result.state
        if (result.exitPlayer) {
            exitRequested = true
        }
    }

    fun requestSeekPreview(deltaMs: Long) {
        val target = _state.value.previewSeekTarget(deltaMs)
        dispatch(
            PlayerOsdAction.SeekPreviewRequested(
                deltaMs = deltaMs,
                thumbnailUrl = seekThumbnailProvider(target)?.trim()?.takeIf { it.isNotBlank() },
            ),
        )
    }

    fun consumeExitRequested(): Boolean {
        val requested = exitRequested
        exitRequested = false
        return requested
    }
}

private fun PlayerOsdState.previewSeekTarget(deltaMs: Long): Long {
    val basePosition = seekPreview?.targetPositionMs ?: positionMs
    val requested = basePosition.saturatingAdd(deltaMs)
    val maximum = durationMs.takeIf { it > 0L } ?: requested.coerceAtLeast(0L)
    return requested.coerceIn(0L, maximum)
}

private fun Long.saturatingAdd(delta: Long): Long =
    when {
        delta > 0L && this > Long.MAX_VALUE - delta -> Long.MAX_VALUE
        delta < 0L && this < Long.MIN_VALUE - delta -> Long.MIN_VALUE
        else -> this + delta
    }
