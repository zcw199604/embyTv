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
import androidx.compose.runtime.rememberUpdatedState
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
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.Tracks
import androidx.media3.ui.PlayerView
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.embytv.core.di.AppContainer
import com.embytv.domain.model.MediaItemSummary
import com.embytv.domain.model.PlaybackSource
import com.embytv.domain.model.PlayerTrackOption
import com.embytv.domain.model.PlayerTrackType
import com.embytv.ui.components.FocusableGlassSurface
import com.embytv.ui.components.GlassPanel
import com.embytv.ui.components.RemoteHint
import com.embytv.ui.theme.CinematicGlassColors
import com.embytv.ui.theme.CinematicGlassSpacing
import com.kuaishou.akdanmaku.DanmakuConfig
import com.kuaishou.akdanmaku.ui.DanmakuView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun PlayerScreen(
    container: AppContainer,
    playbackSource: PlaybackSource,
    onBack: () -> Unit,
    onPlayNext: (MediaItemSummary) -> Unit = {},
) {
    val player = remember { container.playerFactory.createPlayer() }
    val danmakuPlayer = remember { container.danmakuBridge.createPlayer() }
    val focusRequester = remember { FocusRequester() }
    val lifecycleOwner = LocalLifecycleOwner.current
    val reportingCoordinator = remember(playbackSource) {
        PlaybackReportingCoordinator { event ->
            reportPlaybackEvent(container, playbackSource, event)
        }
    }
    val currentPlaybackSource by rememberUpdatedState(playbackSource)
    val currentReportingCoordinator by rememberUpdatedState(reportingCoordinator)
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
        reportingCoordinator.onStarted(positionMs = 0L)

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
        reportingCoordinator.onPauseChanged(
            positionMs = player.currentPosition,
            isPaused = !osdState.isPlaying,
        )
    }

    LaunchedEffect(osdState.danmakuEnabled, osdState.danmakuPaused) {
        if (osdState.danmakuEnabled && !osdState.danmakuPaused) {
            danmakuPlayer.start()
        } else {
            danmakuPlayer.pause()
        }
    }

    LaunchedEffect(player, playbackSource) {
        while (true) {
            dispatch(
                PlayerOsdAction.ProgressChanged(
                    positionMs = player.currentPosition,
                    durationMs = player.duration.takeIf { it > 0L } ?: 0L,
                ),
            )
            currentReportingCoordinator.onProgressTick(
                positionMs = player.currentPosition,
                isPaused = !player.isPlaying,
            )
            delay(1_000)
        }
    }

    DisposableEffect(player, playbackSource, reportingCoordinator) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    currentReportingCoordinator.onStopped(
                        positionMs = player.duration.takeIf { it > 0L } ?: player.currentPosition,
                    )
                    val next = currentPlaybackSource.queue?.next
                    if (currentPlaybackSource.queue?.autoPlayNext == true && next != null) {
                        onPlayNext(next)
                    }
                }
            }

            override fun onTracksChanged(tracks: Tracks) {
                dispatch(
                    PlayerOsdAction.TracksChanged(
                        audioTracks = tracks.toTrackOptions(C.TRACK_TYPE_AUDIO),
                        subtitleTracks = tracks.toTrackOptions(C.TRACK_TYPE_TEXT),
                    ),
                )
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
        }
    }

    DisposableEffect(lifecycleOwner, player, danmakuPlayer) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    if (osdState.isPlaying) {
                        player.play()
                        currentReportingCoordinator.onPauseChanged(
                            positionMs = player.currentPosition,
                            isPaused = false,
                        )
                    }
                    if (osdState.danmakuEnabled && !osdState.danmakuPaused) {
                        danmakuPlayer.start()
                    }
                }
                Lifecycle.Event.ON_PAUSE -> {
                    currentReportingCoordinator.onPauseChanged(
                        positionMs = player.currentPosition,
                        isPaused = true,
                    )
                    player.pause()
                    danmakuPlayer.pause()
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            currentReportingCoordinator.onStopped(positionMs = player.currentPosition)
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
            details = playbackSource.details,
            state = osdState,
            onBack = { dispatch(PlayerOsdAction.BackPressed) },
            onPlayPause = { dispatch(PlayerOsdAction.TogglePlayPause) },
            onReplay10 = {
                val target = (player.currentPosition - 10_000L).coerceAtLeast(0L)
                player.seekTo(target)
                reportingCoordinator.onSeek(positionMs = target, isPaused = !player.isPlaying)
                dispatch(PlayerOsdAction.UserInteraction)
            },
            onForward10 = {
                val requestedTarget = player.currentPosition + 10_000L
                val target = player.duration.takeIf { it > 0L }?.let { duration ->
                    requestedTarget.coerceAtMost(duration)
                } ?: requestedTarget
                player.seekTo(target)
                reportingCoordinator.onSeek(positionMs = target, isPaused = !player.isPlaying)
                dispatch(PlayerOsdAction.UserInteraction)
            },
            onPrevious = {
                playbackSource.queue?.previous?.let { previous ->
                    reportingCoordinator.onStopped(positionMs = player.currentPosition)
                    onPlayNext(previous)
                }
                    ?: dispatch(PlayerOsdAction.UnsupportedAction("没有上一集"))
            },
            onNext = {
                playbackSource.queue?.next?.let { next ->
                    reportingCoordinator.onStopped(positionMs = player.currentPosition)
                    onPlayNext(next)
                }
                    ?: dispatch(PlayerOsdAction.UnsupportedAction("没有下一集"))
            },
            onSelectTrack = { option ->
                if (option.type == PlayerTrackType.Subtitle) {
                    player.trackSelectionParameters = player.trackSelectionParameters
                        .buildUpon()
                        .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                        .build()
                }
                player.trackSelectionParameters = player.trackSelectionParameters
                    .buildUpon()
                    .setOverrideForType(TrackSelectionOverride(option.trackGroup, option.trackIndex))
                    .build()
                dispatch(PlayerOsdAction.UserInteraction)
            },
            onDisableSubtitles = {
                player.trackSelectionParameters = player.trackSelectionParameters
                    .buildUpon()
                    .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                    .build()
                dispatch(PlayerOsdAction.DisableSubtitles)
            },
            onToggleDanmaku = { dispatch(PlayerOsdAction.ToggleDanmaku) },
            onQuickPanel = { dispatch(PlayerOsdAction.SelectQuickPanel(it)) },
            onUnsupported = { dispatch(PlayerOsdAction.UnsupportedAction(it)) },
            onClearFeedback = { dispatch(PlayerOsdAction.ClearFeedback) },
        )
    }
}

