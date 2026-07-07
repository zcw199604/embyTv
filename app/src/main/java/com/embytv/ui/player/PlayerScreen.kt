package com.embytv.ui.player

import android.view.ViewGroup
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.ui.PlayerView
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.embytv.R
import com.embytv.core.di.AppContainer
import com.embytv.domain.model.EmbyMediaDetail
import com.embytv.domain.model.MediaItemSummary
import com.embytv.domain.model.PlaybackSource
import com.embytv.domain.model.PlayerTrackOption
import com.embytv.domain.model.previewThumbnailFor
import com.embytv.ui.components.FocusableGlassSurface
import com.embytv.ui.components.GlassPanel
import com.embytv.ui.components.NetworkBackdropImage
import com.embytv.ui.components.RemoteHint
import com.embytv.ui.theme.CinematicGlassColors
import com.embytv.ui.theme.CinematicGlassSpacing
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun PlayerScreen(
    container: AppContainer,
    playbackSource: PlaybackSource,
    onBack: () -> Unit,
    onPlayNext: (MediaItemSummary) -> Unit = {},
) {
    val noPreviousMessage = stringResource(R.string.player_no_previous)
    val noNextMessage = stringResource(R.string.player_no_next)
    val subtitlesDisabledFeedback = stringResource(R.string.player_subtitles_disabled)
    val playbackSpeedFeedbackFormat = stringResource(R.string.player_playback_speed_feedback)
    val playbackFailedMessage = stringResource(R.string.player_playback_failed)
    val detailsFailedMessage = stringResource(R.string.player_details_failed)
    val player = remember { container.playerFactory.createPlayer() }
    val danmakuPlayer = remember { container.danmakuBridge.createPlayer() }
    val startPositionMs = PlayerStartupPositionPolicy.normalize(playbackSource.startPositionMs)
    val playerManager = remember(playbackSource, startPositionMs) {
        PlayerManager(
            initialState = PlayerOsdState(positionMs = startPositionMs),
            seekThumbnailProvider = playbackSource::previewThumbnailFor,
        )
    }
    val osdState by playerManager.state.collectAsState()
    val focusRequester = remember { FocusRequester() }
    val lifecycleOwner = LocalLifecycleOwner.current
    val reportingCoordinator = remember(playbackSource) {
        PlaybackReportingCoordinator { event ->
            reportPlaybackEvent(container, playbackSource, event)
        }
    }
    val currentPlaybackSource by rememberUpdatedState(playbackSource)
    val currentReportingCoordinator by rememberUpdatedState(reportingCoordinator)
    val currentOsdState by rememberUpdatedState(osdState)
    val shouldDisplayDetailOverlay = PlayerDetailOverlayVisibilityPolicy.shouldDisplay(
        PlayerDetailOverlayVisibilitySnapshot(
            osdVisible = osdState.visible,
            status = osdState.status,
        ),
    )
    val queueNavigationState = PlayerQueueNavigationPolicy.resolve(
        queue = playbackSource.queue,
        noPreviousReason = noPreviousMessage,
        noNextReason = noNextMessage,
    )
    val currentQueueNavigationState by rememberUpdatedState(queueNavigationState)

    fun dispatch(action: PlayerOsdAction) {
        playerManager.dispatch(action)
        if (playerManager.consumeExitRequested()) {
            onBack()
        }
    }

    fun applyDanmakuPlaybackCommand(command: DanmakuPlaybackCommand) {
        when (command) {
            is DanmakuPlaybackCommand.Start -> {
                danmakuPlayer.updateConfig(command.config)
                danmakuPlayer.start(command.config)
            }
            DanmakuPlaybackCommand.Pause -> danmakuPlayer.pause()
        }
    }

    fun syncDanmakuTo(positionMs: Long, state: PlayerOsdState = currentOsdState) {
        when (val command = DanmakuPlaybackPolicy.commandForSeek(positionMs)) {
            is DanmakuSyncCommand.ClearAndSeek -> {
                danmakuPlayer.stop()
                danmakuPlayer.seekTo(command.positionMs)
                applyDanmakuPlaybackCommand(
                    DanmakuPlaybackPolicy.commandForPlayback(
                        settings = state.danmakuSettings,
                        enabled = state.danmakuEnabled,
                        paused = state.danmakuPaused,
                    ),
                )
            }
        }
    }

    fun applyPlaybackUpdate(update: PlayerPlaybackUpdate) {
        update.actions.forEach(::dispatch)
        update.effects.forEach { effect ->
            when (effect) {
                is PlayerPlaybackEffect.ReportStopped -> {
                    currentReportingCoordinator.onStopped(positionMs = effect.positionMs)
                }
                is PlayerPlaybackEffect.ReportSeek -> {
                    currentReportingCoordinator.onSeek(
                        positionMs = effect.positionMs,
                        isPaused = effect.isPaused,
                    )
                }
                is PlayerPlaybackEffect.SyncDanmaku -> {
                    syncDanmakuTo(effect.positionMs)
                }
                PlayerPlaybackEffect.PlayNext -> {
                    currentQueueNavigationState.autoPlayNextTarget?.let(onPlayNext)
                }
            }
        }
    }

    fun seekBy(deltaMs: Long) {
        playerManager.requestSeekPreview(deltaMs)
        val target = playerManager.state.value.seekPreview?.targetPositionMs ?: player.currentPosition
        player.seekTo(target)
        syncDanmakuTo(target, playerManager.state.value)
        reportingCoordinator.onSeek(positionMs = target, isPaused = !player.isPlaying)
        dispatch(PlayerOsdAction.SeekPreviewCommitted)
    }

    fun executeRemoteKeyCommand(command: PlayerRemoteKeyCommand): Boolean =
        when (command) {
            is PlayerRemoteKeyCommand.Dispatch -> {
                dispatch(command.action)
                true
            }
            is PlayerRemoteKeyCommand.SeekBy -> {
                seekBy(command.deltaMs)
                true
            }
            PlayerRemoteKeyCommand.Ignore -> false
        }

    fun executePreviewRemoteKeyCommand(command: PlayerRemoteKeyCommand): Boolean =
        when (command) {
            is PlayerRemoteKeyCommand.SeekBy -> executeRemoteKeyCommand(command)
            PlayerRemoteKeyCommand.Dispatch(PlayerOsdAction.BackPressed) -> executeRemoteKeyCommand(command)
            else -> false
        }

    LaunchedEffect(playbackSource) {
        dispatch(PlayerOsdAction.PlaybackStatusChanged(PlaybackEngineStatus.Loading))
        player.setMediaItem(PlayerMediaItemFactory.create(playbackSource))
        player.prepare()
        if (startPositionMs > 0L) {
            player.seekTo(startPositionMs)
        }
        player.playWhenReady = true
        reportingCoordinator.onStarted(positionMs = startPositionMs)

        danmakuPlayer.stop()
        danmakuPlayer.updateData(container.danmakuBridge.toAkItems(playbackSource.danmaku))
        when (val command = DanmakuPlaybackPolicy.commandForSeek(startPositionMs)) {
            is DanmakuSyncCommand.ClearAndSeek -> danmakuPlayer.seekTo(command.positionMs)
        }
        applyDanmakuPlaybackCommand(
            DanmakuPlaybackPolicy.commandForPlayback(
                settings = osdState.danmakuSettings,
                enabled = osdState.danmakuEnabled,
                paused = osdState.danmakuPaused,
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

    LaunchedEffect(osdState.playbackSpeed) {
        player.setPlaybackSpeed(osdState.playbackSpeed)
    }

    LaunchedEffect(osdState.visible, osdState.interactionRevision, osdState.status, osdState.selectedQuickPanel) {
        if (
            PlayerOsdAutoHidePolicy.shouldScheduleAutoHide(
                osdState.toAutoHideSnapshot(),
            )
        ) {
            delay(PlayerOsdAutoHidePolicy.AUTO_HIDE_DELAY_MS)
            dispatch(PlayerOsdAction.Hide)
        }
    }

    LaunchedEffect(player, playbackSource) {
        while (true) {
            applyPlaybackUpdate(
                PlayerPlaybackController.onProgressTick(
                    positionMs = player.currentPosition,
                    durationMs = player.duration.takeIf { it > 0L } ?: 0L,
                    bufferedFraction = player.bufferedFraction(),
                ),
            )
            currentReportingCoordinator.onProgressTick(
                positionMs = player.currentPosition,
                isPaused = !player.isPlaying,
            )
            delay(PlayerPlaybackController.UI_PROGRESS_INTERVAL_MS)
        }
    }

    LaunchedEffect(
        playbackSource.itemId,
        playbackSource.session,
        playbackSource.deviceId,
        shouldDisplayDetailOverlay,
    ) {
        val session = playbackSource.session
        val deviceId = playbackSource.deviceId
        val snapshot = PlayerDetailOverlayLoadSnapshot(
            currentItemId = playbackSource.itemId,
            overlayItemId = osdState.detailOverlay.itemId,
            shouldDisplayOverlay = shouldDisplayDetailOverlay,
            sessionAvailable = session != null && deviceId != null,
            isLoading = osdState.detailOverlay.isLoading,
            hasDetail = osdState.detailOverlay.detail != null,
            hasError = osdState.detailOverlay.errorMessage != null,
        )
        if (!PlayerDetailOverlayLoadPolicy.shouldRequestLoad(snapshot) || session == null || deviceId == null) {
            return@LaunchedEffect
        }
        dispatch(PlayerOsdAction.DetailOverlayLoading(playbackSource.itemId))
        container.embyRepository.loadPlaybackOverlayDetails(session, deviceId, playbackSource.itemId)
            .onSuccess {
                dispatch(PlayerOsdAction.DetailOverlayLoaded(playbackSource.itemId, it.mediaDetail, it.playbackDetails))
            }
            .onFailure {
                dispatch(PlayerOsdAction.DetailOverlayFailed(playbackSource.itemId, it.message ?: detailsFailedMessage))
            }
    }

    DisposableEffect(playbackSource, reportingCoordinator) {
        onDispose {
            reportingCoordinator.onStopped(positionMs = player.currentPosition)
        }
    }

    DisposableEffect(player, playbackSource, reportingCoordinator) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                applyPlaybackUpdate(
                    PlayerPlaybackController.onPlaybackStateChanged(
                        playbackState = playbackState,
                        isPlaying = player.isPlaying,
                        currentPositionMs = player.currentPosition,
                        durationMs = player.duration,
                        shouldAutoPlayNext = currentQueueNavigationState.autoPlayNextTarget != null,
                    ),
                )
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                applyPlaybackUpdate(
                    PlayerPlaybackController.onIsPlayingChanged(
                        isPlaying = isPlaying,
                        playbackState = player.playbackState,
                    ),
                )
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                applyPlaybackUpdate(PlayerPlaybackController.onPlayerError(error.message, playbackFailedMessage))
            }

            override fun onPositionDiscontinuity(
                oldPosition: Player.PositionInfo,
                newPosition: Player.PositionInfo,
                reason: Int,
            ) {
                applyPlaybackUpdate(
                    PlayerPlaybackController.onPositionDiscontinuity(
                        newPositionMs = newPosition.positionMs,
                        isPlaying = player.isPlaying,
                        reason = reason,
                    ),
                )
            }

            override fun onPlaybackParametersChanged(playbackParameters: androidx.media3.common.PlaybackParameters) {
                applyPlaybackUpdate(PlayerPlaybackController.onPlaybackSpeedChanged(playbackParameters.speed))
            }

            override fun onRenderedFirstFrame() {
                applyPlaybackUpdate(PlayerPlaybackController.onRenderedFirstFrame(player.isPlaying))
            }

            override fun onTracksChanged(tracks: Tracks) {
                dispatch(
                    PlayerOsdAction.TracksChanged(
                        audioTracks = tracks.toPlayerTrackOptions(C.TRACK_TYPE_AUDIO),
                        subtitleTracks = tracks.toPlayerTrackOptions(C.TRACK_TYPE_TEXT),
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
        fun applyLifecyclePlaybackEffect(effect: PlayerLifecyclePlaybackEffect) {
            when (effect) {
                PlayerLifecyclePlaybackEffect.PlayPlayer -> player.play()
                PlayerLifecyclePlaybackEffect.PausePlayer -> player.pause()
                PlayerLifecyclePlaybackEffect.PauseDanmaku -> danmakuPlayer.pause()
                is PlayerLifecyclePlaybackEffect.ReportPauseChanged -> {
                    currentReportingCoordinator.onPauseChanged(
                        positionMs = player.currentPosition,
                        isPaused = effect.isPaused,
                    )
                }
                is PlayerLifecyclePlaybackEffect.ApplyDanmaku -> applyDanmakuPlaybackCommand(effect.command)
            }
        }

        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    val latestOsdState = currentOsdState
                    PlayerLifecyclePlaybackPolicy.onResume(
                        PlayerLifecyclePlaybackSnapshot(
                            isPlaying = latestOsdState.isPlaying,
                            danmakuEnabled = latestOsdState.danmakuEnabled,
                            danmakuPaused = latestOsdState.danmakuPaused,
                            danmakuSettings = latestOsdState.danmakuSettings,
                        ),
                    ).forEach(::applyLifecyclePlaybackEffect)
                }
                Lifecycle.Event.ON_PAUSE -> {
                    PlayerLifecyclePlaybackPolicy.onPause().forEach(::applyLifecyclePlaybackEffect)
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
                executePreviewRemoteKeyCommand(
                    PlayerRemoteKeyPolicy.commandFor(
                        eventType = event.type.toPlayerRemoteKeyEventType(),
                        key = event.key.toPlayerRemoteKey(),
                        osdVisible = osdState.visible,
                    ),
                )
            }
            .onKeyEvent { event ->
                executeRemoteKeyCommand(
                    PlayerRemoteKeyPolicy.commandFor(
                        eventType = event.type.toPlayerRemoteKeyEventType(),
                        key = event.key.toPlayerRemoteKey(),
                        osdVisible = osdState.visible,
                    ),
                )
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

        DanmakuOverlay(
            danmakuPlayer = danmakuPlayer,
            settings = osdState.danmakuSettings,
            enabled = osdState.danmakuEnabled,
            paused = osdState.danmakuPaused,
            modifier = Modifier
                .fillMaxSize(),
        )

        if (shouldDisplayDetailOverlay && !osdState.visible) {
            PlaybackDetailsOverlay(
                overlayState = osdState.detailOverlay,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = CinematicGlassSpacing.SafeAreaX, top = 10.dp),
            )
        }

        PlayerOsdOverlay(
            title = playbackSource.title,
            playbackItemId = playbackSource.itemId,
            contextLabel = playbackSource.contextLabel,
            details = playbackSource.details,
            state = osdState,
            onBack = { dispatch(PlayerOsdAction.BackPressed) },
            onPlayPause = { dispatch(PlayerOsdAction.TogglePlayPause) },
            onReplay10 = {
                seekBy(-10_000L)
            },
            onForward10 = {
                seekBy(10_000L)
            },
            previousNavigation = queueNavigationState.previous,
            nextNavigation = queueNavigationState.next,
            onPrevious = {
                queueNavigationState.previous.target?.let { previous ->
                    reportingCoordinator.onStopped(positionMs = player.currentPosition)
                    onPlayNext(previous)
                }
                    ?: dispatch(PlayerOsdAction.UnsupportedAction(noPreviousMessage))
            },
            onNext = {
                queueNavigationState.next.target?.let { next ->
                    reportingCoordinator.onStopped(positionMs = player.currentPosition)
                    onPlayNext(next)
                }
                    ?: dispatch(PlayerOsdAction.UnsupportedAction(noNextMessage))
            },
            onSelectTrack = { option ->
                player.trackSelectionParameters = player.trackSelectionParameters.selectTrack(option)
                dispatch(PlayerOsdAction.SelectTrack(option))
            },
            onDisableSubtitles = {
                player.trackSelectionParameters = player.trackSelectionParameters.disableSubtitles()
                dispatch(PlayerOsdAction.DisableSubtitles(subtitlesDisabledFeedback))
            },
            onSelectSpeed = { speed ->
                val feedback = playbackSpeedFeedbackFormat.format(
                    speed.nearestSupportedPlaybackSpeed().toSpeedLabel(),
                )
                dispatch(PlayerOsdAction.SelectPlaybackSpeed(speed, feedback))
            },
            onSetDanmakuEnabled = { dispatch(PlayerOsdAction.SetDanmakuEnabled(it)) },
            onUpdateDanmaku = { opacity, textScale, displayArea ->
                dispatch(
                    PlayerOsdAction.UpdateDanmakuSettings(
                        opacity = opacity,
                        textSizeScale = textScale,
                        displayArea = displayArea,
                    ),
                )
            },
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
        PlayerPlaybackDiagnostics.reportQueued(playbackSource, event)
        runCatching {
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
        }.onSuccess {
            PlayerPlaybackDiagnostics.reportSucceeded(playbackSource, event)
        }.onFailure { throwable ->
            PlayerPlaybackDiagnostics.reportFailed(playbackSource, event, throwable)
        }
    }
}

@Composable
private fun PlayerOsdOverlay(
    title: String,
    playbackItemId: String,
    contextLabel: String?,
    details: com.embytv.domain.model.PlaybackDetails,
    state: PlayerOsdState,
    onBack: () -> Unit,
    onPlayPause: () -> Unit,
    onReplay10: () -> Unit,
    onForward10: () -> Unit,
    previousNavigation: PlayerQueueNavigationItemState,
    nextNavigation: PlayerQueueNavigationItemState,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSelectTrack: (PlayerTrackOption) -> Unit,
    onDisableSubtitles: () -> Unit,
    onSelectSpeed: (Float) -> Unit,
    onSetDanmakuEnabled: (Boolean) -> Unit,
    onUpdateDanmaku: (Float?, Float?, DanmakuDisplayArea?) -> Unit,
    onQuickPanel: (PlayerQuickPanel?) -> Unit,
    onUnsupported: (String) -> Unit,
    onClearFeedback: () -> Unit,
) {
    val playFocusRequester = remember { FocusRequester() }
    val quickPanelFocusRequester = remember { FocusRequester() }
    val lastFocusSnapshot = remember { mutableStateOf<PlayerOsdFocusSnapshot?>(null) }
    val lastQuickPanelFocusSnapshot = remember { mutableStateOf<PlayerQuickPanelFocusSnapshot?>(null) }
    val currentDetails = state.detailOverlay.playbackDetails ?: details
    val backLabel = stringResource(R.string.player_back)
    val fallbackTitle = stringResource(R.string.player_title_fallback)
    val audioLabel = stringResource(R.string.player_audio)
    val subtitlesLabel = stringResource(R.string.player_subtitles)
    val speedLabel = stringResource(R.string.player_speed)
    val danmakuLabel = stringResource(R.string.player_danmaku)
    val onLabel = stringResource(R.string.player_on)
    val offLabel = stringResource(R.string.player_off)
    val noAudioLabel = stringResource(R.string.player_no_audio)
    val noSubtitlesLabel = stringResource(R.string.player_no_subtitles)
    val directPlayLabel = stringResource(R.string.player_direct_play)
    val unknownQualityLabel = stringResource(R.string.player_unknown_quality)
    val previousLabel = stringResource(R.string.player_previous)
    val nextLabel = stringResource(R.string.player_next)
    val replay10Label = stringResource(R.string.player_replay_10)
    val forward10Label = stringResource(R.string.player_forward_10)
    val pauseLabel = stringResource(R.string.player_pause)
    val playLabel = stringResource(R.string.player_play)
    val trackAvailability = PlayerTrackAvailabilityResolver.resolve(state, currentDetails)
    val quickPanelFocusableOptionCount = PlayerQuickPanelFocusPolicy.focusableOptionCount(state)
    val detailLabels = PlayerPlaybackDetailsLabelResolver.resolve(
        details = currentDetails,
        directPlayLabel = directPlayLabel,
        unknownQualityLabel = unknownQualityLabel,
        noAudioLabel = noAudioLabel,
        noSubtitlesLabel = noSubtitlesLabel,
    )
    val trackSummaryLabels = PlayerTrackSummaryLabelResolver.resolve(
        state = state,
        details = currentDetails,
        noAudioLabel = noAudioLabel,
        noSubtitlesLabel = noSubtitlesLabel,
    )

    LaunchedEffect(playbackItemId, state.visible, state.interactionRevision, state.selectedQuickPanel) {
        val current = PlayerOsdFocusSnapshot(
            playbackItemId = playbackItemId,
            visible = state.visible,
            interactionRevision = state.interactionRevision,
            selectedQuickPanel = state.selectedQuickPanel,
        )
        if (PlayerOsdFocusController.shouldRequestPrimaryFocus(lastFocusSnapshot.value, current)) {
            playFocusRequester.requestFocus()
        }
        lastFocusSnapshot.value = current
    }

    LaunchedEffect(state.visible, state.selectedQuickPanel, quickPanelFocusableOptionCount) {
        val current = PlayerQuickPanelFocusSnapshot(
            visible = state.visible,
            selectedQuickPanel = state.selectedQuickPanel,
            focusableOptionCount = quickPanelFocusableOptionCount,
        )
        if (PlayerQuickPanelFocusPolicy.shouldRequestPanelFocus(lastQuickPanelFocusSnapshot.value, current)) {
            quickPanelFocusRequester.requestFocus()
        }
        lastQuickPanelFocusSnapshot.value = current
    }

    val motionSpec = PlayerOsdMotionPolicy.TvDefault
    AnimatedVisibility(
        visible = state.visible,
        enter = fadeIn(animationSpec = tween(motionSpec.enterDurationMs)) +
            slideInVertically(
                animationSpec = tween(motionSpec.enterDurationMs),
                initialOffsetY = { motionSpec.enterOffsetPx(it) },
            ),
        exit = fadeOut(animationSpec = tween(motionSpec.exitDurationMs)) +
            slideOutVertically(
                animationSpec = tween(motionSpec.exitDurationMs),
                targetOffsetY = { motionSpec.exitOffsetPx(it) },
            ),
    ) {
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
            OsdIconButton(icon = Icons.AutoMirrored.Filled.ArrowBack, label = backLabel, onClick = onBack, onUnsupported = onUnsupported)
            Column {
                Text(
                    text = title.ifBlank { fallbackTitle },
                    color = CinematicGlassColors.Primary,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                contextLabel?.takeIf { it.isNotBlank() }?.let { label ->
                    Text(
                        text = label,
                        color = CinematicGlassColors.OnSurface,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = detailLabels.summary,
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
                Text(detailLabels.quality, color = CinematicGlassColors.OnSurfaceVariant, fontSize = 14.sp)
            }
        }
        PlaybackDetailsOverlay(
            overlayState = state.detailOverlay,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = CinematicGlassSpacing.SafeAreaX, top = 10.dp),
        )

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
                    label = audioLabel,
                    value = trackSummaryLabels.audio,
                    selected = state.selectedQuickPanel == PlayerQuickPanel.Audio,
                    enabled = trackAvailability.hasAudio,
                    disabledReason = if (trackAvailability.hasAudio) null else noAudioLabel,
                    onClick = { onQuickPanel(PlayerQuickPanel.Audio) },
                    onUnsupported = onUnsupported,
                )
                QuickSettingPill(
                    icon = Icons.Filled.Subtitles,
                    label = subtitlesLabel,
                    value = trackSummaryLabels.subtitles,
                    selected = state.selectedQuickPanel == PlayerQuickPanel.Subtitles,
                    enabled = trackAvailability.hasSubtitles,
                    disabledReason = if (trackAvailability.hasSubtitles) null else noSubtitlesLabel,
                    onClick = { onQuickPanel(PlayerQuickPanel.Subtitles) },
                    onUnsupported = onUnsupported,
                )
                QuickSettingPill(
                    icon = Icons.Filled.Speed,
                    label = speedLabel,
                    value = state.playbackSpeed.toSpeedLabel(),
                    selected = state.selectedQuickPanel == PlayerQuickPanel.Speed,
                    enabled = true,
                    disabledReason = null,
                    onClick = { onQuickPanel(PlayerQuickPanel.Speed) },
                    onUnsupported = onUnsupported,
                )
                QuickSettingPill(
                    icon = Icons.AutoMirrored.Filled.Chat,
                    label = danmakuLabel,
                    value = if (state.danmakuEnabled) onLabel else offLabel,
                    selected = state.selectedQuickPanel == PlayerQuickPanel.Danmaku,
                    enabled = true,
                    disabledReason = null,
                    onClick = { onQuickPanel(PlayerQuickPanel.Danmaku) },
                    onUnsupported = onUnsupported,
                )
            }
            TrackQuickPanel(
                state = state,
                onSelectTrack = onSelectTrack,
                onDisableSubtitles = onDisableSubtitles,
                onSelectSpeed = onSelectSpeed,
                onSetDanmakuEnabled = onSetDanmakuEnabled,
                onUpdateDanmaku = onUpdateDanmaku,
                firstOptionFocusRequester = quickPanelFocusRequester,
            )
            ProgressRail(state = state)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OsdIconButton(
                    icon = Icons.Filled.SkipPrevious,
                    label = previousLabel,
                    enabled = previousNavigation.enabled,
                    disabledReason = previousNavigation.disabledReason,
                    onClick = onPrevious,
                    onUnsupported = onUnsupported,
                )
                Spacer(modifier = Modifier.width(28.dp))
                OsdIconButton(icon = Icons.Filled.Replay10, label = replay10Label, onClick = onReplay10, large = true, onUnsupported = onUnsupported)
                Spacer(modifier = Modifier.width(34.dp))
                OsdIconButton(
                    icon = if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    label = if (state.isPlaying) pauseLabel else playLabel,
                    onClick = onPlayPause,
                    primary = true,
                    focusRequester = playFocusRequester,
                    onUnsupported = onUnsupported,
                )
                Spacer(modifier = Modifier.width(34.dp))
                OsdIconButton(icon = Icons.Filled.Forward10, label = forward10Label, onClick = onForward10, large = true, onUnsupported = onUnsupported)
                Spacer(modifier = Modifier.width(28.dp))
                OsdIconButton(
                    icon = Icons.Filled.SkipNext,
                    label = nextLabel,
                    enabled = nextNavigation.enabled,
                    disabledReason = nextNavigation.disabledReason,
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
}

@Composable
private fun PlaybackDetailsOverlay(
    overlayState: PlayerDetailOverlayState,
    modifier: Modifier = Modifier,
) {
    if (!overlayState.isLoading && overlayState.detail == null && overlayState.errorMessage == null) return
    val loadingLabel = stringResource(R.string.player_loading_details)
    GlassPanel(modifier = modifier.fillMaxWidth().widthIn(max = 420.dp), cornerRadius = 14.dp) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            when {
                overlayState.isLoading -> {
                    Text(loadingLabel, color = CinematicGlassColors.OnSurface, fontSize = 15.sp)
                }
                overlayState.errorMessage != null -> {
                    Text(overlayState.errorMessage, color = CinematicGlassColors.Error, fontSize = 15.sp)
                }
                overlayState.detail != null -> {
                    MediaDetailsSummary(overlayState.detail)
                }
            }
        }
    }
}

@Composable
private fun MediaDetailsSummary(detail: EmbyMediaDetail) {
    Text(
        text = detail.item.name,
        color = CinematicGlassColors.Primary,
        fontSize = 19.sp,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
    val facts = listOfNotNull(
        detail.item.productionYear?.toString(),
        detail.officialRating.toOfficialRatingLabel(stringResource(R.string.player_official_rating_label)),
        detail.communityRating?.let { stringResource(R.string.player_rating_label, it.toCommunityRatingLabel()) },
        detail.criticRating?.let { stringResource(R.string.player_critic_rating_label, it.toCriticRatingLabel()) },
        detail.genres.take(2).joinToString(" / ").takeIf { it.isNotBlank() },
    ).joinToString(" · ")
    if (facts.isNotBlank()) {
        Text(facts, color = CinematicGlassColors.OnSurfaceVariant, fontSize = 13.sp)
    }
    PlayerDetailProviderIdsLabelResolver.resolve(detail.providerIds)?.let { providerIds ->
        Text(providerIds, color = CinematicGlassColors.OnSurfaceVariant, fontSize = 12.sp)
    }
    detail.item.overview?.takeIf { it.isNotBlank() }?.let { overview ->
        Text(
            text = overview,
            color = CinematicGlassColors.OnSurface,
            fontSize = 13.sp,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis,
        )
    }
    val actorRoleFormat = stringResource(R.string.player_actor_role_format)
    PlayerDetailCastLabelResolver.resolve(
        people = detail.people,
        roleLabel = { name, role -> actorRoleFormat.format(name, role) },
    )?.let { cast ->
        Text(stringResource(R.string.player_actor_prefix, cast), color = CinematicGlassColors.OnSurfaceVariant, fontSize = 12.sp, maxLines = 1)
    }
}

@Composable
private fun TrackQuickPanel(
    state: PlayerOsdState,
    onSelectTrack: (PlayerTrackOption) -> Unit,
    onDisableSubtitles: () -> Unit,
    onSelectSpeed: (Float) -> Unit,
    onSetDanmakuEnabled: (Boolean) -> Unit,
    onUpdateDanmaku: (Float?, Float?, DanmakuDisplayArea?) -> Unit,
    firstOptionFocusRequester: FocusRequester,
) {
    val disableSubtitlesLabel = stringResource(R.string.player_disable_subtitles)
    val opacity60Label = stringResource(R.string.player_danmaku_opacity_60)
    val opacity100Label = stringResource(R.string.player_danmaku_opacity_100)
    val sizeSmallLabel = stringResource(R.string.player_danmaku_size_small)
    val sizeNormalLabel = stringResource(R.string.player_danmaku_size_normal)
    val sizeLargeLabel = stringResource(R.string.player_danmaku_size_large)
    val areaTopLabel = stringResource(R.string.player_danmaku_area_top)
    val areaFullLabel = stringResource(R.string.player_danmaku_area_full)
    val onLabel = stringResource(R.string.player_on)
    val offLabel = stringResource(R.string.player_off)
    val tracks = when (state.selectedQuickPanel) {
        PlayerQuickPanel.Audio -> state.audioTracks
        PlayerQuickPanel.Subtitles -> state.subtitleTracks
        else -> emptyList()
    }
    if (state.selectedQuickPanel == PlayerQuickPanel.Speed) {
        QuickTrackButtonRows(
            rows = PlayerQuickPanelLayoutPolicy.TvDefault.rowsFor(
                SupportedPlaybackSpeeds.map { speed ->
                    QuickTrackButtonAction(
                        label = speed.toSpeedLabel(),
                        selected = state.playbackSpeed == speed,
                        onClick = { onSelectSpeed(speed) },
                    )
                },
            ),
            firstOptionFocusRequester = firstOptionFocusRequester,
        )
        return
    }
    if (state.selectedQuickPanel == PlayerQuickPanel.Danmaku) {
        val actions = mapOf(
            DanmakuQuickOption.Enabled to QuickTrackButtonAction(
                label = onLabel,
                selected = state.danmakuEnabled,
                onClick = { onSetDanmakuEnabled(true) },
            ),
            DanmakuQuickOption.Disabled to QuickTrackButtonAction(
                label = offLabel,
                selected = !state.danmakuEnabled,
                onClick = { onSetDanmakuEnabled(false) },
            ),
            DanmakuQuickOption.Opacity60 to QuickTrackButtonAction(
                label = opacity60Label,
                selected = state.danmakuSettings.opacity <= 0.6f,
                onClick = { onUpdateDanmaku(0.6f, null, null) },
            ),
            DanmakuQuickOption.Opacity100 to QuickTrackButtonAction(
                label = opacity100Label,
                selected = state.danmakuSettings.opacity > 0.6f,
                onClick = { onUpdateDanmaku(1f, null, null) },
            ),
            DanmakuQuickOption.TextSmall to QuickTrackButtonAction(
                label = sizeSmallLabel,
                selected = state.danmakuSettings.textSizeScale < 1f,
                onClick = { onUpdateDanmaku(null, 0.9f, null) },
            ),
            DanmakuQuickOption.TextNormal to QuickTrackButtonAction(
                label = sizeNormalLabel,
                selected = state.danmakuSettings.textSizeScale in 1f..1.25f,
                onClick = { onUpdateDanmaku(null, 1.15f, null) },
            ),
            DanmakuQuickOption.TextLarge to QuickTrackButtonAction(
                label = sizeLargeLabel,
                selected = state.danmakuSettings.textSizeScale > 1.25f,
                onClick = { onUpdateDanmaku(null, 1.4f, null) },
            ),
            DanmakuQuickOption.AreaTop to QuickTrackButtonAction(
                label = areaTopLabel,
                selected = state.danmakuSettings.displayArea == DanmakuDisplayArea.Top,
                onClick = { onUpdateDanmaku(null, null, DanmakuDisplayArea.Top) },
            ),
            DanmakuQuickOption.AreaFull to QuickTrackButtonAction(
                label = areaFullLabel,
                selected = state.danmakuSettings.displayArea == DanmakuDisplayArea.Full,
                onClick = { onUpdateDanmaku(null, null, DanmakuDisplayArea.Full) },
            ),
        )
        QuickTrackButtonRows(
            rows = DanmakuQuickPanelLayoutPolicy.TvDefault.rows.map { row ->
                row.map { option -> actions.getValue(option) }
            },
            firstOptionFocusRequester = firstOptionFocusRequester,
        )
        return
    }
    if (tracks.isEmpty() && state.selectedQuickPanel != PlayerQuickPanel.Subtitles) return
    val trackActions = buildList {
        if (state.selectedQuickPanel == PlayerQuickPanel.Subtitles) {
            add(
                QuickTrackButtonAction(
                    label = disableSubtitlesLabel,
                    selected = state.subtitleDisabled,
                    onClick = onDisableSubtitles,
                ),
            )
        }
        tracks.forEach { track ->
            add(
                QuickTrackButtonAction(
                    label = track.label,
                    selected = track.selected,
                    onClick = { onSelectTrack(track) },
                ),
            )
        }
    }
    QuickTrackButtonRows(
        rows = PlayerQuickPanelLayoutPolicy.TvDefault.rowsFor(trackActions),
        firstOptionFocusRequester = firstOptionFocusRequester,
    )
}

private data class QuickTrackButtonAction(
    val label: String,
    val selected: Boolean,
    val onClick: () -> Unit,
)

@Composable
private fun QuickTrackButtonRows(
    rows: List<List<QuickTrackButtonAction>>,
    firstOptionFocusRequester: FocusRequester,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 156.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        rows.forEachIndexed { rowIndex, row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                row.forEachIndexed { actionIndex, action ->
                    QuickTrackButton(
                        label = action.label,
                        selected = action.selected,
                        onClick = action.onClick,
                        focusRequester = if (rowIndex == 0 && actionIndex == 0) {
                            firstOptionFocusRequester
                        } else {
                            null
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickTrackButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    focusRequester: FocusRequester? = null,
) {
    FocusableGlassSurface(
        modifier = focusRequester?.let { Modifier.focusRequester(it) } ?: Modifier,
        cornerRadius = 999.dp,
        onClick = onClick,
    ) { focused ->
        val visuals = OsdFocusVisualResolver.resolve(
            OsdFocusVisualState(
                focused = focused,
                selected = selected,
            ),
        )
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (visuals.selectedIndicator) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(CinematicGlassColors.Primary, RoundedCornerShape(999.dp)),
                )
            }
            Text(
                text = label,
                color = visuals.foregroundTone.toColor().copy(alpha = visuals.contentAlpha),
                fontSize = 13.sp,
                fontWeight = if (visuals.emphasizedLabel) FontWeight.Bold else FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
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
    ) { focused ->
        val visuals = OsdFocusVisualResolver.resolve(
            OsdFocusVisualState(
                focused = focused,
                selected = selected,
                enabled = enabled,
            ),
        )
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (visuals.selectedIndicator) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(34.dp)
                        .background(CinematicGlassColors.Primary, RoundedCornerShape(999.dp)),
                )
            }
            Icon(
                icon,
                contentDescription = null,
                tint = visuals.foregroundTone.toColor().copy(alpha = visuals.contentAlpha),
            )
            Column {
                Text(
                    text = label.uppercase(Locale.US),
                    color = CinematicGlassColors.OnSurfaceVariant.copy(alpha = visuals.contentAlpha),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = value,
                    color = visuals.foregroundTone.toColor().copy(alpha = visuals.contentAlpha),
                    fontSize = 14.sp,
                    fontWeight = if (visuals.emphasizedLabel) FontWeight.Bold else FontWeight.Normal,
                )
            }
        }
    }
}

@Composable
private fun ProgressRail(state: PlayerOsdState) {
    val remainingLabel = stringResource(
        R.string.player_remaining,
        (state.durationMs - state.positionMs).coerceAtLeast(0L).toClockLabel(),
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        state.seekPreview?.thumbnailUrl?.let { thumbnailUrl ->
            GlassPanel(modifier = Modifier.width(220.dp).height(124.dp), cornerRadius = 8.dp) {
                NetworkBackdropImage(
                    imageUrl = thumbnailUrl,
                    contentDescription = state.seekPreview.speedLabel,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(CinematicGlassSpacing.ProgressRailHeight)
                .background(Color.White.copy(alpha = 0.16f), RoundedCornerShape(999.dp)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(state.bufferedFraction.coerceAtLeast(state.progressFraction))
                    .height(CinematicGlassSpacing.ProgressRailHeight)
                    .background(Color.White.copy(alpha = 0.35f), RoundedCornerShape(999.dp)),
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(state.progressFraction)
                    .height(CinematicGlassSpacing.ProgressRailHeight)
                    .background(CinematicGlassColors.Primary, RoundedCornerShape(999.dp)),
            )
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(state.positionMs.toClockLabel(), color = CinematicGlassColors.OnSurfaceVariant, fontSize = 13.sp)
            Text(remainingLabel, color = CinematicGlassColors.OnSurface, fontSize = 13.sp)
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
    ) { focused ->
        val visuals = OsdFocusVisualResolver.resolve(
            OsdFocusVisualState(
                focused = focused,
                selected = primary,
                primary = primary,
                enabled = enabled,
            ),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    if (primary) {
                        CinematicGlassColors.Primary.copy(alpha = if (focused) 1f else 0.9f)
                    } else {
                        Color.Transparent
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = visuals.foregroundTone.toColor().copy(alpha = visuals.contentAlpha),
                modifier = Modifier.size(if (primary) 42.dp else 30.dp),
            )
        }
    }
}

@Composable
private fun OsdFocusTone.toColor(): Color =
    when (this) {
        OsdFocusTone.Primary -> CinematicGlassColors.Primary
        OsdFocusTone.OnPrimary -> CinematicGlassColors.OnPrimary
        OsdFocusTone.OnSurface -> CinematicGlassColors.OnSurface
        OsdFocusTone.OnSurfaceVariant -> CinematicGlassColors.OnSurfaceVariant
        OsdFocusTone.Disabled -> CinematicGlassColors.OnSurfaceVariant
    }

internal fun Long.toClockLabel(): String {
    val totalSeconds = (this / 1_000L).coerceAtLeast(0L)
    val hours = totalSeconds / 3_600L
    val minutes = (totalSeconds % 3_600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
}

internal fun Double.toCommunityRatingLabel(): String =
    String.format(Locale.US, "%.1f", this)

internal fun Double.toCriticRatingLabel(): String =
    String.format(Locale.US, "%.0f", this)

internal fun String?.toOfficialRatingLabel(format: String): String? =
    this?.takeIf { it.isNotBlank() }?.let { String.format(Locale.US, format, it.trim()) }

private fun Player.bufferedFraction(): Float {
    val duration = duration.takeIf { it > 0L } ?: return 0f
    return (bufferedPosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
}

private fun KeyEventType.toPlayerRemoteKeyEventType(): PlayerRemoteKeyEventType =
    when (this) {
        KeyEventType.KeyDown -> PlayerRemoteKeyEventType.Down
        KeyEventType.KeyUp -> PlayerRemoteKeyEventType.Up
        else -> PlayerRemoteKeyEventType.Other
    }

private fun Key.toPlayerRemoteKey(): PlayerRemoteKey =
    when (this) {
        Key.Back -> PlayerRemoteKey.Back
        Key.DirectionCenter -> PlayerRemoteKey.Center
        Key.Enter -> PlayerRemoteKey.Enter
        Key.NumPadEnter -> PlayerRemoteKey.NumPadEnter
        Key.DirectionUp -> PlayerRemoteKey.Up
        Key.DirectionDown -> PlayerRemoteKey.Down
        Key.DirectionLeft -> PlayerRemoteKey.Left
        Key.DirectionRight -> PlayerRemoteKey.Right
        else -> PlayerRemoteKey.Other
    }
