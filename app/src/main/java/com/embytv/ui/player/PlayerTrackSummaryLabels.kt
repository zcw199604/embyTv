package com.embytv.ui.player

import com.embytv.domain.model.PlaybackDetails

data class PlayerTrackSummaryLabels(
    val audio: String,
    val subtitles: String,
)

object PlayerTrackSummaryLabelResolver {
    fun resolve(
        state: PlayerOsdState,
        details: PlaybackDetails,
        noAudioLabel: String,
        noSubtitlesLabel: String,
    ): PlayerTrackSummaryLabels =
        PlayerTrackSummaryLabels(
            audio = state.audioTracks.firstOrNull { it.selected }?.label
                ?: details.audioTracks.firstOrNull { it.isDefault }?.label
                ?: details.audioTracks.firstOrNull()?.label
                ?: noAudioLabel,
            subtitles = if (state.subtitleDisabled) {
                noSubtitlesLabel
            } else {
                state.subtitleTracks.firstOrNull { it.selected }?.label
                    ?: details.subtitleTracks.firstOrNull { it.isDefault }?.label
                    ?: details.subtitleTracks.firstOrNull()?.label
                    ?: noSubtitlesLabel
            },
        )
}
