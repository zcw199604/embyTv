package com.embytv.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

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
        assertEquals("2160p · HDR10", details.qualityLabel)
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
}
