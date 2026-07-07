package com.embytv.data.remote.dto

import com.google.gson.annotations.SerializedName

data class EmbyItemsResponse(
    @SerializedName("Items") val items: List<EmbyItemDto>? = emptyList(),
    @SerializedName("TotalRecordCount") val totalRecordCount: Int = 0,
)

data class EmbyViewsResponse(
    @SerializedName("Items") val items: List<EmbyItemDto>? = emptyList(),
    @SerializedName("TotalRecordCount") val totalRecordCount: Int = 0,
)

data class EmbyItemDto(
    @SerializedName("Id") val id: String?,
    @SerializedName("Name") val name: String?,
    @SerializedName("Type") val type: String?,
    @SerializedName("CollectionType") val collectionType: String? = null,
    @SerializedName("ChildCount") val childCount: Int? = null,
    @SerializedName("RecursiveItemCount") val recursiveItemCount: Int? = null,
    @SerializedName("Overview") val overview: String?,
    @SerializedName("PrimaryImageTag") val primaryImageTag: String? = null,
    @SerializedName("ImageTags") val imageTags: Map<String, String>?,
    @SerializedName("BackdropImageTags") val backdropImageTags: List<String>? = emptyList(),
    @SerializedName("ParentThumbItemId") val parentThumbItemId: String? = null,
    @SerializedName("ParentThumbImageTag") val parentThumbImageTag: String? = null,
    @SerializedName("ParentBackdropItemId") val parentBackdropItemId: String? = null,
    @SerializedName("ParentBackdropImageTags") val parentBackdropImageTags: List<String>? = emptyList(),
    @SerializedName("UserData") val userData: EmbyUserDataDto? = null,
    @SerializedName("RunTimeTicks") val runTimeTicks: Long? = null,
    @SerializedName("MediaSources") val mediaSources: List<EmbyMediaSourceDto>? = emptyList(),
    @SerializedName("ParentId") val parentId: String? = null,
    @SerializedName("SeriesId") val seriesId: String? = null,
    @SerializedName("SeriesName") val seriesName: String? = null,
    @SerializedName("SeriesPrimaryImageTag") val seriesPrimaryImageTag: String? = null,
    @SerializedName("SeasonName") val seasonName: String? = null,
    @SerializedName("ParentIndexNumber") val parentIndexNumber: Int? = null,
    @SerializedName("IndexNumber") val indexNumber: Int? = null,
    @SerializedName("ProductionYear") val productionYear: Int? = null,
    @SerializedName("Genres") val genres: List<String>? = emptyList(),
    @SerializedName("Studios") val studios: List<EmbyStudioDto>? = emptyList(),
    @SerializedName("People") val people: List<EmbyPersonDto>? = emptyList(),
    @SerializedName("CommunityRating") val communityRating: Double? = null,
    @SerializedName("CriticRating") val criticRating: Double? = null,
    @SerializedName("OfficialRating") val officialRating: String? = null,
    @SerializedName("ProviderIds") val providerIds: Map<String, String>? = emptyMap(),
    @SerializedName("PremiereDate") val premiereDate: String? = null,
    @SerializedName("DateCreated") val dateCreated: String? = null,
    @SerializedName("PlaylistItemId") val playlistItemId: String? = null,
    @SerializedName("Chapters") val chapters: List<EmbyChapterInfoDto>? = emptyList(),
)

data class EmbyChapterInfoDto(
    @SerializedName("StartPositionTicks") val startPositionTicks: Long? = null,
    @SerializedName("Name") val name: String? = null,
    @SerializedName("ImageTag") val imageTag: String? = null,
    @SerializedName("ChapterIndex") val chapterIndex: Int? = null,
)

data class EmbyStudioDto(
    @SerializedName("Name") val name: String?,
)

data class EmbyPersonDto(
    @SerializedName("Id") val id: String?,
    @SerializedName("Name") val name: String?,
    @SerializedName("Role") val role: String?,
    @SerializedName("Type") val type: String?,
    @SerializedName("PrimaryImageTag") val primaryImageTag: String? = null,
    @SerializedName("ImageTags") val imageTags: Map<String, String>? = null,
)

data class EmbyUserDataUpdateRequest(
    @SerializedName("PlaybackPositionTicks") val playbackPositionTicks: Long? = null,
    @SerializedName("Played") val played: Boolean? = null,
    @SerializedName("IsFavorite") val isFavorite: Boolean? = null,
)

data class EmbyUserDataDto(
    @SerializedName("PlaybackPositionTicks") val playbackPositionTicks: Long = 0L,
    @SerializedName("PlayedPercentage") val playedPercentage: Double? = null,
    @SerializedName("PlayCount") val playCount: Int? = null,
    @SerializedName("Played") val played: Boolean = false,
    @SerializedName("IsFavorite") val isFavorite: Boolean = false,
    @SerializedName("UnplayedItemCount") val unplayedItemCount: Int? = null,
)

data class EmbyPlaybackInfoResponse(
    @SerializedName("PlaySessionId") val playSessionId: String?,
    @SerializedName("MediaSources") val mediaSources: List<EmbyMediaSourceDto>? = emptyList(),
)

data class EmbyMediaSourceDto(
    @SerializedName("Id") val id: String?,
    @SerializedName("Container") val container: String?,
    @SerializedName("Bitrate") val bitrate: Int?,
    @SerializedName("MediaStreams") val mediaStreams: List<EmbyMediaStreamDto>? = emptyList(),
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
    @SerializedName("IsExternal") val isExternal: Boolean = false,
    @SerializedName("DeliveryMethod") val deliveryMethod: String? = null,
    @SerializedName("DeliveryUrl") val deliveryUrl: String? = null,
)
