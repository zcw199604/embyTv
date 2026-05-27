package com.embytv.ui.player

import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.ui.PlayerView
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.embytv.core.di.AppContainer
import com.embytv.domain.model.PlaybackSource
import com.embytv.ui.components.FocusableGlassSurface
import com.embytv.ui.components.GlassPanel
import com.embytv.ui.components.RemoteHint
import com.embytv.ui.theme.CinematicGlassColors
import com.embytv.ui.theme.CinematicGlassSpacing
import com.kuaishou.akdanmaku.DanmakuConfig
import com.kuaishou.akdanmaku.ui.DanmakuView
import kotlinx.coroutines.delay

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
    var osdState by remember { mutableStateOf(PlayerOsdState()) }

    fun dispatch(action: PlayerOsdAction) {
        val result = PlayerOsdReducer.reduce(osdState, action)
        osdState = result.state
        if (result.exitPlayer) {
            onBack()
        }
    }

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

    LaunchedEffect(osdState.isPlaying) {
        if (osdState.isPlaying) {
            player.play()
        } else {
            player.pause()
        }
    }

    LaunchedEffect(osdState.danmakuEnabled, osdState.danmakuPaused) {
        if (osdState.danmakuEnabled && !osdState.danmakuPaused) {
            danmakuPlayer.start()
        } else {
            danmakuPlayer.pause()
        }
    }

    LaunchedEffect(player) {
        while (true) {
            dispatch(
                PlayerOsdAction.ProgressChanged(
                    positionMs = player.currentPosition,
                    durationMs = player.duration.takeIf { it > 0L } ?: 0L,
                ),
            )
            delay(1_000)
        }
    }

    DisposableEffect(lifecycleOwner, player, danmakuPlayer) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    if (osdState.isPlaying) {
                        player.play()
                    }
                    if (osdState.danmakuEnabled && !osdState.danmakuPaused) {
                        danmakuPlayer.start()
                    }
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
                if (event.type != KeyEventType.KeyUp) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.Back -> {
                        dispatch(PlayerOsdAction.BackPressed)
                        true
                    }
                    else -> false
                }
            }
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyUp || osdState.visible) {
                    return@onKeyEvent false
                }
                when (event.key) {
                    Key.DirectionCenter, Key.Enter, Key.NumPadEnter,
                    Key.DirectionUp, Key.DirectionDown, Key.DirectionLeft, Key.DirectionRight -> {
                        dispatch(PlayerOsdAction.UserInteraction)
                        true
                    }
                    else -> false
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
                    useController = false
                    setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                    this.player = player
                }
            },
            update = { it.player = player },
        )

        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .alpha(if (osdState.danmakuEnabled) 1f else 0f),
            factory = { context ->
                DanmakuView(context).also { danmakuView ->
                    danmakuPlayer.bindView(danmakuView)
                }
            },
        )

        PlayerOsdOverlay(
            title = playbackSource.title,
            state = osdState,
            onBack = { dispatch(PlayerOsdAction.BackPressed) },
            onPlayPause = { dispatch(PlayerOsdAction.TogglePlayPause) },
            onReplay10 = {
                player.seekTo((player.currentPosition - 10_000L).coerceAtLeast(0L))
                dispatch(PlayerOsdAction.UserInteraction)
            },
            onForward10 = {
                val target = (player.currentPosition + 10_000L).coerceAtMost(player.duration.takeIf { it > 0L } ?: Long.MAX_VALUE)
                player.seekTo(target)
                dispatch(PlayerOsdAction.UserInteraction)
            },
            onToggleDanmaku = { dispatch(PlayerOsdAction.ToggleDanmaku) },
            onQuickPanel = { dispatch(PlayerOsdAction.SelectQuickPanel(it)) },
            onUnsupported = { dispatch(PlayerOsdAction.UnsupportedAction(it)) },
            onClearFeedback = { dispatch(PlayerOsdAction.ClearFeedback) },
        )
    }
}

