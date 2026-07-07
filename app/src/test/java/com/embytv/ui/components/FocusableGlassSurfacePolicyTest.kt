package com.embytv.ui.components

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FocusableGlassSurfacePolicyTest {
    @Test
    fun disabledControlsWithReasonRemainFocusableAndExposeContentFocus() {
        val state = FocusableGlassSurfacePolicy.resolve(
            focused = true,
            enabled = false,
            disabledReason = "没有下一集",
        )

        assertTrue(state.canFocus)
        assertTrue(state.contentFocused)
        assertFalse(state.scaleFocused)
    }

    @Test
    fun disabledControlsWithoutReasonAreNotFocusable() {
        val state = FocusableGlassSurfacePolicy.resolve(
            focused = true,
            enabled = false,
            disabledReason = null,
        )

        assertFalse(state.canFocus)
        assertFalse(state.contentFocused)
        assertFalse(state.scaleFocused)
    }

    @Test
    fun enabledFocusedControlsExposeContentFocusAndScaleFocus() {
        val state = FocusableGlassSurfacePolicy.resolve(
            focused = true,
            enabled = true,
            disabledReason = null,
        )

        assertTrue(state.canFocus)
        assertTrue(state.contentFocused)
        assertTrue(state.scaleFocused)
    }
}
