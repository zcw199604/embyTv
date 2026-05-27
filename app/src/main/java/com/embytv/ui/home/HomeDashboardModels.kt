package com.embytv.ui.home

import com.embytv.domain.model.MediaItemSummary

enum class HomeNavigationId {
    Home,
    Movies,
    TvShows,
    Collections,
    Settings,
}

data class HomeNavigationItem(
    val id: HomeNavigationId,
    val title: String,
    val enabled: Boolean,
)

data class LibrarySummaryUiModel(
    val id: String,
    val title: String,
    val countLabel: String,
    val imageUrl: String?,
    val enabled: Boolean = true,
)

data class MediaCardUiModel(
    val id: String,
    val title: String,
    val subtitle: String,
    val imageUrl: String?,
    val progressFraction: Float,
    val badge: String,
)

data class HomeDashboardUiModel(
    val navigationItems: List<HomeNavigationItem>,
    val libraries: List<LibrarySummaryUiModel>,
    val continueWatching: List<MediaCardUiModel>,
)

object HomeDashboardMapper {
    fun map(items: List<MediaItemSummary>): HomeDashboardUiModel {
        val movies = items.filter { it.type.equals("Movie", ignoreCase = true) }
        val shows = items.filter {
            it.type.equals("Series", ignoreCase = true) ||
                it.type.equals("Episode", ignoreCase = true)
        }

        return HomeDashboardUiModel(
            navigationItems = listOf(
                HomeNavigationItem(HomeNavigationId.Home, "Home", enabled = true),
                HomeNavigationItem(HomeNavigationId.Movies, "Movies", enabled = false),
                HomeNavigationItem(HomeNavigationId.TvShows, "TV Shows", enabled = false),
                HomeNavigationItem(HomeNavigationId.Collections, "Collections", enabled = false),
                HomeNavigationItem(HomeNavigationId.Settings, "Settings", enabled = false),
            ),
            libraries = listOf(
                LibrarySummaryUiModel(
                    id = "movies",
                    title = "Movies",
                    countLabel = movies.size.toItemCountLabel(),
                    imageUrl = movies.firstOrNull()?.imageUrl,
                    enabled = movies.isNotEmpty(),
                ),
                LibrarySummaryUiModel(
                    id = "tv",
                    title = "TV Shows",
                    countLabel = shows.size.toSeriesCountLabel(),
                    imageUrl = shows.firstOrNull()?.imageUrl,
                    enabled = shows.isNotEmpty(),
                ),
                LibrarySummaryUiModel(
                    id = "anime",
                    title = "Anime",
                    countLabel = "0 items",
                    imageUrl = null,
                    enabled = false,
                ),
            ),
            continueWatching = items.take(12).mapIndexed { index, item ->
                MediaCardUiModel(
                    id = item.id,
                    title = item.name.ifBlank { item.id },
                    subtitle = item.overview?.takeIf { it.isNotBlank() } ?: item.type,
                    imageUrl = item.imageUrl,
                    progressFraction = seededProgress(index),
                    badge = item.type.ifBlank { "Media" },
                )
            },
        )
    }

    private fun Int.toItemCountLabel(): String = if (this == 1) "1 item" else "$this items"

    private fun Int.toSeriesCountLabel(): String = if (this == 1) "1 series" else "$this series"

    private fun seededProgress(index: Int): Float {
        return when (index % 4) {
            0 -> 0.72f
            1 -> 0.38f
            2 -> 0.18f
            else -> 0.0f
        }
    }
}
