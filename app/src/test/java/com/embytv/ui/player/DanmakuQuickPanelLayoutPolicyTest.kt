package com.embytv.ui.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DanmakuQuickPanelLayoutPolicyTest {
    @Test
    fun tvDefaultLayoutSplitsNineControlsIntoRows() {
        val rows = DanmakuQuickPanelLayoutPolicy.TvDefault.rows

        assertEquals(4, rows.size)
        assertEquals(9, rows.flatten().size)
        assertTrue(rows.all { it.size <= 3 })
    }

    @Test
    fun tvDefaultLayoutKeepsRelatedControlsTogether() {
        val rows = DanmakuQuickPanelLayoutPolicy.TvDefault.rows

        assertEquals(
            listOf(DanmakuQuickOption.Enabled, DanmakuQuickOption.Disabled),
            rows[0],
        )
        assertEquals(
            listOf(DanmakuQuickOption.Opacity60, DanmakuQuickOption.Opacity100),
            rows[1],
        )
        assertEquals(
            listOf(
                DanmakuQuickOption.TextSmall,
                DanmakuQuickOption.TextNormal,
                DanmakuQuickOption.TextLarge,
            ),
            rows[2],
        )
        assertEquals(
            listOf(DanmakuQuickOption.AreaTop, DanmakuQuickOption.AreaFull),
            rows[3],
        )
    }
}
