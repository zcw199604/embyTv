package com.embytv.ui.home

import com.embytv.domain.model.EmbyHomeDashboard
import com.embytv.domain.model.EmbyFavoriteDashboard
import com.embytv.domain.model.EmbyLibraryContent
import com.embytv.domain.model.EmbyMediaDetail
import com.embytv.domain.model.EmbySeasonEpisodes
import com.embytv.domain.model.EmbySeasonSummary
import com.embytv.domain.model.MediaItemSummary
import java.util.Locale

const val FAVORITES_NAVIGATION_ID = "favorites"

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

enum class FavoriteCategory {
    Movie,
    Series,
}

data class FavoriteCategoryTabUiModel(
    val category: FavoriteCategory,
    val title: String,
    val countLabel: String,
    val selected: Boolean,
)

data class FavoriteDashboardUiModel(
    val title: String,
    val categoryTabs: List<FavoriteCategoryTabUiModel>,
    val items: List<MediaCardUiModel>,
    val emptyTitle: String,
    val emptySubtitle: String,
) {
    val isEmpty: Boolean = items.isEmpty()
}

data class SeasonCardUiModel(
    val id: String,
    val title: String,
    val subtitle: String,
    val imageUrl: String?,
    val cornerBadge: String? = null,
)

data class MediaFactUiModel(
    val label: String,
    val value: String,
)

data class CastMemberUiModel(
    val name: String,
    val role: String?,
)

