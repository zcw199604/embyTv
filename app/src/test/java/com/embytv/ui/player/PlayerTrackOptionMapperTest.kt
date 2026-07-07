package com.embytv.ui.player

import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import androidx.media3.common.TrackGroup
import androidx.media3.common.Tracks
import com.embytv.domain.model.PlayerTrackType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerTrackOptionMapperTest {
    @Test
    fun audioTrackWithoutLabelUsesFriendlyLanguageCodecAndChannelLabel() {
        val tracks = tracksOf(
            trackGroup = TrackGroup(
                "audio",
                Format.Builder()
                    .setId("audio-0")
                    .setSampleMimeType(MimeTypes.AUDIO_AAC)
                    .setLanguage("eng")
                    .setChannelCount(6)
                    .build(),
            ),
            selectedTracks = booleanArrayOf(true),
        )

        val options = tracks.toPlayerTrackOptions(C.TRACK_TYPE_AUDIO)

        assertEquals(1, options.size)
        assertEquals(PlayerTrackType.Audio, options[0].type)
        assertEquals("English AAC 5.1", options[0].label)
        assertTrue(options[0].selected)
    }

    @Test
    fun subtitleTrackWithoutLabelUsesFriendlyLanguageAndFormatLabel() {
        val tracks = tracksOf(
            trackGroup = TrackGroup(
                "subtitle",
                Format.Builder()
                    .setId("subtitle-0")
                    .setSampleMimeType(MimeTypes.APPLICATION_SUBRIP)
                    .setLanguage("chi")
                    .build(),
            ),
            selectedTracks = booleanArrayOf(false),
        )

        val options = tracks.toPlayerTrackOptions(C.TRACK_TYPE_TEXT)

        assertEquals(1, options.size)
        assertEquals(PlayerTrackType.Subtitle, options[0].type)
        assertEquals("Chinese (Simplified) SRT", options[0].label)
    }

    @Test
    fun subtitleTrackLanguageLabelNormalizesUnderscoreVariants() {
        val tracks = tracksOf(
            trackGroup = TrackGroup(
                "subtitle",
                Format.Builder()
                    .setId("subtitle-0")
                    .setSampleMimeType(MimeTypes.TEXT_VTT)
                    .setLanguage("zh_Hans")
                    .build(),
            ),
            selectedTracks = booleanArrayOf(false),
        )

        val options = tracks.toPlayerTrackOptions(C.TRACK_TYPE_TEXT)

        assertEquals("Chinese (Simplified) VTT", options[0].label)
    }

    @Test
    fun media3TrackLabelsAreTrimmedBeforeOsdDisplay() {
        val tracks = tracksOf(
            trackGroup = TrackGroup(
                "subtitle",
                Format.Builder()
                    .setId("subtitle-0")
                    .setLabel("  Chinese Commentary  ")
                    .setSampleMimeType(MimeTypes.APPLICATION_SUBRIP)
                    .build(),
            ),
            selectedTracks = booleanArrayOf(false),
        )

        val options = tracks.toPlayerTrackOptions(C.TRACK_TYPE_TEXT)

        assertEquals("Chinese Commentary", options[0].label)
    }

    @Test
    fun audioTrackLanguageLabelCollapsesEnglishRegionVariants() {
        val tracks = tracksOf(
            trackGroup = TrackGroup(
                "audio",
                Format.Builder()
                    .setId("audio-0")
                    .setSampleMimeType(MimeTypes.AUDIO_AAC)
                    .setLanguage("en_US")
                    .setChannelCount(2)
                    .build(),
            ),
            selectedTracks = booleanArrayOf(false),
        )

        val options = tracks.toPlayerTrackOptions(C.TRACK_TYPE_AUDIO)

        assertEquals("English AAC Stereo", options[0].label)
    }

    @Test
    fun unsupportedTracksAreHiddenFromOsdOptions() {
        val tracks = tracksOf(
            trackGroup = TrackGroup(
                "audio",
                Format.Builder()
                    .setId("audio-0")
                    .setLabel("Supported")
                    .setSampleMimeType(MimeTypes.AUDIO_AAC)
                    .build(),
                Format.Builder()
                    .setId("audio-1")
                    .setLabel("Unsupported")
                    .setSampleMimeType(MimeTypes.AUDIO_TRUEHD)
                    .build(),
            ),
            trackSupport = intArrayOf(C.FORMAT_HANDLED, C.FORMAT_UNSUPPORTED_TYPE),
            selectedTracks = booleanArrayOf(true, false),
        )

        val options = tracks.toPlayerTrackOptions(C.TRACK_TYPE_AUDIO)

        assertEquals(1, options.size)
        assertEquals("Supported", options[0].label)
        assertEquals(0, options[0].trackIndex)
    }

    private fun tracksOf(
        trackGroup: TrackGroup,
        trackSupport: IntArray = IntArray(trackGroup.length) { C.FORMAT_HANDLED },
        selectedTracks: BooleanArray,
    ): Tracks =
        Tracks(
            listOf(
                Tracks.Group(
                    trackGroup,
                    false,
                    trackSupport,
                    selectedTracks,
                ),
            ),
        )
}
