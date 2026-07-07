package com.embytv.ui.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlayerDetailProviderIdsLabelTest {
    @Test
    fun providerIdsPreferStableImdbThenDoubanOrderAndIgnoreCase() {
        val label = PlayerDetailProviderIdsLabelResolver.resolve(
            mapOf(
                "douban" to "35267208",
                "IMDB" to "tt1234567",
                "Tmdb" to "999",
            ),
        )

        assertEquals("IMDb tt1234567 · Douban 35267208", label)
    }

    @Test
    fun providerIdsSkipBlankValuesAndUnknownProviders() {
        val label = PlayerDetailProviderIdsLabelResolver.resolve(
            mapOf(
                "Imdb" to " ",
                "Tvdb" to "tvdb-1",
                "Douban" to "douban-1",
            ),
        )

        assertEquals("Douban douban-1", label)
    }

    @Test
    fun providerIdsReturnNullWhenNoDisplayableProviderExists() {
        val label = PlayerDetailProviderIdsLabelResolver.resolve(
            mapOf(
                "Tmdb" to "tmdb-1",
                "Imdb" to "",
            ),
        )

        assertNull(label)
    }
}
