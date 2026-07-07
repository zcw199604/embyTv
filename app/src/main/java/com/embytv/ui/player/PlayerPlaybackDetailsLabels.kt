package com.embytv.ui.player

import com.embytv.domain.model.PlaybackDetails
import java.util.Locale

data class PlayerPlaybackDetailsLabels(
    val summary: String,
    val quality: String,
    val audio: String,
    val subtitles: String,
)

object PlayerPlaybackDetailsLabelResolver {
    fun resolve(
        details: PlaybackDetails,
        directPlayLabel: String,
        unknownQualityLabel: String,
        noAudioLabel: String,
        noSubtitlesLabel: String,
    ): PlayerPlaybackDetailsLabels =
        PlayerPlaybackDetailsLabels(
            summary = details.localizedPlaybackSummaryLabel(directPlayLabel),
            quality = details.localizedQualityLabel(unknownQualityLabel),
            audio = details.audioTracks.firstOrNull { it.isDefault }?.label
                ?: details.audioTracks.firstOrNull()?.label
                ?: noAudioLabel,
            subtitles = details.subtitleTracks.firstOrNull { it.isDefault }?.label
                ?: details.subtitleTracks.firstOrNull()?.label
                ?: noSubtitlesLabel,
        )
}

private fun PlaybackDetails.localizedPlaybackSummaryLabel(directPlayLabel: String): String =
    listOfNotNull(
        directPlayLabel,
        container?.uppercase(Locale.US),
        video?.codec?.uppercase(Locale.US),
    ).joinToString(" · ")

private fun PlaybackDetails.localizedQualityLabel(unknownQualityLabel: String): String {
    val height = video?.height?.takeIf { it > 0 }?.let { "${it}p" }
    val range = video?.videoRange?.takeIf { it.isNotBlank() && !it.equals("SDR", ignoreCase = true) }
    return listOfNotNull(height, range, bitrate.toMegabitsLabel())
        .joinToString(" · ")
        .ifBlank { unknownQualityLabel }
}

private fun Int?.toMegabitsLabel(): String? =
    this?.takeIf { it > 0 }?.let { String.format(Locale.US, "%.1f Mbps", it / 1_000_000.0) }
