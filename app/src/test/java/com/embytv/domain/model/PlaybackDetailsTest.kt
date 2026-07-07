package com.embytv.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.util.Locale

class PlaybackDetailsTest {
    @Test
    fun buildsQualityLabelsFromRealMediaStreams() {
        val details = PlaybackDetails(
            playSessionId = "play-session",
            mediaSourceId = "media-source",
            container = "mkv",
            bitrate = 4_029_281,
            video = PlaybackVideoStream(
                codec = "hevc",
                width = 3840,
                height = 2160,
                videoRange = "HDR10",
            ),
            audioTracks = listOf(
                PlaybackTrack(index = 1, codec = "eac3", displayTitle = "EAC3 5.1", channels = 6, isDefault = true),
            ),
            subtitleTracks = listOf(
                PlaybackTrack(index = 2, codec = "srt", displayTitle = "简体中文", isDefault = true),
            ),
        )

        assertEquals("Direct Play · MKV · HEVC", details.playbackSummaryLabel)
        assertEquals("2160p · HDR10 · 4.0 Mbps", details.qualityLabel)
        assertEquals("EAC3 5.1", details.audioLabel)
        assertEquals("简体中文", details.subtitleLabel)
    }

    @Test
    fun doesNotFallBackToFakeHdrWhenStreamsAreMissing() {
        val details = PlaybackDetails()

        assertEquals("Direct Play", details.playbackSummaryLabel)
        assertEquals("未知画质", details.qualityLabel)
        assertEquals("无音轨信息", details.audioLabel)
        assertEquals("无字幕", details.subtitleLabel)
        assertFalse(details.playbackSummaryLabel.contains("HEVC"))
        assertFalse(details.qualityLabel.contains("HDR10"))
    }

    @Test
    fun externalSubtitleLabelNormalizesUnderscoreLanguageCodes() {
        val track = PlaybackTrack(
            index = 2,
            codec = "srt",
            language = "zh_Hans",
            isExternal = true,
        )

        assertEquals("Chinese (Simplified) · SRT · External", track.label)
    }

    @Test
    fun externalSubtitleLabelCollapsesEnglishRegionVariants() {
        val track = PlaybackTrack(
            index = 3,
            codec = "vtt",
            language = "en_US",
            isExternal = true,
        )

        assertEquals("English · VTT · External", track.label)
    }

    @Test
    fun externalSubtitleLabelUsesFriendlyFormatForEmbySubtitleCodecs() {
        val subrip = PlaybackTrack(
            index = 4,
            codec = "subrip",
            language = "chi",
            isExternal = true,
        )
        val webvtt = PlaybackTrack(
            index = 5,
            codec = "webvtt",
            language = "eng",
            isExternal = true,
        )

        assertEquals("Chinese (Simplified) · SRT · External", subrip.label)
        assertEquals("English · VTT · External", webvtt.label)
    }

    @Test
    fun externalSubtitleLabelFallsBackToDeliveryUrlExtensionWhenCodecIsMissing() {
        val vtt = PlaybackTrack(
            index = 6,
            codec = null,
            language = "zh_CN",
            isExternal = true,
            externalUrl = "http://emby.test/Videos/movie-1/Subtitles/6/Stream.vtt?api_key=token",
        )
        val ass = PlaybackTrack(
            index = 7,
            codec = "",
            language = "eng",
            isExternal = true,
            externalUrl = "/Videos/movie-1/Subtitles/7/Stream.ass?api_key=token",
        )

        assertEquals("Chinese (Simplified) · VTT · External", vtt.label)
        assertEquals("English · ASS · External", ass.label)
    }

    @Test
    fun playbackTrackDisplayTitlesAreTrimmedBeforeOsdLabels() {
        val audio = PlaybackTrack(
            index = 1,
            codec = "eac3",
            displayTitle = "  English EAC3 5.1  ",
        )
        val externalSubtitle = PlaybackTrack(
            index = 2,
            codec = "srt",
            displayTitle = "  Commentary  ",
            isExternal = true,
        )

        assertEquals("English EAC3 5.1", audio.label)
        assertEquals("Commentary · SRT · External", externalSubtitle.label)
    }

    @Test
    fun bitrateLabelsUseStableTechnicalDecimalSeparatorAcrossLocales() {
        val previousLocale = Locale.getDefault()
        try {
            Locale.setDefault(Locale.FRANCE)
            val details = PlaybackDetails(
                bitrate = 4_029_281,
                video = PlaybackVideoStream(height = 2160),
            )

            assertEquals("4.0 Mbps", details.bitrateLabel)
            assertEquals("2160p · 4.0 Mbps", details.qualityLabel)
        } finally {
            Locale.setDefault(previousLocale)
        }
    }

    @Test
    fun technicalCodecLabelsUseStableAsciiUppercaseAcrossLocales() {
        val previousLocale = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"))
            val details = PlaybackDetails(
                container = "mkv",
                video = PlaybackVideoStream(codec = "divx", height = 720),
                audioTracks = listOf(PlaybackTrack(index = 1, codec = "dts")),
                subtitleTracks = listOf(PlaybackTrack(index = 2, codec = "idx", language = "eng", isExternal = true)),
            )

            assertEquals("Direct Play · MKV · DIVX", details.playbackSummaryLabel)
            assertEquals("DTS", details.audioLabel)
            assertEquals("English · IDX · External", details.subtitleLabel)
        } finally {
            Locale.setDefault(previousLocale)
        }
    }
}
