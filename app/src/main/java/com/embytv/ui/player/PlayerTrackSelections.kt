package com.embytv.ui.player

import androidx.media3.common.C
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.TrackSelectionParameters
import com.embytv.domain.model.PlayerTrackOption
import com.embytv.domain.model.PlayerTrackType

fun TrackSelectionParameters.selectTrack(option: PlayerTrackOption): TrackSelectionParameters {
    val builder = buildUpon()
    if (option.type == PlayerTrackType.Subtitle) {
        builder.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
    }
    return builder
        .setOverrideForType(TrackSelectionOverride(option.trackGroup, option.trackIndex))
        .build()
}

fun TrackSelectionParameters.disableSubtitles(): TrackSelectionParameters =
    buildUpon()
        .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
        .clearOverridesOfType(C.TRACK_TYPE_TEXT)
        .build()
