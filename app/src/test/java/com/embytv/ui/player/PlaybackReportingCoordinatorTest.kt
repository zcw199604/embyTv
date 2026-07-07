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
    fun ignoresProgressPauseAndSeekUntilPlaybackStarted() {
        coordinator.onProgressTick(positionMs = 12_000L, isPaused = false)
        coordinator.onPauseChanged(positionMs = 12_000L, isPaused = true)
        coordinator.onSeek(positionMs = 20_000L, isPaused = false)

        assertEquals(emptyList<PlaybackReportEvent>(), events)
    }

    @Test
    fun ignoresStoppedUntilPlaybackStarted() {
        coordinator.onStopped(positionMs = 20_000L)

        assertEquals(emptyList<PlaybackReportEvent>(), events)
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
    fun fallsBackToDefaultThrottleWhenProgressIntervalIsNotPositive() {
        val customEvents = mutableListOf<PlaybackReportEvent>()
        val customCoordinator = PlaybackReportingCoordinator(
            progressIntervalMs = 0L,
            onEvent = { customEvents += it },
        )

        customCoordinator.onStarted(positionMs = 0L)
        customCoordinator.onProgressTick(positionMs = 250L, isPaused = false)
        customCoordinator.onProgressTick(positionMs = 10_000L, isPaused = false)

        assertEquals(
            listOf(
                PlaybackReportEvent.Started(0L),
                PlaybackReportEvent.Progress(positionMs = 10_000L, isPaused = false),
            ),
            customEvents,
        )
    }

    @Test
    fun progressTickReportsImmediatelyWhenPauseStateChanges() {
        coordinator.onStarted(positionMs = 0L)
        coordinator.onProgressTick(positionMs = 4_000L, isPaused = true)
        coordinator.onProgressTick(positionMs = 5_000L, isPaused = true)
        coordinator.onProgressTick(positionMs = 6_000L, isPaused = false)

        assertEquals(
            listOf(
                PlaybackReportEvent.Started(0L),
                PlaybackReportEvent.Progress(positionMs = 4_000L, isPaused = true),
                PlaybackReportEvent.Progress(positionMs = 6_000L, isPaused = false),
            ),
            events,
        )
    }

    @Test
    fun duplicateSeekToSamePositionAndPauseStateIsReportedOnlyOnce() {
        coordinator.onStarted(positionMs = 0L)
        coordinator.onSeek(positionMs = 30_000L, isPaused = false)
        coordinator.onSeek(positionMs = 30_000L, isPaused = false)
        coordinator.onSeek(positionMs = 40_000L, isPaused = false)

        assertEquals(
            listOf(
                PlaybackReportEvent.Started(0L),
                PlaybackReportEvent.Progress(positionMs = 30_000L, isPaused = false),
                PlaybackReportEvent.Progress(positionMs = 40_000L, isPaused = false),
            ),
            events,
        )
    }

    @Test
    fun reportsStoppedOnlyOnce() {
        coordinator.onStarted(positionMs = 0L)
        coordinator.onStopped(positionMs = 20_000L)
        coordinator.onStopped(positionMs = 21_000L)
        coordinator.onProgressTick(positionMs = 22_000L, isPaused = false)

        assertEquals(
            listOf(
                PlaybackReportEvent.Started(0L),
                PlaybackReportEvent.Stopped(20_000L),
            ),
            events,
        )
    }
}
