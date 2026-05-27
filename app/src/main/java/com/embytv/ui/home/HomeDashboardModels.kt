package com.embytv.ui.home

import com.embytv.domain.model.EmbyHomeDashboard
import com.embytv.domain.model.EmbyLibraryContent
import com.embytv.domain.model.MediaItemSummary

data class HomeNavigationItem(
    val id: String,
    val title: String,
    val enabled: Boolean,
    val disabledReason: String? = null,
)

data class LibrarySummaryUiModel(
    val id: String,
    val title: String,
    val countLabel: String,
    val imageUrl: String?,
    val enabled: Boolean = true,
    val disabledReason: String? = null,
)

data class MediaCardUiModel(
    val id: String,
    val title: String,
    val subtitle: String,
    val imageUrl: String?,
    val progressFraction: Float,
    val badge: String,
    val cornerBadge: String? = null,
)

data class MediaSectionUiModel(
    val id: String,
    val title: String,
    val items: List<MediaCardUiModel>,
)

data class HomeDashboardUiModel(
    val navigationItems: List<HomeNavigationItem>,
    val libraries: List<LibrarySummaryUiModel>,
    val continueWatching: List<MediaCardUiModel>,
    val libraryLatestSections: List<MediaSectionUiModel>,
    val mediaSectionTitle: String,
)

data class DrawerUiState(
    val isOpen: Boolean = false,
    val restoreMenuFocus: Boolean = false,
) {
    fun open(): DrawerUiState = copy(isOpen = true, restoreMenuFocus = false)

    fun close(): DrawerUiState = copy(isOpen = false, restoreMenuFocus = true)

    fun onBack(): DrawerUiState = if (isOpen) close() else this

    fun menuFocusRestored(): DrawerUiState = copy(restoreMenuFocus = false)
}

data class LibraryContentUiState(
    val selectedLibraryId: String? = null,
    val content: EmbyLibraryContent? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
) {
    val isOpen: Boolean = selectedLibraryId != null || content != null

    fun close(): LibraryContentUiState = LibraryContentUiState()
}

object HomeDashboardMapper {
    fun mapMediaItem(item: MediaItemSummary): MediaCardUiModel = item.toMediaCard()

    fun map(dashboard: EmbyHomeDashboard): HomeDashboardUiModel {
        val mediaItems = if (dashboard.resumeItems.isNotEmpty()) {
            dashboard.resumeItems
        } else {
            dashboard.latestItems
        }
        return HomeDashboardUiModel(
            navigationItems = dashboard.libraries.map { library ->
                HomeNavigationItem(
                    id = library.id,
                    title = library.name.ifBlank { library.collectionType ?: "媒体库" },
                    enabled = true,
                    disabledReason = null,
                )
            },
            libraries = dashboard.libraries.map { library ->
                LibrarySummaryUiModel(
                    id = library.id,
                    title = library.name.ifBlank { library.collectionType ?: library.type },
                    countLabel = library.itemCount.toItemCountLabel(),
                    imageUrl = library.imageUrl,
                    enabled = true,
                    disabledReason = null,
                )
            },
            continueWatching = mediaItems.take(12).map { item ->
                item.toMediaCard()
            },
            libraryLatestSections = dashboard.libraryLatestSections.mapNotNull { section ->
                val items = section.items.take(12).map { it.toMediaCard() }
                if (items.isEmpty()) {
                    null
                } else {
                    MediaSectionUiModel(
                        id = section.library.id,
                        title = "${section.library.name.ifBlank { section.library.collectionType ?: "媒体库" }} · 最新入库",
                        items = items,
                    )
                }
            },
            mediaSectionTitle = if (dashboard.resumeItems.isNotEmpty()) "Continue Watching" else "最近入库",
        )
    }

    private fun Int.toItemCountLabel(): String = if (this == 1) "1 item" else "$this items"

    private fun MediaItemSummary.toMediaCard(): MediaCardUiModel =
        MediaCardUiModel(
            id = id,
            title = displayTitle(),
            subtitle = subtitle(),
            imageUrl = preferredImageUrl(),
            progressFraction = progressFraction(),
            badge = type.ifBlank { "Media" },
            cornerBadge = cornerBadge(),
        )

    private fun MediaItemSummary.displayTitle(): String = name.ifBlank { seriesName ?: id }

    private fun MediaItemSummary.subtitle(): String =
        episodeContext()?.takeIf { it.isNotBlank() }
            ?: seriesName?.takeIf { it.isNotBlank() }
            ?: overview?.takeIf { it.isNotBlank() }
            ?: productionYear?.toString()
            ?: type

    private fun MediaItemSummary.episodeContext(): String? {
        if (!type.equals("Episode", ignoreCase = true)) return null
        val series = seriesName?.takeIf { it.isNotBlank() }
        val episodeCode = episodeCode()
        return listOfNotNull(series, episodeCode).joinToString(" · ").ifBlank {
            seasonName?.takeIf { it.isNotBlank() } ?: name.takeIf { it.isNotBlank() }
        }
    }

    private fun MediaItemSummary.episodeCode(): String? {
        val season = parentIndexNumber ?: return indexNumber?.let { "E%02d".format(it) }
        val episode = indexNumber ?: return "S%02d".format(season)
        return "S%02dE%02d".format(season, episode)
    }

    private fun MediaItemSummary.preferredImageUrl(): String? =
        if (type.equals("Episode", ignoreCase = true)) {
            thumbImageUrl ?: backdropImageUrl ?: imageUrl
        } else {
            imageUrl ?: thumbImageUrl ?: backdropImageUrl
        }

    private fun MediaItemSummary.cornerBadge(): String? {
        if (!type.equals("Series", ignoreCase = true)) return null
        val count = unplayedItemCount ?: return null
        if (count <= 0) return null
        return "剩 $count 集"
    }

    private fun MediaItemSummary.progressFraction(): Float {
        playedPercentage?.let { return (it / 100.0).toFloat().coerceIn(0f, 1f) }
        val runtime = runTimeTicks ?: return 0f
        if (runtime <= 0L) return 0f
        return (playbackPositionTicks.toFloat() / runtime.toFloat()).coerceIn(0f, 1f)
    }
}
