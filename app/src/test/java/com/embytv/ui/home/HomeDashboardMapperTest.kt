package com.embytv.ui.home

import com.embytv.domain.model.EmbyHomeDashboard
import com.embytv.domain.model.EmbyLibraryLatestSection
import com.embytv.domain.model.EmbyLibrarySummary
import com.embytv.domain.model.MediaItemSummary
import com.embytv.domain.model.SavedEmbyCredential
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeDashboardMapperTest {
    @Test
    fun mapsRealEmbyViewsAndResumeItems() {
        val dashboard = HomeDashboardMapper.map(
            EmbyHomeDashboard(
                libraries = listOf(
                    EmbyLibrarySummary(
                        id = "9",
                        name = "电影",
                        type = "CollectionFolder",
                        collectionType = "movies",
                        itemCount = 1001,
                        imageUrl = "https://example.test/library.jpg",
                    ),
                ),
                resumeItems = listOf(
                    MediaItemSummary(
                        id = "episode-1",
                        name = "第 1 集",
                        type = "Episode",
                        overview = "真实简介",
                        imageUrl = "https://example.test/episode.jpg",
                        seriesName = "真实剧集",
                        seasonName = "Season 1",
                        parentIndexNumber = 1,
                        indexNumber = 1,
                        runTimeTicks = 10_000,
                        playbackPositionTicks = 2_500,
                        playedPercentage = 25.0,
                        productionYear = 2026,
                    ),
                ),
                latestItems = emptyList(),
            ),
        )

        assertEquals(1, dashboard.libraries.size)
        assertEquals("电影", dashboard.libraries.first().title)
        assertEquals("1001 items", dashboard.libraries.first().countLabel)
        assertEquals("https://example.test/library.jpg", dashboard.libraries.first().imageUrl)
        assertTrue(dashboard.libraries.first().enabled)
        assertEquals(null, dashboard.libraries.first().disabledReason)
        assertEquals(1, dashboard.continueWatching.size)
        assertEquals("第 1 集", dashboard.continueWatching.first().title)
        assertEquals("真实剧集 · S01E01", dashboard.continueWatching.first().subtitle)
        assertEquals(0.25f, dashboard.continueWatching.first().progressFraction)
        assertEquals("Episode", dashboard.continueWatching.first().badge)
    }

    @Test
    fun mapsLibraryLatestSectionsWithRealThumbnailsAndEpisodeContext() {
        val dashboard = HomeDashboardMapper.map(
            EmbyHomeDashboard(
                libraries = listOf(
                    EmbyLibrarySummary(
                        id = "library-1",
                        name = "电视剧",
                        type = "CollectionFolder",
                        collectionType = "tvshows",
                        itemCount = 88,
                        imageUrl = "https://example.test/library.jpg",
                    ),
                ),
                libraryLatestSections = listOf(
                    EmbyLibraryLatestSection(
                        library = EmbyLibrarySummary(
                            id = "library-1",
                            name = "电视剧",
                            type = "CollectionFolder",
                            collectionType = "tvshows",
                            itemCount = 88,
                            imageUrl = "https://example.test/library.jpg",
                        ),
                        items = listOf(
                            MediaItemSummary(
                                id = "episode-2",
                                name = "第 2 集",
                                type = "Episode",
                                overview = null,
                                imageUrl = "https://example.test/poster.jpg",
                                thumbImageUrl = "https://example.test/thumb.jpg",
                                backdropImageUrl = "https://example.test/backdrop.jpg",
                                seriesName = "真实剧集",
                                seasonName = "第一季",
                                parentIndexNumber = 1,
                                indexNumber = 2,
                            ),
                        ),
                    ),
                ),
            ),
        )

        assertEquals(1, dashboard.libraryLatestSections.size)
        assertEquals("电视剧 · 最新入库", dashboard.libraryLatestSections.single().title)
        assertEquals("episode-2", dashboard.libraryLatestSections.single().items.single().id)
        assertEquals("https://example.test/thumb.jpg", dashboard.libraryLatestSections.single().items.single().imageUrl)
        assertEquals("真实剧集 · S01E02", dashboard.libraryLatestSections.single().items.single().subtitle)
    }

    @Test
    fun mapsSeriesCardsWithPosterAndUnplayedCornerBadge() {
        val dashboard = HomeDashboardMapper.map(
            EmbyHomeDashboard(
                libraries = listOf(
                    EmbyLibrarySummary(
                        id = "library-1",
                        name = "电视剧",
                        type = "CollectionFolder",
                        collectionType = "tvshows",
                        itemCount = 88,
                        imageUrl = "https://example.test/library.jpg",
                    ),
                ),
                libraryLatestSections = listOf(
                    EmbyLibraryLatestSection(
                        library = EmbyLibrarySummary(
                            id = "library-1",
                            name = "电视剧",
                            type = "CollectionFolder",
                            collectionType = "tvshows",
                            itemCount = 88,
                            imageUrl = "https://example.test/library.jpg",
                        ),
                        items = listOf(
                            MediaItemSummary(
                                id = "series-1",
                                name = "真实剧集",
                                type = "Series",
                                overview = "剧集简介",
                                imageUrl = "https://example.test/series.jpg",
                                unplayedItemCount = 3,
                                productionYear = 2026,
                            ),
                            MediaItemSummary(
                                id = "movie-1",
                                name = "真实电影",
                                type = "Movie",
                                overview = null,
                                imageUrl = "https://example.test/movie.jpg",
                                unplayedItemCount = 1,
                                productionYear = 2026,
                            ),
                        ),
                    ),
                ),
            ),
        )

        val cards = dashboard.libraryLatestSections.single().items
        assertEquals("真实剧集", cards[0].title)
        assertEquals("https://example.test/series.jpg", cards[0].imageUrl)
        assertEquals("剩 3 集", cards[0].cornerBadge)
        assertEquals(null, cards[1].cornerBadge)
    }

    @Test
    fun fallsBackToLatestWhenResumeIsEmpty() {
        val dashboard = HomeDashboardMapper.map(
            EmbyHomeDashboard(
                libraries = emptyList(),
                resumeItems = emptyList(),
                latestItems = listOf(
                    MediaItemSummary(
                        id = "movie-1",
                        name = "新电影",
                        type = "Movie",
                        overview = null,
                        imageUrl = null,
                        productionYear = 2026,
                    ),
                ),
            ),
        )

        assertEquals("最近入库", dashboard.mediaSectionTitle)
        assertEquals("新电影", dashboard.continueWatching.single().title)
        assertEquals(0f, dashboard.continueWatching.single().progressFraction)
    }

    @Test
    fun dashboardDoesNotCreateHardcodedLibraries() {
        val dashboard = HomeDashboardMapper.map(EmbyHomeDashboard())

        assertEquals(0, dashboard.libraries.size)
        assertFalse(dashboard.libraries.any { it.title == "Anime" })
        assertFalse(dashboard.libraries.any { it.title == "Movies" })
    }

    @Test
    fun closesDrawerOnBackWhenOpen() {
        val open = DrawerUiState(isOpen = true)

        val closed = open.onBack()

        assertFalse(closed.isOpen)
        assertTrue(closed.restoreMenuFocus)
    }

    @Test
    fun closesLibraryContentOnBackWhenOpen() {
        val state = LibraryContentUiState(
            selectedLibraryId = "library-1",
            content = null,
            isLoading = true,
            errorMessage = null,
        )

        val closed = state.close()

        assertEquals(null, closed.selectedLibraryId)
        assertFalse(closed.isLoading)
        assertEquals(null, closed.errorMessage)
    }

    @Test
    fun confirmationStateCarriesDeleteCredentialWithoutPassword() {
        val credential = SavedEmbyCredential(
            serverUrl = "http://emby.test",
            userId = "user-1",
            username = "wm",
            accessToken = "token-value",
            serverId = "server-1",
            deviceId = "device-1",
            savedAtEpochMillis = 1L,
        )

        val state = HomeUiState(
            confirmation = HomeConfirmationUiState(
                kind = HomeConfirmationKind.DeleteCredential,
                title = "删除保存身份",
                message = "确认删除 wm",
                confirmLabel = "确认删除",
                credential = credential,
            ),
        )

        assertEquals(HomeConfirmationKind.DeleteCredential, state.confirmation?.kind)
        assertEquals("wm", state.confirmation?.credential?.username)
        assertFalse(state.confirmation?.message.orEmpty().contains("token-value"))
    }

    @Test
    fun searchStateIgnoresResultWhenCurrentQueryChanged() {
        val loading = SearchUiState(isOpen = true, query = "ab").loading("a")
        val current = loading.copy(query = "ab")
        val staleQuery = "a"
        val updated = if (current.query.trim() == staleQuery) {
            current.loaded(com.embytv.domain.model.EmbySearchResults(query = staleQuery))
        } else {
            current
        }

        assertEquals("ab", updated.query)
        assertTrue(updated.isLoading)
    }
}
