package com.embytv.ui.player

import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import androidx.media3.common.TrackGroup
import com.embytv.domain.model.EmbyMediaDetail
import com.embytv.domain.model.MediaItemSummary
import com.embytv.domain.model.PlaybackDetails
import com.embytv.domain.model.PlaybackVideoStream
import com.embytv.domain.model.PlayerTrackOption
import com.embytv.domain.model.PlayerTrackType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerOsdReducerTest {
    @Test
    fun backHidesVisibleOsdBeforeExit() {
        val state = PlayerOsdState(visible = true)

        val result = PlayerOsdReducer.reduce(state, PlayerOsdAction.BackPressed)

        assertFalse(result.state.visible)
        assertFalse(result.exitPlayer)
    }

    @Test
    fun backClosesOpenQuickPanelBeforeHidingOsd() {
        val state = PlayerOsdState(
            visible = true,
            selectedQuickPanel = PlayerQuickPanel.Subtitles,
            feedbackMessage = "Subtitles disabled",
            seekPreview = SeekPreviewState(
                targetPositionMs = 30_000L,
                deltaMs = 10_000L,
                speedLabel = "+10s",
            ),
        )

        val result = PlayerOsdReducer.reduce(state, PlayerOsdAction.BackPressed)

        assertTrue(result.state.visible)
        assertEquals(null, result.state.selectedQuickPanel)
        assertEquals(null, result.state.feedbackMessage)
        assertEquals(null, result.state.seekPreview)
        assertFalse(result.exitPlayer)
    }

    @Test
    fun backHidingVisibleOsdClearsTransientSeekPreview() {
        val state = PlayerOsdState(
            visible = true,
            seekPreview = SeekPreviewState(
                targetPositionMs = 30_000L,
                deltaMs = 10_000L,
                speedLabel = "+10s",
                thumbnailUrl = "thumb.jpg",
            ),
        )

        val result = PlayerOsdReducer.reduce(state, PlayerOsdAction.BackPressed)

        assertFalse(result.state.visible)
        assertEquals(null, result.state.seekPreview)
        assertFalse(result.exitPlayer)
    }

    @Test
    fun hidingOsdClearsTransientFeedback() {
        val state = PlayerOsdState(
            visible = true,
            feedbackMessage = "+10s",
        )

        val hidden = PlayerOsdReducer.reduce(state, PlayerOsdAction.Hide).state
        val backHidden = PlayerOsdReducer.reduce(state, PlayerOsdAction.BackPressed).state

        assertEquals(null, hidden.feedbackMessage)
        assertEquals(null, backHidden.feedbackMessage)
    }

    @Test
    fun backExitsWhenOsdAlreadyHidden() {
        val state = PlayerOsdState(visible = false)

        val result = PlayerOsdReducer.reduce(state, PlayerOsdAction.BackPressed)

        assertTrue(result.exitPlayer)
    }

    @Test
    fun interactionShowsOsd() {
        val state = PlayerOsdState(visible = false)

        val result = PlayerOsdReducer.reduce(state, PlayerOsdAction.UserInteraction)

        assertTrue(result.state.visible)
        assertEquals(1, result.state.interactionRevision)
    }

    @Test
    fun unsupportedActionsKeepOsdVisibleAndExposeFeedback() {
        val result = PlayerOsdReducer.reduce(
            PlayerOsdState(visible = true),
            PlayerOsdAction.UnsupportedAction("Audio 暂未支持"),
        )

        assertTrue(result.state.visible)
        assertEquals("Audio 暂未支持", result.state.feedbackMessage)
        assertFalse(result.exitPlayer)
    }

    @Test
    fun clearingFeedbackDoesNotHideOsd() {
        val result = PlayerOsdReducer.reduce(
            PlayerOsdState(visible = true, feedbackMessage = "暂未支持"),
            PlayerOsdAction.ClearFeedback,
        )

        assertTrue(result.state.visible)
        assertEquals(null, result.state.feedbackMessage)
    }

    @Test
    fun selectingCurrentQuickPanelClosesItWithoutHidingOsd() {
        val state = PlayerOsdState(
            visible = true,
            selectedQuickPanel = PlayerQuickPanel.Audio,
            feedbackMessage = "Audio",
        )

        val result = PlayerOsdReducer.reduce(
            state,
            PlayerOsdAction.SelectQuickPanel(PlayerQuickPanel.Audio),
        )

        assertTrue(result.state.visible)
        assertEquals(null, result.state.selectedQuickPanel)
        assertEquals(null, result.state.feedbackMessage)
    }

    @Test
    fun selectingDifferentQuickPanelKeepsTargetPanelOpen() {
        val state = PlayerOsdState(
            visible = true,
            selectedQuickPanel = PlayerQuickPanel.Audio,
        )

        val result = PlayerOsdReducer.reduce(
            state,
            PlayerOsdAction.SelectQuickPanel(PlayerQuickPanel.Subtitles),
        )

        assertTrue(result.state.visible)
        assertEquals(PlayerQuickPanel.Subtitles, result.state.selectedQuickPanel)
    }

    @Test
    fun selectingQuickPanelClearsSeekPreview() {
        val state = PlayerOsdState(
            visible = true,
            seekPreview = SeekPreviewState(
                targetPositionMs = 30_000L,
                deltaMs = 10_000L,
                speedLabel = "+10s",
                thumbnailUrl = "thumb.jpg",
            ),
        )

        val result = PlayerOsdReducer.reduce(
            state,
            PlayerOsdAction.SelectQuickPanel(PlayerQuickPanel.Speed),
        )

        assertEquals(PlayerQuickPanel.Speed, result.state.selectedQuickPanel)
        assertEquals(null, result.state.seekPreview)
    }

    @Test
    fun togglesPlaybackAndDanmakuState() {
        val paused = PlayerOsdReducer.reduce(
            PlayerOsdState(isPlaying = true, danmakuEnabled = true),
            PlayerOsdAction.TogglePlayPause,
        ).state
        val hiddenDanmaku = PlayerOsdReducer.reduce(paused, PlayerOsdAction.ToggleDanmaku).state

        assertFalse(paused.isPlaying)
        assertTrue(paused.danmakuPaused)
        assertEquals(PlaybackEngineStatus.Paused, paused.status)
        assertFalse(hiddenDanmaku.danmakuEnabled)
        assertTrue(hiddenDanmaku.danmakuPaused)
    }

    @Test
    fun explicitDanmakuSwitchKeepsSettingsPanelOpenWithoutAccidentalToggle() {
        val disabled = PlayerOsdReducer.reduce(
            PlayerOsdState(isPlaying = true, danmakuEnabled = true),
            PlayerOsdAction.SetDanmakuEnabled(false),
        ).state
        val disabledAgain = PlayerOsdReducer.reduce(
            disabled,
            PlayerOsdAction.SetDanmakuEnabled(false),
        ).state
        val enabled = PlayerOsdReducer.reduce(
            disabledAgain,
            PlayerOsdAction.SetDanmakuEnabled(true),
        ).state

        assertFalse(disabled.danmakuEnabled)
        assertTrue(disabled.danmakuPaused)
        assertEquals(PlayerQuickPanel.Danmaku, disabled.selectedQuickPanel)
        assertFalse(disabledAgain.danmakuEnabled)
        assertTrue(enabled.danmakuEnabled)
        assertFalse(enabled.danmakuPaused)
        assertEquals(PlayerQuickPanel.Danmaku, enabled.selectedQuickPanel)
    }

    @Test
    fun playbackStatusTracksLoadingBufferingPlayingPausedAndErrors() {
        val buffering = PlayerOsdReducer.reduce(
            PlayerOsdState(),
            PlayerOsdAction.PlaybackStatusChanged(PlaybackEngineStatus.Buffering),
        ).state
        val error = PlayerOsdReducer.reduce(
            buffering,
            PlayerOsdAction.PlaybackStatusChanged(PlaybackEngineStatus.Error("播放失败")),
        ).state

        assertEquals(PlaybackEngineStatus.Buffering, buffering.status)
        assertTrue(buffering.visible)
        assertEquals(PlaybackEngineStatus.Error("播放失败"), error.status)
        assertEquals("播放失败", error.feedbackMessage)
    }

    @Test
    fun playbackErrorClearsOpenPanelAndSeekPreview() {
        val state = PlayerOsdState(
            selectedQuickPanel = PlayerQuickPanel.Subtitles,
            seekPreview = SeekPreviewState(
                targetPositionMs = 30_000L,
                deltaMs = 10_000L,
                speedLabel = "+10s",
                thumbnailUrl = "thumb.jpg",
            ),
        )

        val error = PlayerOsdReducer.reduce(
            state,
            PlayerOsdAction.PlaybackStatusChanged(PlaybackEngineStatus.Error("播放失败")),
        ).state

        assertTrue(error.visible)
        assertEquals(null, error.selectedQuickPanel)
        assertEquals(null, error.seekPreview)
        assertEquals("播放失败", error.feedbackMessage)
    }

    @Test
    fun bufferingKeepsExistingPlaybackIntentToAvoidPausingPlayer() {
        val state = PlayerOsdReducer.reduce(
            PlayerOsdState(isPlaying = true, danmakuEnabled = true, danmakuPaused = false),
            PlayerOsdAction.PlaybackStatusChanged(PlaybackEngineStatus.Buffering),
        ).state

        assertEquals(PlaybackEngineStatus.Buffering, state.status)
        assertTrue(state.isPlaying)
        assertTrue(state.danmakuPaused)
        assertTrue(state.visible)
    }

    @Test
    fun playbackStatusTracksEndedAndShowsOsd() {
        val state = PlayerOsdReducer.reduce(
            PlayerOsdState(
                visible = false,
                isPlaying = true,
                danmakuEnabled = true,
                danmakuPaused = false,
            ),
            PlayerOsdAction.PlaybackStatusChanged(PlaybackEngineStatus.Ended),
        ).state

        assertEquals(PlaybackEngineStatus.Ended, state.status)
        assertFalse(state.isPlaying)
        assertTrue(state.visible)
        assertTrue(state.danmakuPaused)
    }

    @Test
    fun playbackEndedClearsOpenPanelSeekPreviewAndStaleFeedback() {
        val state = PlayerOsdState(
            visible = false,
            isPlaying = true,
            danmakuEnabled = true,
            danmakuPaused = false,
            selectedQuickPanel = PlayerQuickPanel.Speed,
            feedbackMessage = "+10s",
            seekPreview = SeekPreviewState(
                targetPositionMs = 30_000L,
                deltaMs = 10_000L,
                speedLabel = "+10s",
                thumbnailUrl = "thumb.jpg",
            ),
        )

        val ended = PlayerOsdReducer.reduce(
            state,
            PlayerOsdAction.PlaybackStatusChanged(PlaybackEngineStatus.Ended),
        ).state

        assertEquals(PlaybackEngineStatus.Ended, ended.status)
        assertTrue(ended.visible)
        assertEquals(null, ended.selectedQuickPanel)
        assertEquals(null, ended.seekPreview)
        assertEquals(null, ended.feedbackMessage)
    }

    @Test
    fun updatesProgress() {
        val state = PlayerOsdReducer.reduce(
            PlayerOsdState(),
            PlayerOsdAction.ProgressChanged(positionMs = 1_000, durationMs = 4_000, bufferedFraction = 0.5f),
        ).state

        assertEquals(1_000, state.positionMs)
        assertEquals(4_000, state.durationMs)
        assertEquals(0.25f, state.progressFraction)
        assertEquals(0.5f, state.bufferedFraction)
    }

    @Test
    fun keepsBufferedProgressAtLeastPlaybackProgress() {
        val state = PlayerOsdReducer.reduce(
            PlayerOsdState(),
            PlayerOsdAction.ProgressChanged(positionMs = 3_000, durationMs = 4_000, bufferedFraction = 0.25f),
        ).state

        assertEquals(0.75f, state.progressFraction)
        assertEquals(0.75f, state.bufferedFraction)
    }

    @Test
    fun clampsProgressPositionToDurationForStableTimelineLabels() {
        val state = PlayerOsdReducer.reduce(
            PlayerOsdState(),
            PlayerOsdAction.ProgressChanged(positionMs = 5_000, durationMs = 4_000, bufferedFraction = 1f),
        ).state

        assertEquals(4_000, state.positionMs)
        assertEquals(4_000, state.durationMs)
        assertEquals(1f, state.progressFraction)
        assertEquals(1f, state.bufferedFraction)
    }

    @Test
    fun clampsBufferedProgress() {
        val state = PlayerOsdReducer.reduce(
            PlayerOsdState(),
            PlayerOsdAction.ProgressChanged(positionMs = -1_000, durationMs = -4_000, bufferedFraction = 1.5f),
        ).state

        assertEquals(0, state.positionMs)
        assertEquals(0, state.durationMs)
        assertEquals(1f, state.bufferedFraction)
    }

    @Test
    fun resetsNonFiniteBufferedProgressToPlaybackProgressForStableUi() {
        listOf(Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY).forEach { bufferedFraction ->
            val state = PlayerOsdReducer.reduce(
                PlayerOsdState(bufferedFraction = 0.75f),
                PlayerOsdAction.ProgressChanged(
                    positionMs = 1_000,
                    durationMs = 4_000,
                    bufferedFraction = bufferedFraction,
                ),
            ).state

            assertEquals(0.25f, state.progressFraction)
            assertEquals(0.25f, state.bufferedFraction)
        }
    }

    @Test
    fun playbackSpeedSelectionClampsToSupportedValues() {
        val tooFast = PlayerOsdReducer.reduce(
            PlayerOsdState(),
            PlayerOsdAction.SelectPlaybackSpeed(3.25f, feedbackMessage = "Playback speed 2x"),
        ).state
        val supported = PlayerOsdReducer.reduce(
            tooFast,
            PlayerOsdAction.SelectPlaybackSpeed(1.5f, feedbackMessage = "Playback speed 1.5x"),
        ).state

        assertEquals(2.0f, tooFast.playbackSpeed)
        assertEquals(PlayerQuickPanel.Speed, tooFast.selectedQuickPanel)
        assertEquals("Playback speed 2x", tooFast.feedbackMessage)
        assertEquals(1.5f, supported.playbackSpeed)
        assertEquals("Playback speed 1.5x", supported.feedbackMessage)
    }

    @Test
    fun nonFinitePlaybackSpeedFallsBackToNormalSpeed() {
        listOf(Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY).forEach { speed ->
            val state = PlayerOsdReducer.reduce(
                PlayerOsdState(playbackSpeed = 1.5f),
                PlayerOsdAction.SelectPlaybackSpeed(speed),
            ).state

            assertEquals(1.0f, state.playbackSpeed)
        }
    }

    @Test
    fun playbackSpeedSyncWithoutFeedbackDoesNotRevealOsdOrOpenSpeedPanel() {
        val state = PlayerOsdState(
            visible = false,
            selectedQuickPanel = null,
            feedbackMessage = "Existing feedback",
            playbackSpeed = 1.0f,
        )

        val synced = PlayerOsdReducer.reduce(
            state,
            PlayerOsdAction.SelectPlaybackSpeed(1.5f),
        ).state

        assertFalse(synced.visible)
        assertEquals(null, synced.selectedQuickPanel)
        assertEquals("Existing feedback", synced.feedbackMessage)
        assertEquals(1.5f, synced.playbackSpeed)
    }

    @Test
    fun disablingSubtitlesClearsSelectedSubtitleOptionsImmediately() {
        val selectedSubtitle = subtitleTrack(selected = true)
        val state = PlayerOsdReducer.reduce(
            PlayerOsdState(subtitleTracks = listOf(selectedSubtitle)),
            PlayerOsdAction.DisableSubtitles(feedbackMessage = "Subtitles disabled"),
        ).state

        assertTrue(state.subtitleDisabled)
        assertTrue(state.subtitleTracks.isNotEmpty())
        assertTrue(state.subtitleTracks.none { it.selected })
        assertEquals("Subtitles disabled", state.feedbackMessage)
    }

    @Test
    fun staleTracksChangedAfterDisableDoesNotRestoreSelectedSubtitle() {
        val selectedSubtitle = subtitleTrack(selected = true)
        val disabled = PlayerOsdReducer.reduce(
            PlayerOsdState(subtitleTracks = listOf(selectedSubtitle)),
            PlayerOsdAction.DisableSubtitles(feedbackMessage = "Subtitles disabled"),
        ).state

        val state = PlayerOsdReducer.reduce(
            disabled,
            PlayerOsdAction.TracksChanged(
                audioTracks = emptyList(),
                subtitleTracks = listOf(selectedSubtitle),
            ),
        ).state

        assertTrue(state.subtitleDisabled)
        assertTrue(state.subtitleTracks.isNotEmpty())
        assertTrue(state.subtitleTracks.none { it.selected })
    }

    @Test
    fun selectingSubtitleAfterDisableClearsDisabledStateImmediately() {
        val subtitle = subtitleTrack(selected = false)
        val disabled = PlayerOsdReducer.reduce(
            PlayerOsdState(subtitleTracks = listOf(subtitle)),
            PlayerOsdAction.DisableSubtitles(feedbackMessage = "Subtitles disabled"),
        ).state

        val selected = PlayerOsdReducer.reduce(
            disabled,
            PlayerOsdAction.SelectTrack(subtitle),
        ).state

        assertFalse(selected.subtitleDisabled)
        assertEquals(PlayerQuickPanel.Subtitles, selected.selectedQuickPanel)
        assertTrue(selected.subtitleTracks.single().selected)
    }

    @Test
    fun selectingAudioTrackKeepsSubtitleDisabledState() {
        val audio = audioTrack(selected = false)
        val state = PlayerOsdReducer.reduce(
            PlayerOsdState(audioTracks = listOf(audio), subtitleDisabled = true),
            PlayerOsdAction.SelectTrack(audio),
        ).state

        assertTrue(state.subtitleDisabled)
        assertEquals(PlayerQuickPanel.Audio, state.selectedQuickPanel)
        assertTrue(state.audioTracks.single().selected)
    }

    @Test
    fun trackSelectionClearsSeekPreviewAndStaleFeedback() {
        val audio = audioTrack(selected = false)
        val state = PlayerOsdReducer.reduce(
            PlayerOsdState(
                audioTracks = listOf(audio),
                seekPreview = SeekPreviewState(
                    targetPositionMs = 30_000L,
                    deltaMs = 10_000L,
                    speedLabel = "+10s",
                    thumbnailUrl = "thumb.jpg",
                ),
                feedbackMessage = "+10s",
            ),
            PlayerOsdAction.SelectTrack(audio),
        ).state

        assertEquals(PlayerQuickPanel.Audio, state.selectedQuickPanel)
        assertEquals(null, state.seekPreview)
        assertEquals(null, state.feedbackMessage)
    }

    @Test
    fun subtitleDisableClearsSeekPreviewAndDoesNotKeepStaleFeedback() {
        val subtitle = subtitleTrack(selected = true)
        val state = PlayerOsdReducer.reduce(
            PlayerOsdState(
                subtitleTracks = listOf(subtitle),
                seekPreview = SeekPreviewState(
                    targetPositionMs = 30_000L,
                    deltaMs = 10_000L,
                    speedLabel = "+10s",
                ),
                feedbackMessage = "+10s",
            ),
            PlayerOsdAction.DisableSubtitles(),
        ).state

        assertEquals(PlayerQuickPanel.Subtitles, state.selectedQuickPanel)
        assertEquals(null, state.seekPreview)
        assertEquals(null, state.feedbackMessage)
    }

    @Test
    fun userPlaybackSpeedSelectionClearsSeekPreview() {
        val state = PlayerOsdReducer.reduce(
            PlayerOsdState(
                seekPreview = SeekPreviewState(
                    targetPositionMs = 30_000L,
                    deltaMs = 10_000L,
                    speedLabel = "+10s",
                ),
            ),
            PlayerOsdAction.SelectPlaybackSpeed(1.5f, feedbackMessage = "Playback speed 1.5x"),
        ).state

        assertEquals(PlayerQuickPanel.Speed, state.selectedQuickPanel)
        assertEquals(1.5f, state.playbackSpeed)
        assertEquals(null, state.seekPreview)
    }

    @Test
    fun danmakuSettingActionsClearSeekPreviewAndStaleFeedback() {
        val initial = PlayerOsdState(
            seekPreview = SeekPreviewState(
                targetPositionMs = 30_000L,
                deltaMs = 10_000L,
                speedLabel = "+10s",
            ),
            feedbackMessage = "+10s",
        )

        val enabled = PlayerOsdReducer.reduce(
            initial,
            PlayerOsdAction.SetDanmakuEnabled(false),
        ).state
        val configured = PlayerOsdReducer.reduce(
            initial,
            PlayerOsdAction.UpdateDanmakuSettings(opacity = 0.6f),
        ).state

        assertEquals(PlayerQuickPanel.Danmaku, enabled.selectedQuickPanel)
        assertEquals(null, enabled.seekPreview)
        assertEquals(null, enabled.feedbackMessage)
        assertEquals(PlayerQuickPanel.Danmaku, configured.selectedQuickPanel)
        assertEquals(null, configured.seekPreview)
        assertEquals(null, configured.feedbackMessage)
    }

    @Test
    fun seekPreviewClampsTargetAndShowsSpeedIntent() {
        val state = PlayerOsdReducer.reduce(
            PlayerOsdState(positionMs = 30_000, durationMs = 45_000),
            PlayerOsdAction.SeekPreviewRequested(deltaMs = 20_000, thumbnailUrl = "thumb.jpg"),
        ).state

        assertTrue(state.visible)
        assertEquals(45_000L, state.seekPreview?.targetPositionMs)
        assertEquals("+15s", state.seekPreview?.speedLabel)
        assertEquals("thumb.jpg", state.seekPreview?.thumbnailUrl)
    }

    @Test
    fun seekPreviewKeepsLastThumbnailWhenFollowUpSeekHasNoNewImage() {
        val first = PlayerOsdReducer.reduce(
            PlayerOsdState(positionMs = 10_000, durationMs = 60_000),
            PlayerOsdAction.SeekPreviewRequested(deltaMs = 10_000, thumbnailUrl = "thumb.jpg"),
        ).state
        val second = PlayerOsdReducer.reduce(
            first,
            PlayerOsdAction.SeekPreviewRequested(deltaMs = 10_000),
        ).state

        assertEquals("thumb.jpg", second.seekPreview?.thumbnailUrl)
        assertEquals(30_000L, second.seekPreview?.targetPositionMs)
    }

    @Test
    fun seekPreviewSaturatesWhenTargetWouldOverflowWithoutKnownDuration() {
        val state = PlayerOsdReducer.reduce(
            PlayerOsdState(positionMs = Long.MAX_VALUE - 5_000L, durationMs = 0L),
            PlayerOsdAction.SeekPreviewRequested(deltaMs = 10_000L),
        ).state

        assertEquals(Long.MAX_VALUE, state.seekPreview?.targetPositionMs)
    }

    @Test
    fun seekPreviewLabelDoesNotWrapWhenOriginIsOutsideNormalizedTimeline() {
        val state = PlayerOsdReducer.reduce(
            PlayerOsdState(positionMs = Long.MIN_VALUE, durationMs = 0L),
            PlayerOsdAction.SeekPreviewRequested(deltaMs = Long.MAX_VALUE),
        ).state

        assertEquals(0L, state.seekPreview?.targetPositionMs)
        assertEquals("+9223372036854775s", state.seekPreview?.speedLabel)
        assertEquals("+9223372036854775s", state.feedbackMessage)
    }

    @Test
    fun consecutiveSeekPreviewShowsCumulativeSeekIntent() {
        val first = PlayerOsdReducer.reduce(
            PlayerOsdState(positionMs = 10_000, durationMs = 60_000),
            PlayerOsdAction.SeekPreviewRequested(deltaMs = 10_000),
        ).state
        val second = PlayerOsdReducer.reduce(
            first,
            PlayerOsdAction.SeekPreviewRequested(deltaMs = 10_000),
        ).state
        val third = PlayerOsdReducer.reduce(
            second,
            PlayerOsdAction.SeekPreviewRequested(deltaMs = -10_000),
        ).state

        assertEquals("+10s", first.seekPreview?.speedLabel)
        assertEquals("+20s", second.seekPreview?.speedLabel)
        assertEquals("+10s", third.seekPreview?.speedLabel)
    }

    @Test
    fun committingSeekPreviewUpdatesProgressAndKeepsPreviewVisibleUntilOsdHides() {
        val preview = PlayerOsdReducer.reduce(
            PlayerOsdState(positionMs = 30_000, durationMs = 45_000),
            PlayerOsdAction.SeekPreviewRequested(deltaMs = -20_000),
        ).state
        val committed = PlayerOsdReducer.reduce(preview, PlayerOsdAction.SeekPreviewCommitted).state
        val hidden = PlayerOsdReducer.reduce(committed, PlayerOsdAction.Hide).state

        assertEquals(10_000L, committed.positionMs)
        assertEquals(10_000L, committed.seekPreview?.targetPositionMs)
        assertTrue(committed.visible)
        assertEquals(null, hidden.seekPreview)
    }

    @Test
    fun danmakuSettingsAreClampedForTvReadability() {
        val state = PlayerOsdReducer.reduce(
            PlayerOsdState(),
            PlayerOsdAction.UpdateDanmakuSettings(
                opacity = 0.1f,
                textSizeScale = 2.5f,
                displayArea = DanmakuDisplayArea.Top,
            ),
        ).state

        assertEquals(0.2f, state.danmakuSettings.opacity)
        assertEquals(1.6f, state.danmakuSettings.textSizeScale)
        assertEquals(DanmakuDisplayArea.Top, state.danmakuSettings.displayArea)
        assertEquals(PlayerQuickPanel.Danmaku, state.selectedQuickPanel)
    }

    @Test
    fun detailOverlayTransitionsBetweenLoadingLoadedAndFailed() {
        val loading = PlayerOsdReducer.reduce(
            PlayerOsdState(),
            PlayerOsdAction.DetailOverlayLoading("item-1"),
        ).state
        val failed = PlayerOsdReducer.reduce(
            loading,
            PlayerOsdAction.DetailOverlayFailed("item-1", "详情加载失败"),
        ).state

        assertEquals("item-1", loading.detailOverlay.itemId)
        assertTrue(loading.detailOverlay.isLoading)
        assertEquals("item-1", failed.detailOverlay.itemId)
        assertEquals("详情加载失败", failed.detailOverlay.errorMessage)
        assertFalse(failed.detailOverlay.isLoading)
    }

    @Test
    fun detailOverlayLoadedKeepsFreshPlaybackInfoForOsdTechnicalLabels() {
        val detail = mediaDetail()
        val playbackDetails = PlaybackDetails(
            container = "mp4",
            bitrate = 8_000_000,
            video = PlaybackVideoStream(codec = "h264", height = 1080),
        )

        val loaded = PlayerOsdReducer.reduce(
            PlayerOsdState(),
            PlayerOsdAction.DetailOverlayLoaded("item-1", detail, playbackDetails),
        ).state

        assertEquals("item-1", loaded.detailOverlay.itemId)
        assertEquals(detail, loaded.detailOverlay.detail)
        assertEquals(playbackDetails, loaded.detailOverlay.playbackDetails)
        assertFalse(loaded.detailOverlay.isLoading)
    }

    @Test
    fun detailOverlayFailureAlsoShowsRemoteFeedback() {
        val failed = PlayerOsdReducer.reduce(
            PlayerOsdState(feedbackMessage = null),
            PlayerOsdAction.DetailOverlayFailed("item-1", "详情加载失败"),
        ).state

        assertEquals("item-1", failed.detailOverlay.itemId)
        assertEquals("详情加载失败", failed.detailOverlay.errorMessage)
        assertEquals("详情加载失败", failed.feedbackMessage)
    }

    @Test
    fun detailOverlayIgnoresLoadedResultFromPreviousPlaybackItem() {
        val currentLoading = PlayerOsdReducer.reduce(
            PlayerOsdState(),
            PlayerOsdAction.DetailOverlayLoading("item-2"),
        ).state
        val staleLoaded = PlayerOsdReducer.reduce(
            currentLoading,
            PlayerOsdAction.DetailOverlayLoaded("item-1", mediaDetail(), PlaybackDetails(container = "mp4")),
        ).state

        assertEquals("item-2", staleLoaded.detailOverlay.itemId)
        assertTrue(staleLoaded.detailOverlay.isLoading)
        assertEquals(null, staleLoaded.detailOverlay.detail)
        assertEquals(null, staleLoaded.detailOverlay.playbackDetails)
    }

    @Test
    fun detailOverlayIgnoresFailureFromPreviousPlaybackItem() {
        val currentLoading = PlayerOsdReducer.reduce(
            PlayerOsdState(feedbackMessage = null),
            PlayerOsdAction.DetailOverlayLoading("item-2"),
        ).state
        val staleFailed = PlayerOsdReducer.reduce(
            currentLoading,
            PlayerOsdAction.DetailOverlayFailed("item-1", "旧详情加载失败"),
        ).state

        assertEquals("item-2", staleFailed.detailOverlay.itemId)
        assertTrue(staleFailed.detailOverlay.isLoading)
        assertEquals(null, staleFailed.detailOverlay.errorMessage)
        assertEquals(null, staleFailed.feedbackMessage)
    }

    private fun subtitleTrack(selected: Boolean): PlayerTrackOption =
        PlayerTrackOption(
            id = "subtitle:0",
            label = "Chinese (Simplified)",
            type = PlayerTrackType.Subtitle,
            trackGroup = TrackGroup(
                "subtitle",
                Format.Builder()
                    .setId("subtitle-0")
                    .setSampleMimeType(MimeTypes.APPLICATION_SUBRIP)
                    .build(),
            ),
            trackIndex = 0,
            selected = selected,
        )

    private fun audioTrack(selected: Boolean): PlayerTrackOption =
        PlayerTrackOption(
            id = "audio:0",
            label = "English AAC",
            type = PlayerTrackType.Audio,
            trackGroup = TrackGroup(
                "audio",
                Format.Builder()
                    .setId("audio-0")
                    .setSampleMimeType(MimeTypes.AUDIO_AAC)
                    .build(),
            ),
            trackIndex = 0,
            selected = selected,
        )

    private fun mediaDetail(): EmbyMediaDetail =
        EmbyMediaDetail(
            item = MediaItemSummary(
                id = "movie-1",
                name = "Movie",
                type = "Movie",
                overview = null,
                imageUrl = null,
            ),
            people = emptyList(),
            genres = emptyList(),
            studios = emptyList(),
            communityRating = null,
            officialRating = null,
            premiereDate = null,
        )
}
