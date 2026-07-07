package com.embytv.ui.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OsdFocusVisualsTest {
    @Test
    fun focusedControlsExposeVisibleContainerHighlight() {
        val visuals = OsdFocusVisualResolver.resolve(
            OsdFocusVisualState(focused = true),
        )

        assertTrue(visuals.containerHighlighted)
        assertTrue(visuals.emphasizedLabel)
        assertEquals(OsdFocusTone.OnSurface, visuals.foregroundTone)
    }

    @Test
    fun selectedControlsUsePrimaryToneAndIndicatorEvenWhenNotFocused() {
        val visuals = OsdFocusVisualResolver.resolve(
            OsdFocusVisualState(selected = true),
        )

        assertTrue(visuals.selectedIndicator)
        assertEquals(OsdFocusTone.Primary, visuals.foregroundTone)
        assertFalse(visuals.containerHighlighted)
    }

    @Test
    fun disabledControlsCanKeepFocusFeedbackButDimContent() {
        val visuals = OsdFocusVisualResolver.resolve(
            OsdFocusVisualState(focused = true, enabled = false),
        )

        assertTrue(visuals.containerHighlighted)
        assertEquals(OsdFocusTone.Disabled, visuals.foregroundTone)
        assertEquals(0.56f, visuals.contentAlpha)
    }

    @Test
    fun primaryControlsKeepOnPrimaryToneAndStrongIndicator() {
        val visuals = OsdFocusVisualResolver.resolve(
            OsdFocusVisualState(primary = true, focused = true),
        )

        assertTrue(visuals.containerHighlighted)
        assertTrue(visuals.selectedIndicator)
        assertEquals(OsdFocusTone.OnPrimary, visuals.foregroundTone)
        assertEquals(1f, visuals.contentAlpha)
    }
}