data class MediaDetailUiModel(
    val title: String,
    val overview: String,
    val metadata: String,
    val people: List<String>,
    val mediaFacts: List<MediaFactUiModel>,
    val castMembers: List<CastMemberUiModel>,
    val imageUrl: String?,
    val backdropImageUrl: String?,
    val isMovie: Boolean,
    val seasons: List<SeasonCardUiModel>,
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

data class FavoriteContentUiState(
    val isOpen: Boolean = false,
    val selectedCategory: FavoriteCategory = FavoriteCategory.Movie,
    val dashboard: EmbyFavoriteDashboard = EmbyFavoriteDashboard(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
) {
    fun openLoading(): FavoriteContentUiState = copy(
        isOpen = true,
        isLoading = true,
        errorMessage = null,
    )

    fun loaded(dashboard: EmbyFavoriteDashboard): FavoriteContentUiState = copy(
        isOpen = true,
        dashboard = dashboard,
        isLoading = false,
        errorMessage = null,
    )

    fun failed(message: String): FavoriteContentUiState = copy(
        isOpen = true,
        isLoading = false,
        errorMessage = message,
    )

    fun select(category: FavoriteCategory): FavoriteContentUiState = copy(selectedCategory = category)

    fun close(): FavoriteContentUiState = copy(
        isOpen = false,
        isLoading = false,
        errorMessage = null,
    )
}

data class MediaDetailUiState(
    val requestedItem: MediaItemSummary? = null,
    val detail: EmbyMediaDetail? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val selectedSeason: EmbySeasonSummary? = null,
    val seasonEpisodes: EmbySeasonEpisodes? = null,
    val isSeasonLoading: Boolean = false,
    val seasonErrorMessage: String? = null,
) {
    val isOpen: Boolean = requestedItem != null || detail != null
    val isSeasonOpen: Boolean = selectedSeason != null || seasonEpisodes != null || isSeasonLoading

    fun openLoading(item: MediaItemSummary): MediaDetailUiState = MediaDetailUiState(
        requestedItem = item,
        isLoading = true,
        errorMessage = null,
    )

    fun loaded(detail: EmbyMediaDetail): MediaDetailUiState = copy(
        requestedItem = detail.item,
        detail = detail,
        isLoading = false,
        errorMessage = null,
        selectedSeason = null,
        seasonEpisodes = null,
        isSeasonLoading = false,
        seasonErrorMessage = null,
    )

    fun failed(message: String): MediaDetailUiState = copy(
        isLoading = false,
        errorMessage = message,
        selectedSeason = null,
        seasonEpisodes = null,
        isSeasonLoading = false,
        seasonErrorMessage = null,
    )

    fun loadingSeason(season: EmbySeasonSummary): MediaDetailUiState = copy(
        selectedSeason = season,
        seasonEpisodes = null,
        isSeasonLoading = true,
        seasonErrorMessage = null,
    )

    fun seasonLoaded(episodes: EmbySeasonEpisodes): MediaDetailUiState = copy(
        selectedSeason = episodes.season,
        seasonEpisodes = episodes,
        isSeasonLoading = false,
        seasonErrorMessage = null,
    )

    fun seasonFailed(message: String): MediaDetailUiState = copy(
        isSeasonLoading = false,
        seasonErrorMessage = message,
    )

    fun close(): MediaDetailUiState = MediaDetailUiState()

    fun back(): MediaDetailUiState =
        if (isSeasonOpen) {
            copy(
                selectedSeason = null,
                seasonEpisodes = null,
                isSeasonLoading = false,
                seasonErrorMessage = null,
            )
        } else {
            close()
        }
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
            navigationItems = listOf(
                HomeNavigationItem(
                    id = FAVORITES_NAVIGATION_ID,
                    title = "收藏",
                    enabled = true,
                    disabledReason = null,
                ),
            ) + dashboard.libraries.map { library ->
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

object HomeMediaDetailMapper {
    fun map(detail: EmbyMediaDetail): MediaDetailUiModel =
        MediaDetailUiModel(
            title = detail.item.name.ifBlank { detail.item.seriesName ?: detail.item.id },
            overview = detail.item.overview?.takeIf { it.isNotBlank() } ?: "暂无简介",
            metadata = detail.metadataLabel(),
            people = detail.people
                .filter { person -> person.name.isNotBlank() }
                .take(12)
                .map { person ->
                    val role = person.role?.takeIf { it.isNotBlank() }
                    if (role == null) person.name else "${person.name} 饰 $role"
                },
            mediaFacts = detail.mediaFacts(),
            castMembers = detail.people
                .filter { person -> person.name.isNotBlank() }
                .take(12)
                .map { person ->
                    CastMemberUiModel(
                        name = person.name,
                        role = person.role?.takeIf { it.isNotBlank() },
                    )
                },
            imageUrl = detail.item.imageUrl ?: detail.item.thumbImageUrl,
            backdropImageUrl = detail.item.backdropImageUrl ?: detail.item.thumbImageUrl ?: detail.item.imageUrl,
            isMovie = detail.item.type.equals("Movie", ignoreCase = true),
            seasons = detail.seasons.map { it.toSeasonCard() },
        )

    fun mapEpisodes(episodes: EmbySeasonEpisodes): List<MediaCardUiModel> =
        episodes.episodes.map { HomeDashboardMapper.mapMediaItem(it) }

    private fun EmbyMediaDetail.metadataLabel(): String =
        listOfNotNull(
            item.productionYear?.toString(),
            genres.takeIf { it.isNotEmpty() }?.joinToString(" / "),
            communityRating?.let { String.format(Locale.US, "%.1f", it) },
            officialRating?.takeIf { it.isNotBlank() },
        ).joinToString(" · ")

    private fun EmbyMediaDetail.mediaFacts(): List<MediaFactUiModel> =
        buildList {
            item.productionYear?.let { add(MediaFactUiModel("年份", it.toString())) }
            runtimeMinutes()?.let { add(MediaFactUiModel("时长", "$it 分钟")) }
            genres.takeIf { it.isNotEmpty() }?.let { add(MediaFactUiModel("类型", it.joinToString(" / "))) }
            communityRating?.let { add(MediaFactUiModel("评分", String.format(Locale.US, "%.1f", it))) }
            officialRating?.takeIf { it.isNotBlank() }?.let { add(MediaFactUiModel("分级", it)) }
            premiereDate?.takeIf { it.length >= 10 }?.let { add(MediaFactUiModel("首播", it.take(10))) }
            studios.takeIf { it.isNotEmpty() }?.let { add(MediaFactUiModel("制片方", it.joinToString(" / "))) }
            item.recursiveItemCount?.let { add(MediaFactUiModel("总集数", "$it 集")) }
            item.childCount?.let { add(MediaFactUiModel("季数", "$it 季")) }
        }

    private fun EmbyMediaDetail.runtimeMinutes(): Long? {
        val ticks = item.runTimeTicks ?: return null
        if (ticks <= 0L) return null
        return (ticks / 600_000_000L).takeIf { it > 0L }
    }

    private fun EmbySeasonSummary.toSeasonCard(): SeasonCardUiModel =
        SeasonCardUiModel(
            id = id,
            title = name.ifBlank { indexNumber?.let { "第 $it 季" } ?: id },
            subtitle = episodeCount?.let { "$it 集" } ?: "剧集",
            imageUrl = imageUrl,
            cornerBadge = unplayedItemCount?.takeIf { it > 0 }?.let { "剩 $it 集" },
        )
}

object HomeFavoritesMapper {
    fun map(
        dashboard: EmbyFavoriteDashboard,
        selectedCategory: FavoriteCategory,
    ): FavoriteDashboardUiModel {
        val sourceItems = when (selectedCategory) {
            FavoriteCategory.Movie -> dashboard.movies
            FavoriteCategory.Series -> dashboard.series
        }
        return FavoriteDashboardUiModel(
            title = when (selectedCategory) {
                FavoriteCategory.Movie -> "收藏电影"
                FavoriteCategory.Series -> "收藏电视剧"
            },
            categoryTabs = listOf(
                FavoriteCategoryTabUiModel(
                    category = FavoriteCategory.Movie,
                    title = "电影",
                    countLabel = dashboard.movies.size.toMovieCountLabel(),
                    selected = selectedCategory == FavoriteCategory.Movie,
                ),
                FavoriteCategoryTabUiModel(
                    category = FavoriteCategory.Series,
                    title = "电视剧",
                    countLabel = dashboard.series.size.toSeriesCountLabel(),
                    selected = selectedCategory == FavoriteCategory.Series,
                ),
            ),
            items = sourceItems.map { HomeDashboardMapper.mapMediaItem(it) },
            emptyTitle = when (selectedCategory) {
                FavoriteCategory.Movie -> "还没有收藏电影"
                FavoriteCategory.Series -> "还没有收藏电视剧"
            },
            emptySubtitle = when (selectedCategory) {
                FavoriteCategory.Movie -> "收藏电影后会显示在这里。"
                FavoriteCategory.Series -> "收藏电视剧或单集后会按剧集汇总显示在这里。"
            },
        )
    }

    private fun Int.toMovieCountLabel(): String = "$this 部电影"

    private fun Int.toSeriesCountLabel(): String = "$this 部剧集"
}
