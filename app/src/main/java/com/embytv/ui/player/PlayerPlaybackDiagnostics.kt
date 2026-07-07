package com.embytv.ui.player

import android.util.Log
import com.embytv.BuildConfig
import com.embytv.domain.model.PlaybackSource

object PlayerPlaybackDiagnostics {
    private const val TAG = "EmbyTvPlaybackReport"

    fun reportQueued(source: PlaybackSource, event: PlaybackReportEvent) {
        if (!BuildConfig.DEBUG) return
        Log.d(TAG, "queued ${event.label()} ${source.safeIdentity()} ${event.details()}")
    }

    fun reportSucceeded(source: PlaybackSource, event: PlaybackReportEvent) {
        if (!BuildConfig.DEBUG) return
        Log.d(TAG, "succeeded ${event.label()} ${source.safeIdentity()} ${event.details()}")
    }

    fun reportFailed(source: PlaybackSource, event: PlaybackReportEvent, throwable: Throwable) {
        if (!BuildConfig.DEBUG) return
        Log.w(TAG, "failed ${event.label()} ${source.safeIdentity()} ${event.details()}", throwable)
    }

    private fun PlaybackReportEvent.label(): String =
        when (this) {
            is PlaybackReportEvent.Started -> "Started"
            is PlaybackReportEvent.Progress -> "Progress"
            is PlaybackReportEvent.Stopped -> "Stopped"
        }

    private fun PlaybackReportEvent.details(): String =
        when (this) {
            is PlaybackReportEvent.Started -> "positionMs=$positionMs"
            is PlaybackReportEvent.Progress -> "positionMs=$positionMs isPaused=$isPaused"
            is PlaybackReportEvent.Stopped -> "positionMs=$positionMs"
        }

    private fun PlaybackSource.safeIdentity(): String =
        "itemId=$itemId " +
            "playlistItemIdPresent=${playlistItemId != null} " +
            "sessionPresent=${session != null} " +
            "deviceIdPresent=${deviceId != null}"
}
