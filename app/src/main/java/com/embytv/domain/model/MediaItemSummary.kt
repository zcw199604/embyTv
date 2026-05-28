package com.embytv.domain.model

data class MediaItemSummary(
    val id: String,
    val name: String,
    val type: String,
    val overview: String?,
    val imageUrl: String?,
    val thumbImageUrl: String? = null,
    val backdropImageUrl: String? = null,
    val seriesId: String? = null,
    val seriesName: String? = null,
    val seasonName: String? = null,
    val parentIndexNumber: Int? = null,
    val indexNumber: Int? = null,
    val parentId: String? = null,
    val runTimeTicks: Long? = null,
    val playbackPositionTicks: Long = 0L,
    val playedPercentage: Double? = null,
    val productionYear: Int? = null,
    val unplayedItemCount: Int? = null,
    val childCount: Int? = null,
    val recursiveItemCount: Int? = null,
    val dateCreated: String? = null,
)

data class EmbyLibrarySummary(
    val id: String,
    val name: String,
    val type: String,
    val collectionType: String?,
    val itemCount: Int,
    val imageUrl: String?,
)

data class EmbyLibraryLatestSection(
    val library: EmbyLibrarySummary,
    val items: List<MediaItemSummary>,
)

data class EmbyLibraryContent(
    val library: EmbyLibrarySummary,
    val items: List<MediaItemSummary>,
)

data class EmbyFavoriteDashboard(
    val movies: List<MediaItemSummary> = emptyList(),
    val series: List<MediaItemSummary> = emptyList(),
    val totalCount: Int = 0,
)

data class EmbyHomeDashboard(
    val libraries: List<EmbyLibrarySummary> = emptyList(),
    val resumeItems: List<MediaItemSummary> = emptyList(),
    val latestItems: List<MediaItemSummary> = emptyList(),
    val libraryLatestSections: List<EmbyLibraryLatestSection> = emptyList(),
)

data class EmbyPersonSummary(
    val id: String?,
    val name: String,
    val role: String?,
    val type: String?,
)

data class EmbySeasonSummary(
    val id: String,
    val name: String,
    val indexNumber: Int?,
    val imageUrl: String?,
    val episodeCount: Int?,
    val unplayedItemCount: Int?,
)

data class EmbyMediaDetail(
    val item: MediaItemSummary,
    val people: List<EmbyPersonSummary>,
    val genres: List<String>,
    val studios: List<String>,
    val communityRating: Double?,
    val officialRating: String?,
    val premiereDate: String?,
    val seasons: List<EmbySeasonSummary> = emptyList(),
)

data class EmbySeasonEpisodes(
    val season: EmbySeasonSummary,
    val episodes: List<MediaItemSummary>,
)
