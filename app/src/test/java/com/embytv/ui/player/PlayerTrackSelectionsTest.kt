package com.embytv.ui.player

import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import androidx.media3.common.TrackGroup
import androidx.media3.common.TrackSelectionParameters
import com.embytv.domain.model.PlayerTrackOption
import com.embytv.domain.model.PlayerTrackType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerTrackSelectionsTest {
    @Test
    fun selectingAudioTrackSetsAnAudioOverrideWithoutDisablingTextTracks() {
        val option = playerTrackOption(
            type = PlayerTrackType.Audio,
            trackGroup = trackGroup(MimeTypes.AUDIO_AAC, "audio"),
            trackIndex = 1,
        )

        val parameters = TrackSelectionParameters.Builder()
            .build()
            .selectTrack(option)

        val override = parameters.overrides[option.trackGroup]
        assertEquals(listOf(1), override?.trackIndices)
        assertFalse(parameters.disabledTrackTypes.contains(C.TRACK_TYPE_TEXT))
    }

    @Test
    fun disablingSubtitlesDisablesTextTrackType() {
        val parameters = TrackSelectionParameters.Builder()
            .build()
            .disableSubtitles()

        assertTrue(parameters.disabledTrackTypes.contains(C.TRACK_TYPE_TEXT))
    }

    @Test
    fun disablingSubtitlesClearsExistingTextTrackOverride() {
        val option = playerTrackOption(
            type = PlayerTrackType.Subtitle,
            trackGroup = trackGroup(MimeTypes.APPLICATION_SUBRIP, "subtitle"),
            trackIndex = 1,
        )

        val parameters = TrackSelectionParameters.Builder()
            .build()
            .selectTrack(option)
            .disableSubtitles()

        assertTrue(parameters.disabledTrackTypes.contains(C.TRACK_TYPE_TEXT))
        assertFalse(parameters.overrides.containsKey(option.trackGroup))
    }

    @Test
    fun selectingSubtitleTrackAfterDisableReEnablesTextAndSetsSubtitleOverride() {
        val option = playerTrackOption(
            type = PlayerTrackType.Subtitle,
            trackGroup = trackGroup(MimeTypes.APPLICATION_SUBRIP, "subtitle"),
            trackIndex = 0,
        )

        val parameters = TrackSelectionParameters.Builder()
            .build()
            .disableSubtitles()
            .selectTrack(option)

        val override = parameters.overrides[option.trackGroup]
        assertFalse(parameters.disabledTrackTypes.contains(C.TRACK_TYPE_TEXT))
        assertEquals(listOf(0), override?.trackIndices)
    }

    @Test
    fun selectingAudioTrackPreservesExistingSubtitleOverride() {
        val subtitle = playerTrackOption(
            type = PlayerTrackType.Subtitle,
            trackGroup = trackGroup(MimeTypes.APPLICATION_SUBRIP, "subtitle"),
            trackIndex = 1,
        )
        val audio = playerTrackOption(
            type = PlayerTrackType.Audio,
            trackGroup = trackGroup(MimeTypes.AUDIO_AAC, "audio"),
            trackIndex = 0,
        )

        val parameters = TrackSelectionParameters.Builder()
            .build()
            .selectTrack(subtitle)
            .selectTrack(audio)

        assertEquals(listOf(0), parameters.overrides[audio.trackGroup]?.trackIndices)
        assertEquals(listOf(1), parameters.overrides[subtitle.trackGroup]?.trackIndices)
        assertFalse(parameters.disabledTrackTypes.contains(C.TRACK_TYPE_TEXT))
    }

    @Test
    fun selectingSubtitleTrackPreservesExistingAudioOverride() {
        val audio = playerTrackOption(
            type = PlayerTrackType.Audio,
            trackGroup = trackGroup(MimeTypes.AUDIO_AAC, "audio"),
            trackIndex = 1,
        )
        val subtitle = playerTrackOption(
            type = PlayerTrackType.Subtitle,
            trackGroup = trackGroup(MimeTypes.TEXT_VTT, "subtitle"),
            trackIndex = 0,
        )

        val parameters = TrackSelectionParameters.Builder()
            .build()
            .selectTrack(audio)
            .selectTrack(subtitle)

        assertEquals(listOf(1), parameters.overrides[audio.trackGroup]?.trackIndices)
        assertEquals(listOf(0), parameters.overrides[subtitle.trackGroup]?.trackIndices)
        assertFalse(parameters.disabledTrackTypes.contains(C.TRACK_TYPE_TEXT))
    }

    @Test
    fun selectingDifferentAudioTrackReplacesPreviousAudioOverride() {
        val firstAudio = playerTrackOption(
            type = PlayerTrackType.Audio,
            trackGroup = trackGroup(MimeTypes.AUDIO_AAC, "audio-a"),
            trackIndex = 1,
        )
        val secondAudio = playerTrackOption(
            type = PlayerTrackType.Audio,
            trackGroup = trackGroup(MimeTypes.AUDIO_AC3, "audio-b"),
            trackIndex = 0,
        )

        val parameters = TrackSelectionParameters.Builder()
            .build()
            .selectTrack(firstAudio)
            .selectTrack(secondAudio)

        assertFalse(parameters.overrides.containsKey(firstAudio.trackGroup))
        assertEquals(listOf(0), parameters.overrides[secondAudio.trackGroup]?.trackIndices)
    }

    @Test
    fun selectingDifferentSubtitleTrackReplacesPreviousSubtitleOverrideAndKeepsTextEnabled() {
        val firstSubtitle = playerTrackOption(
            type = PlayerTrackType.Subtitle,
            trackGroup = trackGroup(MimeTypes.APPLICATION_SUBRIP, "subtitle-a"),
            trackIndex = 1,
        )
        val secondSubtitle = playerTrackOption(
            type = PlayerTrackType.Subtitle,
            trackGroup = trackGroup(MimeTypes.TEXT_VTT, "subtitle-b"),
            trackIndex = 0,
        )

        val parameters = TrackSelectionParameters.Builder()
            .build()
            .selectTrack(firstSubtitle)
            .selectTrack(secondSubtitle)

        assertFalse(parameters.overrides.containsKey(firstSubtitle.trackGroup))
        assertEquals(listOf(0), parameters.overrides[secondSubtitle.trackGroup]?.trackIndices)
        assertFalse(parameters.disabledTrackTypes.contains(C.TRACK_TYPE_TEXT))
    }

    @Test
    fun selectingAudioTrackAfterSubtitlesDisabledKeepsTextTrackTypeDisabled() {
        val audio = playerTrackOption(
            type = PlayerTrackType.Audio,
            trackGroup = trackGroup(MimeTypes.AUDIO_AAC, "audio"),
            trackIndex = 0,
        )

        val parameters = TrackSelectionParameters.Builder()
            .build()
            .disableSubtitles()
            .selectTrack(audio)

        assertEquals(listOf(0), parameters.overrides[audio.trackGroup]?.trackIndices)
        assertTrue(parameters.disabledTrackTypes.contains(C.TRACK_TYPE_TEXT))
    }

    @Test
    fun disablingSubtitlesPreservesExistingAudioOverride() {
        val audio = playerTrackOption(
            type = PlayerTrackType.Audio,
            trackGroup = trackGroup(MimeTypes.AUDIO_AAC, "audio"),
            trackIndex = 1,
        )
        val subtitle = playerTrackOption(
            type = PlayerTrackType.Subtitle,
            trackGroup = trackGroup(MimeTypes.APPLICATION_SUBRIP, "subtitle"),
            trackIndex = 0,
        )

        val parameters = TrackSelectionParameters.Builder()
            .build()
            .selectTrack(audio)
            .selectTrack(subtitle)
            .disableSubtitles()

        assertEquals(listOf(1), parameters.overrides[audio.trackGroup]?.trackIndices)
        assertFalse(parameters.overrides.containsKey(subtitle.trackGroup))
        assertTrue(parameters.disabledTrackTypes.contains(C.TRACK_TYPE_TEXT))
    }

    private fun playerTrackOption(
        type: PlayerTrackType,
        trackGroup: TrackGroup,
        trackIndex: Int,
    ): PlayerTrackOption =
        PlayerTrackOption(
            id = "${trackGroup.id}:$trackIndex",
            label = "Track ${trackIndex + 1}",
            type = type,
            trackGroup = trackGroup,
            trackIndex = trackIndex,
        )

    private fun trackGroup(sampleMimeType: String, id: String): TrackGroup =
        TrackGroup(
            id,
            Format.Builder()
                .setId("$id-0")
                .setSampleMimeType(sampleMimeType)
                .build(),
            Format.Builder()
                .setId("$id-1")
                .setSampleMimeType(sampleMimeType)
                .build(),
        )
}
