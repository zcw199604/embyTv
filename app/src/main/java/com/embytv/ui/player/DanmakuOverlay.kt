package com.embytv.ui.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.viewinterop.AndroidView
import com.kuaishou.akdanmaku.ui.DanmakuPlayer
import com.kuaishou.akdanmaku.ui.DanmakuView

@Composable
fun DanmakuOverlay(
    danmakuPlayer: DanmakuPlayer,
    settings: DanmakuOverlaySettings,
    enabled: Boolean,
    paused: Boolean,
    modifier: Modifier = Modifier,
) {
    val normalizedSettings = settings.normalized()
    val playbackConfigKey = settings.playbackConfigKey()

    LaunchedEffect(playbackConfigKey, enabled, paused) {
        when (val command = DanmakuPlaybackPolicy.commandForPlayback(settings, enabled, paused)) {
            is DanmakuPlaybackCommand.Start -> {
                danmakuPlayer.updateConfig(command.config)
                danmakuPlayer.start(command.config)
            }
            DanmakuPlaybackCommand.Pause -> danmakuPlayer.pause()
        }
    }

    AndroidView(
        modifier = modifier.alpha(if (enabled) normalizedSettings.opacity else 0f),
        factory = { context ->
            DanmakuView(context).also { danmakuView ->
                danmakuPlayer.bindView(danmakuView)
            }
        },
    )
}
