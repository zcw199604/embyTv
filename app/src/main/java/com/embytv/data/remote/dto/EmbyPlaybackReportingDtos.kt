package com.embytv.data.remote.dto

import com.google.gson.annotations.SerializedName

data class EmbyPlaybackStartRequest(
    @SerializedName("ItemId") val itemId: String,
    @SerializedName("MediaSourceId") val mediaSourceId: String?,
    @SerializedName("PlaySessionId") val playSessionId: String?,
    @SerializedName("PositionTicks") val positionTicks: Long,
    @SerializedName("CanSeek") val canSeek: Boolean = true,
    @SerializedName("IsPaused") val isPaused: Boolean = false,
    @SerializedName("PlayMethod") val playMethod: String = "DirectPlay",
)

data class EmbyPlaybackProgressRequest(
    @SerializedName("ItemId") val itemId: String,
    @SerializedName("MediaSourceId") val mediaSourceId: String?,
    @SerializedName("PlaySessionId") val playSessionId: String?,
    @SerializedName("PositionTicks") val positionTicks: Long,
    @SerializedName("IsPaused") val isPaused: Boolean,
    @SerializedName("IsMuted") val isMuted: Boolean = false,
    @SerializedName("PlayMethod") val playMethod: String = "DirectPlay",
)

data class EmbyPlaybackStoppedRequest(
    @SerializedName("ItemId") val itemId: String,
    @SerializedName("MediaSourceId") val mediaSourceId: String?,
    @SerializedName("PlaySessionId") val playSessionId: String?,
    @SerializedName("PositionTicks") val positionTicks: Long,
)
