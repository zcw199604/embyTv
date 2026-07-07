package com.embytv.ui.player

import com.embytv.domain.model.PlaybackDetails
import com.embytv.domain.model.PlaybackTrack
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerTrackAvailabilityTest {
    @Test
    fun embyPlaybackInfoKeepsSubtitleEntryAvailableBeforeMedia3TracksArrive() {
        val availability = PlayerTrackAvailabilityResolver.resolve(
            state = PlayerOsdState(subtitleTracks = emptyList()),
            details = PlaybackDetails(
                subtitleTracks = listOf(
                    PlaybackTrack(
                        index = 2,
                        codec = "srt",
                        language = "chi",
                        isExternal = true,
                    ),
                ),
            ),
        )

        assertTrue(availability.hasSubtitles)
    }

    @Test
    fun noMedia3OrEmbyTracksKeepsTrackEntriesDisabled() {
        val availability = PlayerTrackAvailabilityResolver.resolve(
            state = PlayerOsdState(),
            details = PlaybackDetails(),
        )

        assertFalse(availability.hasAudio)
        assertFalse(availability.hasSubtitles)
    }
}
