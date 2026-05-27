package com.embytv.domain.model

data class MediaItemSummary(
    val id: String,
    val name: String,
    val type: String,
    val overview: String?,
    val imageUrl: String?,
    val seriesName: String? = null,
    val seasonName: String? = null,
    val runTimeTicks: Long? = null,
    val playbackPositionTicks: Long = 0L,
    val playedPercentage: Double? = null,
    val productionYear: Int? = null,
)

data class EmbyLibrarySummary(
    val id: String,
    val name: String,
    val type: String,
    val collectionType: String?,
    val itemCount: Int,
    val imageUrl: String?,
)

data class EmbyHomeDashboard(
    val libraries: List<EmbyLibrarySummary> = emptyList(),
    val resumeItems: List<MediaItemSummary> = emptyList(),
    val latestItems: List<MediaItemSummary> = emptyList(),
)
