package com.embytv.ui.player

import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.ui.PlayerView
import androidx.tv.material3.Button
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import com.embytv.core.di.AppContainer
import com.embytv.domain.model.PlaybackSource
import com.kuaishou.akdanmaku.DanmakuConfig
import com.kuaishou.akdanmaku.ui.DanmakuView

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun PlayerScreen(
    container: AppContainer,
    playbackSource: PlaybackSource,
    onBack: () -> Unit,
) {
    val player = remember { container.playerFactory.createPlayer() }
    val danmakuPlayer = remember { container.danmakuBridge.createPlayer() }
    val focusRequester = remember { FocusRequester() }
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(playbackSource) {
        player.setMediaItem(MediaItem.fromUri(playbackSource.streamUrl))
        player.prepare()
        player.playWhenReady = true

        danmakuPlayer.seekTo(0)
        danmakuPlayer.updateData(container.danmakuBridge.toAkItems(playbackSource.danmaku))
        danmakuPlayer.start(
            DanmakuConfig(
                textSizeScale = 1.15f,
                screenPart = 0.82f,
                allowOverlap = false,
            ),
        )
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    DisposableEffect(lifecycleOwner, player, danmakuPlayer) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    player.play()
                    danmakuPlayer.start()
                }
                Lifecycle.Event.ON_PAUSE -> {
                    player.pause()
                    danmakuPlayer.pause()
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            player.release()
            danmakuPlayer.release()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyUp && event.key == Key.Back) {
                    onBack()
                    true
                } else {
                    false
                }
            },
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                PlayerView(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                    useController = true
                    setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                    this.player = player
                }
            },
            update = { it.player = player },
        )

        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                DanmakuView(context).also { danmakuView ->
                    danmakuPlayer.bindView(danmakuView)
                }
            },
        )

        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(28.dp),
        ) {
            Button(onClick = onBack) {
                Text("返回")
            }
        }
    }
}
