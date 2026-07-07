package com.embytv.ui.player

import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerStartupPositionPolicyTest {
    @Test
    fun keepsPositiveResumePositionAsStartupPosition() {
        assertEquals(45_000L, PlayerStartupPositionPolicy.normalize(45_000L))
    }

    @Test
    fun clampsNegativeResumePositionToBeginning() {
        assertEquals(0L, PlayerStartupPositionPolicy.normalize(-1_000L))
    }
}
