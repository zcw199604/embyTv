package com.embytv.ui.home

import com.embytv.domain.model.MediaItemSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeDashboardMapperTest {
    @Test
    fun mapsItemsIntoLibraryAndContinueWatchingCards() {
        val items = listOf(
            MediaItemSummary(
                id = "movie-1",
                name = "Interstellar",
                type = "Movie",
                overview = "Space exploration",
                imageUrl = "https://example.test/interstellar.jpg",
            ),
            MediaItemSummary(
                id = "episode-1",
                name = "The Void",
                type = "Episode",
                overview = "S1 E4",
                imageUrl = null,
            ),
        )

        val dashboard = HomeDashboardMapper.map(items)

        assertEquals(3, dashboard.libraries.size)
        assertEquals("Movies", dashboard.libraries.first().title)
        assertEquals("1 item", dashboard.libraries.first().countLabel)
        assertEquals(2, dashboard.continueWatching.size)
        assertEquals("Interstellar", dashboard.continueWatching.first().title)
        assertEquals("Movie", dashboard.continueWatching.first().badge)
        assertEquals("https://example.test/interstellar.jpg", dashboard.continueWatching.first().imageUrl)
    }

    @Test
    fun marksUnimplementedNavigationDestinationsDisabled() {
        val dashboard = HomeDashboardMapper.map(emptyList())

        assertTrue(dashboard.navigationItems.first { it.id == HomeNavigationId.Home }.enabled)
        assertFalse(dashboard.navigationItems.first { it.id == HomeNavigationId.Movies }.enabled)
        assertFalse(dashboard.navigationItems.first { it.id == HomeNavigationId.Settings }.enabled)
        assertEquals("Movies 暂未支持", dashboard.navigationItems.first { it.id == HomeNavigationId.Movies }.disabledReason)
    }

    @Test
    fun marksLibraryCardsWithoutDestinationUnsupported() {
        val dashboard = HomeDashboardMapper.map(
            listOf(
                MediaItemSummary(
                    id = "movie-1",
                    name = "Interstellar",
                    type = "Movie",
                    overview = null,
                    imageUrl = null,
                ),
            ),
        )

        val movies = dashboard.libraries.first { it.id == "movies" }
        val anime = dashboard.libraries.first { it.id == "anime" }

        assertFalse(movies.enabled)
        assertEquals("媒体库详情暂未支持", movies.disabledReason)
        assertFalse(anime.enabled)
        assertEquals("Anime 暂未支持", anime.disabledReason)
    }

    @Test
    fun closesDrawerOnBackWhenOpen() {
        val open = DrawerUiState(isOpen = true)

        val closed = open.onBack()

        assertFalse(closed.isOpen)
        assertTrue(closed.restoreMenuFocus)
    }
}
