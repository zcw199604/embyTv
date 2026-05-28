package com.embytv.ui.home

import com.embytv.domain.model.EmbyFavoriteDashboard
import com.embytv.domain.model.MediaItemSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class HomeFavoritesMapperTest {
    @Test
    fun mapsFavoriteMoviesWithImagesAndNames() {
        val uiModel = HomeFavoritesMapper.map(
            dashboard = EmbyFavoriteDashboard(
                movies = listOf(
                    MediaItemSummary(
                        id = "movie-1",
                        name = "收藏电影",
                        type = "Movie",
                        overview = null,
                        imageUrl = "https://example.test/movie.jpg",
                        productionYear = 2026,
                    ),
                ),
            ),
            selectedCategory = FavoriteCategory.Movie,
        )

        assertEquals("收藏电影", uiModel.title)
        assertEquals("1 部电影", uiModel.categoryTabs.single { it.category == FavoriteCategory.Movie }.countLabel)
        assertEquals("0 部剧集", uiModel.categoryTabs.single { it.category == FavoriteCategory.Series }.countLabel)
        assertEquals("movie-1", uiModel.items.single().id)
        assertEquals("收藏电影", uiModel.items.single().title)
        assertEquals("https://example.test/movie.jpg", uiModel.items.single().imageUrl)
        assertEquals("Movie", uiModel.items.single().badge)
        assertFalse(uiModel.isEmpty)
    }

    @Test
    fun mapsFavoriteSeriesWithUnplayedBadgeAndFallbackTitle() {
        val uiModel = HomeFavoritesMapper.map(
            dashboard = EmbyFavoriteDashboard(
                series = listOf(
                    MediaItemSummary(
                        id = "series-1",
                        name = "",
                        type = "Series",
                        overview = null,
                        imageUrl = null,
                        seriesName = "收藏剧集",
                        unplayedItemCount = 7,
                    ),
                ),
            ),
            selectedCategory = FavoriteCategory.Series,
        )

        assertEquals("收藏剧集", uiModel.items.single().title)
        assertEquals(null, uiModel.items.single().imageUrl)
        assertEquals("剩 7 集", uiModel.items.single().cornerBadge)
        assertEquals("Series", uiModel.items.single().badge)
    }

    @Test
    fun exposesCategorySpecificEmptyCopy() {
        val movies = HomeFavoritesMapper.map(EmbyFavoriteDashboard(), FavoriteCategory.Movie)
        val series = HomeFavoritesMapper.map(EmbyFavoriteDashboard(), FavoriteCategory.Series)

        assertEquals("还没有收藏电影", movies.emptyTitle)
        assertEquals("收藏电影后会显示在这里。", movies.emptySubtitle)
        assertEquals("还没有收藏电视剧", series.emptyTitle)
        assertEquals("收藏电视剧或单集后会按剧集汇总显示在这里。", series.emptySubtitle)
    }

    @Test
    fun closesFavoritesAndKeepsMovieAsDefaultCategory() {
        val state = FavoriteContentUiState(
            isOpen = true,
            selectedCategory = FavoriteCategory.Series,
            dashboard = EmbyFavoriteDashboard(),
            isLoading = true,
            errorMessage = "error",
        )

        val closed = state.close()
        val fresh = FavoriteContentUiState()

        assertFalse(closed.isOpen)
        assertEquals(FavoriteCategory.Series, closed.selectedCategory)
        assertEquals(null, closed.errorMessage)
        assertEquals(FavoriteCategory.Movie, fresh.selectedCategory)
    }

    @Test
    fun favoriteStateSupportsOpenSwitchAndBackFlow() {
        val dashboard = EmbyFavoriteDashboard(
            movies = listOf(
                MediaItemSummary(
                    id = "movie-1",
                    name = "收藏电影",
                    type = "Movie",
                    overview = null,
                    imageUrl = null,
                ),
            ),
        )

        val opened = FavoriteContentUiState().openLoading()
        val loaded = opened.loaded(dashboard)
        val switched = loaded.select(FavoriteCategory.Series)
        val closed = switched.close()

        assertEquals(true, opened.isOpen)
        assertEquals(true, opened.isLoading)
        assertEquals(false, loaded.isLoading)
        assertEquals("movie-1", loaded.dashboard.movies.single().id)
        assertEquals(FavoriteCategory.Series, switched.selectedCategory)
        assertEquals(false, closed.isOpen)
        assertEquals(FavoriteCategory.Series, closed.selectedCategory)
    }
}
