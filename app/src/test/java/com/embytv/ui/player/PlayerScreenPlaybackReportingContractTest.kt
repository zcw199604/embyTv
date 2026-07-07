package com.embytv.ui.player

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class PlayerScreenPlaybackReportingContractTest {
    @Test
    fun playerScreenStopsCurrentReportingSessionWhenPlaybackSourceChanges() {
        val source = File("src/main/java/com/embytv/ui/player/PlayerScreen.kt").readText()

        assertTrue(source.contains("DisposableEffect(playbackSource, reportingCoordinator)"))
        assertTrue(source.contains("reportingCoordinator.onStopped(positionMs = player.currentPosition)"))
    }

    @Test
    fun playerScreenReportsPlaybackDiagnosticsAroundEmbyCheckIns() {
        val source = File("src/main/java/com/embytv/ui/player/PlayerScreen.kt").readText()

        assertTrue(source.contains("PlayerPlaybackDiagnostics.reportQueued(playbackSource, event)"))
        assertTrue(source.contains("PlayerPlaybackDiagnostics.reportSucceeded(playbackSource, event)"))
        assertTrue(source.contains("PlayerPlaybackDiagnostics.reportFailed(playbackSource, event, throwable)"))
    }

    @Test
    fun playbackDiagnosticsAreDebugOnlyAndDoNotLogSensitiveFields() {
        val source = File("src/main/java/com/embytv/ui/player/PlayerPlaybackDiagnostics.kt").readText()

        assertTrue(source.contains("BuildConfig.DEBUG"))
        assertTrue(source.contains("Log.d("))
        assertTrue(source.contains("Log.w("))
        assertTrue(source.contains("playlistItemIdPresent="))
        assertTrue(source.contains("sessionPresent="))
        assertTrue(source.contains("deviceIdPresent="))
        assertTrue(source.contains("positionMs="))
        assertTrue(source.contains("isPaused="))
        assertTrue(!source.contains("streamUrl"))
        assertTrue(!source.contains("accessToken"))
        assertTrue(!source.contains("api_key"))
        assertTrue(!source.contains("password"))
        assertTrue(!source.contains("serverUrl"))
    }

    @Test
    fun playerScreenUsesQueueNavigationPolicyForAutoPlayNext() {
        val source = File("src/main/java/com/embytv/ui/player/PlayerScreen.kt").readText()

        assertTrue(source.contains("currentQueueNavigationState.autoPlayNextTarget != null"))
        assertTrue(source.contains("currentQueueNavigationState.autoPlayNextTarget?.let(onPlayNext)"))
        assertTrue(!source.contains("queue?.autoPlayNext == true"))
    }
}
