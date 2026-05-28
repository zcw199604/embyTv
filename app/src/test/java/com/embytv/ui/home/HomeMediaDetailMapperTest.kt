package com.embytv.ui.home

import com.embytv.domain.model.EmbyMediaDetail
import com.embytv.domain.model.EmbyPersonSummary
import com.embytv.domain.model.EmbySeasonEpisodes
import com.embytv.domain.model.EmbySeasonSummary
import com.embytv.domain.model.MediaItemSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeMediaDetailMapperTest {
    @Test
    fun mapsMovieDetailWithOverviewPeopleAndMetadata() {
        val uiModel = HomeMediaDetailMapper.map(
            EmbyMediaDetail(
                item = MediaItemSummary(
                    id = "movie-1",
                    name = "真实电影",
                    type = "Movie",
                    overview = "真实简介",
                    imageUrl = "https://example.test/movie.jpg",
                    backdropImageUrl = "https://example.test/backdrop.jpg",
                    productionYear = 2026,
                ),
                people = listOf(EmbyPersonSummary(id = "person-1", name = "演员甲", role = "主角", type = "Actor")),
                genres = listOf("剧情", "科幻"),
                studios = listOf("电影公司"),
                communityRating = 8.6,
                officialRating = "PG-13",
                premiereDate = "2026-05-01T00:00:00.0000000Z",
            ),
        )

        assertEquals("真实电影", uiModel.title)
        assertEquals("真实简介", uiModel.overview)
        assertEquals("2026 · 剧情 / 科幻 · 8.6 · PG-13", uiModel.metadata)
        assertEquals("演员甲 饰 主角", uiModel.people.single())
        assertEquals("https://example.test/movie.jpg", uiModel.imageUrl)
        assertEquals("https://example.test/backdrop.jpg", uiModel.backdropImageUrl)
        assertTrue(uiModel.isMovie)
        assertEquals(emptyList<SeasonCardUiModel>(), uiModel.seasons)
    }

    @Test
    fun mapsSeriesSeasonsWithUnplayedCornerBadge() {
        val uiModel = HomeMediaDetailMapper.map(
            EmbyMediaDetail(
                item = MediaItemSummary(
                    id = "series-1",
                    name = "真实剧集",
                    type = "Series",
                    overview = null,
                    imageUrl = null,
                    productionYear = 2025,
                ),
                people = emptyList(),
                genres = emptyList(),
                studios = emptyList(),
                communityRating = null,
                officialRating = null,
                premiereDate = null,
                seasons = listOf(
                    EmbySeasonSummary(
                        id = "season-1",
                        name = "第 1 季",
                        indexNumber = 1,
                        imageUrl = "https://example.test/season.jpg",
                        episodeCount = 12,
                        unplayedItemCount = 4,
                    ),
                    EmbySeasonSummary(
                        id = "season-2",
                        name = "第 2 季",
                        indexNumber = 2,
                        imageUrl = null,
                        episodeCount = 8,
                        unplayedItemCount = 0,
                    ),
                ),
            ),
        )

        assertFalse(uiModel.isMovie)
        assertEquals("真实剧集", uiModel.title)
        assertEquals("第 1 季", uiModel.seasons[0].title)
        assertEquals("12 集", uiModel.seasons[0].subtitle)
        assertEquals("剩 4 集", uiModel.seasons[0].cornerBadge)
        assertNull(uiModel.seasons[1].cornerBadge)
    }

    @Test
    fun mediaDetailStateSupportsSeasonBackAndClose() {
        val detail = EmbyMediaDetail(
            item = MediaItemSummary(
                id = "series-1",
                name = "真实剧集",
                type = "Series",
                overview = null,
                imageUrl = null,
            ),
            people = emptyList(),
            genres = emptyList(),
            studios = emptyList(),
            communityRating = null,
            officialRating = null,
            premiereDate = null,
        )
        val season = EmbySeasonSummary(
            id = "season-1",
            name = "第 1 季",
            indexNumber = 1,
            imageUrl = null,
            episodeCount = 1,
            unplayedItemCount = 1,
        )
        val episodes = EmbySeasonEpisodes(
            season = season,
            episodes = listOf(
                MediaItemSummary(
                    id = "episode-1",
                    name = "第一集",
                    type = "Episode",
                    overview = null,
                    imageUrl = null,
                ),
            ),
        )

        val loaded = MediaDetailUiState().openLoading(detail.item).loaded(detail)
        val seasonLoaded = loaded.loadingSeason(season).seasonLoaded(episodes)
        val backToDetail = seasonLoaded.back()
        val closed = backToDetail.back()

        assertTrue(loaded.isOpen)
        assertFalse(loaded.isSeasonOpen)
        assertTrue(seasonLoaded.isSeasonOpen)
        assertEquals("episode-1", seasonLoaded.seasonEpisodes?.episodes?.single()?.id)
        assertTrue(backToDetail.isOpen)
        assertFalse(backToDetail.isSeasonOpen)
        assertFalse(closed.isOpen)
    }
}
