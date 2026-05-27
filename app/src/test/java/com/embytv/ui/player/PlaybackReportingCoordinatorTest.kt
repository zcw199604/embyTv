package com.embytv.ui.player

import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackReportingCoordinatorTest {
    private val events = mutableListOf<PlaybackReportEvent>()
    private val coordinator = PlaybackReportingCoordinator(
        progressIntervalMs = 10_000L,
        onEvent = { events += it },
    )

    @Test
    fun reportsStartOnlyOnce() {
        coordinator.onStarted(positionMs = 0L)
        coordinator.onStarted(positionMs = 1_000L)

        assertEquals(listOf(PlaybackReportEvent.Started(0L)), events)
    }

    @Test
    fun throttlesProgressTicksButAllowsForcedEvents() {
        coordinator.onStarted(positionMs = 0L)
        coordinator.onProgressTick(positionMs = 4_000L, isPaused = false)
        coordinator.onProgressTick(positionMs = 10_000L, isPaused = false)
        coordinator.onPauseChanged(positionMs = 11_000L, isPaused = true)
        coordinator.onSeek(positionMs = 16_000L, isPaused = true)

        assertEquals(
            listOf(
                PlaybackReportEvent.Started(0L),
                PlaybackReportEvent.Progress(positionMs = 10_000L, isPaused = false),
                PlaybackReportEvent.Progress(positionMs = 11_000L, isPaused = true),
                PlaybackReportEvent.Progress(positionMs = 16_000L, isPaused = true),
            ),
            events,
        )
    }

    @Test
    fun reportsStoppedOnlyOnce() {
        coordinator.onStopped(positionMs = 20_000L)
        coordinator.onStopped(positionMs = 21_000L)
        coordinator.onProgressTick(positionMs = 22_000L, isPaused = false)

        assertEquals(listOf(PlaybackReportEvent.Stopped(20_000L)), events)
    }
}
