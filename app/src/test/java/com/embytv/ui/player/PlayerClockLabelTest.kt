package com.embytv.ui.player

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class PlayerClockLabelTest {
    @Test
    fun clockLabelUsesStableWesternDigitsAcrossLocales() {
        val previousLocale = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("ar-EG"))

            assertEquals("01:05", 65_000L.toClockLabel())
            assertEquals("1:01:05", 3_665_000L.toClockLabel())
        } finally {
            Locale.setDefault(previousLocale)
        }
    }
}
