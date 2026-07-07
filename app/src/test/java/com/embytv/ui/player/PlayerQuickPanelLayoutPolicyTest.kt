package com.embytv.ui.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerQuickPanelLayoutPolicyTest {
    @Test
    fun tvDefaultLayoutSplitsTrackOptionsIntoShortRows() {
        val rows = PlayerQuickPanelLayoutPolicy.TvDefault.rowsFor((1..7).toList())

        assertEquals(listOf(listOf(1, 2, 3), listOf(4, 5, 6), listOf(7)), rows)
        assertTrue(rows.all { it.size <= 3 })
    }

    @Test
    fun tvDefaultLayoutSplitsSpeedOptionsIntoTwoRows() {
        val rows = PlayerQuickPanelLayoutPolicy.TvDefault.rowsFor(
            listOf("0.5x", "0.75x", "1x", "1.25x", "1.5x", "2x"),
        )

        assertEquals(2, rows.size)
        assertTrue(rows.all { it.size <= 3 })
    }

    @Test
    fun tvDefaultLayoutKeepsEmptyPanelsEmpty() {
        val rows = PlayerQuickPanelLayoutPolicy.TvDefault.rowsFor(emptyList<String>())

        assertTrue(rows.isEmpty())
    }
}
