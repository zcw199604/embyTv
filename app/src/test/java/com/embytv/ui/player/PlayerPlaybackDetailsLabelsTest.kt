package com.embytv.ui.player

import com.embytv.domain.model.PlaybackDetails
import com.embytv.domain.model.PlaybackTrack
import com.embytv.domain.model.PlaybackVideoStream
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerPlaybackDetailsLabelsTest {
    @Test
    fun missingDetailsUseCallerProvidedLocalizedFallbacks() {
        val labels = PlayerPlaybackDetailsLabelResolver.resolve(
            details = PlaybackDetails(),
            directPlayLabel = "Direct Play",
            unknownQualityLabel = "Unknown quality",
            noAudioLabel = "No audio tracks available",
            noSubtitlesLabel = "No subtitles available",
        )

        assertEquals("Direct Play", labels.summary)
        assertEquals("Unknown quality", labels.quality)
        assertEquals("No audio tracks available", labels.audio)
        assertEquals("No subtitles available", labels.subtitles)
    }

    @Test
    fun realDetailsStillUseMediaMetadataLabels() {
        val labels = PlayerPlaybackDetailsLabelResolver.resolve(
            details = PlaybackDetails(
                container = "mkv",
                bitrate = 4_029_281,
                video = PlaybackVideoStream(
                    codec = "hevc",
                    height = 2160,
                    videoRange = "HDR10",
                ),
                audioTracks = listOf(
                    PlaybackTrack(index = 1, codec = "eac3", displayTitle = "EAC3 5.1", isDefault = true),
                ),
                subtitleTracks = listOf(
                    PlaybackTrack(index = 2, codec = "srt", language = "chi", isExternal = true),
                ),
            ),
            directPlayLabel = "Direct Play",
            unknownQualityLabel = "Unknown quality",
            noAudioLabel = "No audio tracks available",
            noSubtitlesLabel = "No subtitles available",
        )

        assertEquals("Direct Play · MKV · HEVC", labels.summary)
        assertEquals("2160p · HDR10 · 4.0 Mbps", labels.quality)
        assertEquals("EAC3 5.1", labels.audio)
        assertEquals("Chinese (Simplified) · SRT · External", labels.subtitles)
    }

    @Test
    fun playbackSummaryUsesCallerProvidedLocalizedPlaybackModeLabel() {
        val labels = PlayerPlaybackDetailsLabelResolver.resolve(
            details = PlaybackDetails(
                container = "mp4",
                video = PlaybackVideoStream(codec = "h264"),
            ),
            directPlayLabel = "直接播放",
            unknownQualityLabel = "未知画质",
            noAudioLabel = "没有可用音轨信息",
            noSubtitlesLabel = "当前媒体没有字幕",
        )

        assertEquals("直接播放 · MP4 · H264", labels.summary)
    }

    @Test
    fun bitrateLabelUsesStableTechnicalDecimalSeparatorAcrossLocales() {
        val previousLocale = Locale.getDefault()
        try {
            Locale.setDefault(Locale.FRANCE)

            val labels = PlayerPlaybackDetailsLabelResolver.resolve(
                details = PlaybackDetails(
                    bitrate = 4_029_281,
                    video = PlaybackVideoStream(height = 2160),
                ),
                directPlayLabel = "Direct Play",
                unknownQualityLabel = "Unknown quality",
                noAudioLabel = "No audio tracks available",
                noSubtitlesLabel = "No subtitles available",
            )

            assertEquals("2160p · 4.0 Mbps", labels.quality)
        } finally {
            Locale.setDefault(previousLocale)
        }
    }
}
