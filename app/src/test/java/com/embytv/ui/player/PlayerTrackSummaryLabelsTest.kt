package com.embytv.ui.player

import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import androidx.media3.common.TrackGroup
import com.embytv.domain.model.PlaybackDetails
import com.embytv.domain.model.PlaybackTrack
import com.embytv.domain.model.PlayerTrackOption
import com.embytv.domain.model.PlayerTrackType
import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerTrackSummaryLabelsTest {
    @Test
    fun selectedMedia3TracksOverridePlaybackInfoDefaultsForQuickPillValues() {
        val labels = PlayerTrackSummaryLabelResolver.resolve(
            state = PlayerOsdState(
                audioTracks = listOf(
                    playerTrackOption(
                        id = "audio:0",
                        label = "English AAC Stereo",
                        type = PlayerTrackType.Audio,
                        selected = false,
                    ),
                    playerTrackOption(
                        id = "audio:1",
                        label = "Japanese EAC3 5.1",
                        type = PlayerTrackType.Audio,
                        selected = true,
                    ),
                ),
                subtitleTracks = listOf(
                    playerTrackOption(
                        id = "subtitle:0",
                        label = "English SRT",
                        type = PlayerTrackType.Subtitle,
                        selected = true,
                    ),
                ),
            ),
            details = PlaybackDetails(
                audioTracks = listOf(
                    PlaybackTrack(index = 0, displayTitle = "English AAC Stereo", isDefault = true),
                ),
                subtitleTracks = listOf(
                    PlaybackTrack(index = 1, codec = "srt", language = "chi", isDefault = true, isExternal = true),
                ),
            ),
            noAudioLabel = "No audio tracks available",
            noSubtitlesLabel = "No subtitles available",
        )

        assertEquals("Japanese EAC3 5.1", labels.audio)
        assertEquals("English SRT", labels.subtitles)
    }

    @Test
    fun disabledSubtitlesUseLocalizedOffLabelInsteadOfPlaybackInfoDefault() {
        val labels = PlayerTrackSummaryLabelResolver.resolve(
            state = PlayerOsdState(
                subtitleDisabled = true,
                subtitleTracks = listOf(
                    playerTrackOption(
                        id = "subtitle:0",
                        label = "English SRT",
                        type = PlayerTrackType.Subtitle,
                        selected = false,
                    ),
                ),
            ),
            details = PlaybackDetails(
                subtitleTracks = listOf(
                    PlaybackTrack(index = 1, codec = "srt", language = "chi", isDefault = true, isExternal = true),
                ),
            ),
            noAudioLabel = "No audio tracks available",
            noSubtitlesLabel = "No subtitles available",
        )

        assertEquals("No audio tracks available", labels.audio)
        assertEquals("No subtitles available", labels.subtitles)
    }

    private fun playerTrackOption(
        id: String,
        label: String,
        type: PlayerTrackType,
        selected: Boolean,
    ): PlayerTrackOption =
        PlayerTrackOption(
            id = id,
            label = label,
            type = type,
            trackGroup = TrackGroup(
                id,
                Format.Builder()
                    .setId(id)
                    .setSampleMimeType(
                        if (type == PlayerTrackType.Audio) {
                            MimeTypes.AUDIO_AAC
                        } else {
                            MimeTypes.APPLICATION_SUBRIP
                        },
                    )
                    .build(),
            ),
            trackIndex = 0,
            selected = selected,
        )
}