private fun reportPlaybackEvent(
    container: AppContainer,
    playbackSource: PlaybackSource,
    event: PlaybackReportEvent,
) {
    val session = playbackSource.session ?: return
    val deviceId = playbackSource.deviceId ?: return
    container.applicationScope.launch {
        when (event) {
            is PlaybackReportEvent.Started -> {
                container.embyRepository.reportPlaybackStarted(
                    session = session,
                    deviceId = deviceId,
                    source = playbackSource,
                    positionMs = event.positionMs,
                )
            }
            is PlaybackReportEvent.Progress -> {
                container.embyRepository.reportPlaybackProgress(
                    session = session,
                    deviceId = deviceId,
                    source = playbackSource,
                    positionMs = event.positionMs,
                    isPaused = event.isPaused,
                )
            }
            is PlaybackReportEvent.Stopped -> {
                container.embyRepository.reportPlaybackStopped(
                    session = session,
                    deviceId = deviceId,
                    source = playbackSource,
                    positionMs = event.positionMs,
                )
            }
        }
    }
}

@Composable
private fun PlayerOsdOverlay(
    title: String,
    details: com.embytv.domain.model.PlaybackDetails,
    state: PlayerOsdState,
    onBack: () -> Unit,
    onPlayPause: () -> Unit,
    onReplay10: () -> Unit,
    onForward10: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSelectTrack: (PlayerTrackOption) -> Unit,
    onDisableSubtitles: () -> Unit,
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
                    text = details.playbackSummaryLabel,
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
                Text(details.qualityLabel, color = CinematicGlassColors.OnSurfaceVariant, fontSize = 14.sp)
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
                    value = details.audioLabel,
                    selected = state.selectedQuickPanel == PlayerQuickPanel.Audio,
                    enabled = state.audioTracks.isNotEmpty(),
                    disabledReason = if (state.audioTracks.isEmpty()) "没有可用音轨信息" else null,
                    onClick = { onQuickPanel(PlayerQuickPanel.Audio) },
                    onUnsupported = onUnsupported,
                )
                QuickSettingPill(
                    icon = Icons.Filled.Subtitles,
                    label = "Subtitles",
                    value = details.subtitleLabel,
                    selected = state.selectedQuickPanel == PlayerQuickPanel.Subtitles,
                    enabled = state.subtitleTracks.isNotEmpty(),
                    disabledReason = if (state.subtitleTracks.isEmpty()) "当前媒体没有字幕" else null,
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
            TrackQuickPanel(
                state = state,
                onSelectTrack = onSelectTrack,
                onDisableSubtitles = onDisableSubtitles,
            )
            ProgressRail(state = state)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OsdIconButton(
                    icon = Icons.Filled.SkipPrevious,
                    label = "上一集",
                    enabled = true,
                    disabledReason = null,
                    onClick = onPrevious,
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
                    enabled = true,
                    disabledReason = null,
                    onClick = onNext,
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
private fun TrackQuickPanel(
    state: PlayerOsdState,
    onSelectTrack: (PlayerTrackOption) -> Unit,
    onDisableSubtitles: () -> Unit,
) {
    val tracks = when (state.selectedQuickPanel) {
        PlayerQuickPanel.Audio -> state.audioTracks
        PlayerQuickPanel.Subtitles -> state.subtitleTracks
        else -> emptyList()
    }
    if (tracks.isEmpty() && state.selectedQuickPanel != PlayerQuickPanel.Subtitles) return
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (state.selectedQuickPanel == PlayerQuickPanel.Subtitles) {
            QuickTrackButton(
                label = "关闭字幕",
                selected = state.subtitleDisabled,
                onClick = onDisableSubtitles,
            )
        }
        tracks.take(6).forEach { track ->
            QuickTrackButton(
                label = track.label,
                selected = track.selected,
                onClick = { onSelectTrack(track) },
            )
        }
    }
}

@Composable
private fun QuickTrackButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    FocusableGlassSurface(
        cornerRadius = 999.dp,
        onClick = onClick,
    ) {
        Text(
            text = label,
            color = if (selected) CinematicGlassColors.Primary else CinematicGlassColors.OnSurface,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
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

private fun Tracks.toTrackOptions(trackType: Int): List<PlayerTrackOption> =
    groups
        .mapIndexedNotNull { groupIndex, group ->
            if (group.type != trackType || !group.isSupported) return@mapIndexedNotNull null
            groupIndex to group
        }
        .flatMap { (groupIndex, group) ->
            (0 until group.length).map { trackIndex ->
                val format = group.getTrackFormat(trackIndex)
                PlayerTrackOption(
                    id = "$groupIndex:$trackIndex",
                    label = format.label
                        ?: format.language?.takeIf { it.isNotBlank() }
                        ?: format.sampleMimeType?.substringAfterLast('/')
                        ?: "Track ${trackIndex + 1}",
                    type = if (trackType == C.TRACK_TYPE_AUDIO) {
                        PlayerTrackType.Audio
                    } else {
                        PlayerTrackType.Subtitle
                    },
                    trackGroup = group.mediaTrackGroup,
                    trackIndex = trackIndex,
                    selected = group.isTrackSelected(trackIndex),
                )
            }
        }
