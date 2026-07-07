package com.embytv.ui.components

import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteHintMotionPolicyTest {
    @Test
    fun remoteHintMotionIsShorterThanFullOsdTransition() {
        val spec = RemoteHintMotionPolicy.TvFeedback

        assertTrue(spec.enterDurationMs <= 120)
        assertTrue(spec.exitDurationMs <= 100)
    }

    @Test
    fun remoteHintMotionUsesVerySmallVerticalOffset() {
        val spec = RemoteHintMotionPolicy.TvFeedback

        assertTrue(spec.verticalOffsetPx <= 12)
        assertTrue(spec.verticalOffsetPx >= 0)
    }
}
