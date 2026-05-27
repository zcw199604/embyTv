package com.embytv.ui.player

sealed interface PlaybackReportEvent {
    data class Started(val positionMs: Long) : PlaybackReportEvent
    data class Progress(val positionMs: Long, val isPaused: Boolean) : PlaybackReportEvent
    data class Stopped(val positionMs: Long) : PlaybackReportEvent
}

class PlaybackReportingCoordinator(
    private val progressIntervalMs: Long = DEFAULT_PROGRESS_INTERVAL_MS,
    private val onEvent: (PlaybackReportEvent) -> Unit,
) {
    private var startedReported = false
    private var stoppedReported = false
    private var lastProgressPositionMs: Long? = null
    private var lastPausedState: Boolean? = null

    fun onStarted(positionMs: Long) {
        if (startedReported || stoppedReported) return
        startedReported = true
        val normalizedPosition = positionMs.coerceAtLeast(0L)
        lastProgressPositionMs = normalizedPosition
        lastPausedState = false
        onEvent(PlaybackReportEvent.Started(normalizedPosition))
    }

    fun onProgressTick(positionMs: Long, isPaused: Boolean) {
        if (stoppedReported) return
        val normalizedPosition = positionMs.coerceAtLeast(0L)
        val lastPosition = lastProgressPositionMs
        if (lastPosition == null || normalizedPosition - lastPosition >= progressIntervalMs) {
            reportProgress(normalizedPosition, isPaused)
        }
    }

    fun onPauseChanged(positionMs: Long, isPaused: Boolean) {
        if (stoppedReported) return
        if (lastPausedState == isPaused) return
        reportProgress(positionMs.coerceAtLeast(0L), isPaused)
    }

    fun onSeek(positionMs: Long, isPaused: Boolean) {
        if (stoppedReported) return
        reportProgress(positionMs.coerceAtLeast(0L), isPaused)
    }

    fun onStopped(positionMs: Long) {
        if (stoppedReported) return
        stoppedReported = true
        onEvent(PlaybackReportEvent.Stopped(positionMs.coerceAtLeast(0L)))
    }

    private fun reportProgress(positionMs: Long, isPaused: Boolean) {
        lastProgressPositionMs = positionMs
        lastPausedState = isPaused
        onEvent(PlaybackReportEvent.Progress(positionMs, isPaused))
    }

    private companion object {
        const val DEFAULT_PROGRESS_INTERVAL_MS = 10_000L
    }
}
