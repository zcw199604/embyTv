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
    }
}
