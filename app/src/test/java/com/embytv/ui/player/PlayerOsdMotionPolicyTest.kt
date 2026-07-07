package com.embytv.ui.player

import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerOsdMotionPolicyTest {
    @Test
    fun tvDefaultMotionKeepsOsdTransitionsShortForWeakTvHardware() {
        val spec = PlayerOsdMotionPolicy.TvDefault

        assertTrue(spec.enterDurationMs <= 180)
        assertTrue(spec.exitDurationMs <= 150)
    }

    @Test
    fun tvDefaultMotionUsesSmallSlideDistance() {
        val spec = PlayerOsdMotionPolicy.TvDefault

        assertTrue(spec.slideDistanceFraction <= 0.03f)
        assertTrue(spec.enterOffsetPx(fullHeight = 1080) <= 32)
        assertTrue(spec.exitOffsetPx(fullHeight = 1080) <= 32)
    }
}
