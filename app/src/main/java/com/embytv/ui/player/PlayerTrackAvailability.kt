package com.embytv.ui.player

import com.embytv.domain.model.PlaybackDetails

data class PlayerTrackAvailability(
    val hasAudio: Boolean,
    val hasSubtitles: Boolean,
)

object PlayerTrackAvailabilityResolver {
    fun resolve(
        state: PlayerOsdState,
        details: PlaybackDetails,
    ): PlayerTrackAvailability =
        PlayerTrackAvailability(
            hasAudio = state.audioTracks.isNotEmpty() || details.audioTracks.isNotEmpty(),
            hasSubtitles = state.subtitleTracks.isNotEmpty() || details.subtitleTracks.isNotEmpty(),
        )
}
