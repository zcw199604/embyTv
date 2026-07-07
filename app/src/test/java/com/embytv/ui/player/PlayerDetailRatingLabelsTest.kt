package com.embytv.ui.player

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class PlayerDetailRatingLabelsTest {
    @Test
    fun detailRatingLabelsUseStableTechnicalDecimalSeparatorAcrossLocales() {
        val previousLocale = Locale.getDefault()
        try {
            Locale.setDefault(Locale.FRANCE)

            assertEquals("8.6", 8.6.toCommunityRatingLabel())
            assertEquals("92", 92.0.toCriticRatingLabel())
        } finally {
            Locale.setDefault(previousLocale)
        }
    }

    @Test
    fun officialRatingLabelUsesLocalizedPrefixAndSkipsBlankValues() {
        assertEquals("分级 PG-13", "PG-13".toOfficialRatingLabel("分级 %1\$s"))
        assertEquals("Rated TV-MA", "TV-MA".toOfficialRatingLabel("Rated %1\$s"))
        assertEquals(null, " ".toOfficialRatingLabel("分级 %1\$s"))
    }
}