@Composable
private fun PlayerOsdOverlay(
    title: String,
    state: PlayerOsdState,
    onBack: () -> Unit,
    onPlayPause: () -> Unit,
    onReplay10: () -> Unit,
    onForward10: () -> Unit,
    onToggleDanmaku: () -> Unit,
    onQuickPanel: (PlayerQuickPanel?) -> Unit,
    onUnsupported: (String) -> Unit,
    onClearFeedback: () -> Unit,
) {
    if (!state.visible) return
    val playFocusRequester = remember { FocusRequester() }

    LaunchedEffect(state.visible) {
        if (state.visible) {
            playFocusRequester.requestFocus()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Black.copy(alpha = 0.72f), Color.Transparent),
                    ),
                ),
        )
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(horizontal = CinematicGlassSpacing.SafeAreaX, vertical = 34.dp),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OsdIconButton(icon = Icons.AutoMirrored.Filled.ArrowBack, label = "返回", onClick = onBack, onUnsupported = onUnsupported)
            Column {
                Text(
                    text = title.ifBlank { "Emby Playback" },
                    color = CinematicGlassColors.Primary,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "Direct Playing · HEVC · 4K HDR",
                    color = CinematicGlassColors.OnSurfaceVariant,
                    fontSize = 14.sp,
                )
            }
        }
        GlassPanel(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(horizontal = CinematicGlassSpacing.SafeAreaX, vertical = 34.dp),
            cornerRadius = 999.dp,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Info, contentDescription = null, tint = CinematicGlassColors.Primary)
                Text("2160p · HDR10", color = CinematicGlassColors.OnSurfaceVariant, fontSize = 14.sp)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(420.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.58f),
                            Color.Black.copy(alpha = 0.92f),
                        ),
                    ),
                ),
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(
                    start = CinematicGlassSpacing.SafeAreaX,
                    end = CinematicGlassSpacing.SafeAreaX,
                    bottom = CinematicGlassSpacing.SafeAreaY,
                ),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                QuickSettingPill(
                    icon = Icons.AutoMirrored.Filled.VolumeUp,
                    label = "Audio",
                    value = "暂未支持",
                    selected = state.selectedQuickPanel == PlayerQuickPanel.Audio,
                    enabled = false,
                    disabledReason = "Audio 暂未支持",
                    onClick = { onQuickPanel(PlayerQuickPanel.Audio) },
                    onUnsupported = onUnsupported,
                )
                QuickSettingPill(
                    icon = Icons.Filled.Subtitles,
                    label = "Subtitles",
                    value = "暂未支持",
                    selected = state.selectedQuickPanel == PlayerQuickPanel.Subtitles,
                    enabled = false,
                    disabledReason = "Subtitles 暂未支持",
                    onClick = { onQuickPanel(PlayerQuickPanel.Subtitles) },
                    onUnsupported = onUnsupported,
                )
                QuickSettingPill(
                    icon = Icons.AutoMirrored.Filled.Chat,
                    label = "Danmaku",
                    value = if (state.danmakuEnabled) "On" else "Off",
                    selected = state.selectedQuickPanel == PlayerQuickPanel.Danmaku,
                    enabled = true,
                    disabledReason = null,
                    onClick = onToggleDanmaku,
                    onUnsupported = onUnsupported,
                )
            }
            ProgressRail(state = state)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OsdIconButton(
                    icon = Icons.Filled.SkipPrevious,
                    label = "上一集",
                    enabled = false,
                    disabledReason = "上一集暂未支持",
                    onClick = {},
                    onUnsupported = onUnsupported,
                )
                Spacer(modifier = Modifier.width(28.dp))
                OsdIconButton(icon = Icons.Filled.Replay10, label = "快退 10 秒", onClick = onReplay10, large = true, onUnsupported = onUnsupported)
                Spacer(modifier = Modifier.width(34.dp))
                OsdIconButton(
                    icon = if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    label = if (state.isPlaying) "暂停" else "播放",
                    onClick = onPlayPause,
                    primary = true,
                    focusRequester = playFocusRequester,
                    onUnsupported = onUnsupported,
                )
                Spacer(modifier = Modifier.width(34.dp))
                OsdIconButton(icon = Icons.Filled.Forward10, label = "快进 10 秒", onClick = onForward10, large = true, onUnsupported = onUnsupported)
                Spacer(modifier = Modifier.width(28.dp))
                OsdIconButton(
                    icon = Icons.Filled.SkipNext,
                    label = "下一集",
                    enabled = false,
                    disabledReason = "下一集暂未支持",
                    onClick = {},
                    onUnsupported = onUnsupported,
                )
            }
        }
        RemoteHint(
            message = state.feedbackMessage,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(top = 160.dp),
        )
    }
}

@Composable
private fun QuickSettingPill(
    icon: ImageVector,
    label: String,
    value: String,
    selected: Boolean,
    enabled: Boolean,
    disabledReason: String?,
    onClick: () -> Unit,
    onUnsupported: (String) -> Unit,
) {
    FocusableGlassSurface(
        cornerRadius = 12.dp,
        enabled = enabled,
        disabledReason = disabledReason,
        onClick = onClick,
        onDisabledClick = onUnsupported,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = if (selected) CinematicGlassColors.Primary else CinematicGlassColors.OnSurfaceVariant)
            Column {
                Text(text = label.uppercase(), color = CinematicGlassColors.OnSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text(text = value, color = CinematicGlassColors.OnSurface, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun ProgressRail(state: PlayerOsdState) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .background(Color.White.copy(alpha = 0.16f), RoundedCornerShape(999.dp)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(state.progressFraction)
                    .height(6.dp)
                    .background(CinematicGlassColors.Primary, RoundedCornerShape(999.dp)),
            )
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(state.positionMs.toClockLabel(), color = CinematicGlassColors.OnSurfaceVariant, fontSize = 13.sp)
            Text("Remaining: ${(state.durationMs - state.positionMs).coerceAtLeast(0L).toClockLabel()}", color = CinematicGlassColors.OnSurface, fontSize = 13.sp)
        }
    }
}

@Composable
private fun OsdIconButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    primary: Boolean = false,
    large: Boolean = false,
    enabled: Boolean = true,
    disabledReason: String? = null,
    focusRequester: FocusRequester? = null,
    onUnsupported: (String) -> Unit,
) {
    val size = when {
        primary -> 76.dp
        large -> 58.dp
        else -> 48.dp
    }
    FocusableGlassSurface(
        modifier = Modifier
            .size(size)
            .then(focusRequester?.let { Modifier.focusRequester(it) } ?: Modifier),
        cornerRadius = 999.dp,
        enabled = enabled,
        disabledReason = disabledReason,
        onClick = onClick,
        onDisabledClick = onUnsupported,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(if (primary) CinematicGlassColors.Primary else Color.Transparent),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = if (primary) CinematicGlassColors.OnPrimary else CinematicGlassColors.OnSurface,
                modifier = Modifier.size(if (primary) 42.dp else 30.dp),
            )
        }
    }
}

private fun Long.toClockLabel(): String {
    val totalSeconds = (this / 1_000L).coerceAtLeast(0L)
    val hours = totalSeconds / 3_600L
    val minutes = (totalSeconds % 3_600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }
}
