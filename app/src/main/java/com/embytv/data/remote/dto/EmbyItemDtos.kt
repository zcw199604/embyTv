package com.embytv.data.remote.dto

import com.google.gson.annotations.SerializedName

data class EmbyItemsResponse(
    @SerializedName("Items") val items: List<EmbyItemDto> = emptyList(),
    @SerializedName("TotalRecordCount") val totalRecordCount: Int = 0,
)

data class EmbyViewsResponse(
    @SerializedName("Items") val items: List<EmbyItemDto> = emptyList(),
    @SerializedName("TotalRecordCount") val totalRecordCount: Int = 0,
)

data class EmbyItemDto(
    @SerializedName("Id") val id: String?,
    @SerializedName("Name") val name: String?,
    @SerializedName("Type") val type: String?,
    @SerializedName("CollectionType") val collectionType: String? = null,
    @SerializedName("ChildCount") val childCount: Int? = null,
    @SerializedName("Overview") val overview: String?,
    @SerializedName("ImageTags") val imageTags: Map<String, String>?,
    @SerializedName("BackdropImageTags") val backdropImageTags: List<String> = emptyList(),
    @SerializedName("UserData") val userData: EmbyUserDataDto? = null,
    @SerializedName("RunTimeTicks") val runTimeTicks: Long? = null,
    @SerializedName("MediaSources") val mediaSources: List<EmbyMediaSourceDto> = emptyList(),
    @SerializedName("ParentId") val parentId: String? = null,
    @SerializedName("SeriesName") val seriesName: String? = null,
    @SerializedName("SeasonName") val seasonName: String? = null,
    @SerializedName("ParentIndexNumber") val parentIndexNumber: Int? = null,
    @SerializedName("IndexNumber") val indexNumber: Int? = null,
    @SerializedName("ProductionYear") val productionYear: Int? = null,
)

data class EmbyUserDataDto(
    @SerializedName("PlaybackPositionTicks") val playbackPositionTicks: Long = 0L,
    @SerializedName("PlayedPercentage") val playedPercentage: Double? = null,
    @SerializedName("PlayCount") val playCount: Int? = null,
    @SerializedName("Played") val played: Boolean = false,
    @SerializedName("IsFavorite") val isFavorite: Boolean = false,
)

data class EmbyPlaybackInfoResponse(
    @SerializedName("PlaySessionId") val playSessionId: String?,
    @SerializedName("MediaSources") val mediaSources: List<EmbyMediaSourceDto> = emptyList(),
)

data class EmbyMediaSourceDto(
    @SerializedName("Id") val id: String?,
    @SerializedName("Container") val container: String?,
    @SerializedName("Bitrate") val bitrate: Int?,
    @SerializedName("MediaStreams") val mediaStreams: List<EmbyMediaStreamDto> = emptyList(),
)

data class EmbyMediaStreamDto(
    @SerializedName("Index") val index: Int?,
    @SerializedName("Type") val type: String?,
    @SerializedName("Codec") val codec: String?,
    @SerializedName("DisplayTitle") val displayTitle: String?,
    @SerializedName("Language") val language: String?,
    @SerializedName("Channels") val channels: Int?,
    @SerializedName("Width") val width: Int?,
    @SerializedName("Height") val height: Int?,
    @SerializedName("VideoRange") val videoRange: String?,
    @SerializedName("IsDefault") val isDefault: Boolean = false,
    @SerializedName("IsForced") val isForced: Boolean = false,
)
