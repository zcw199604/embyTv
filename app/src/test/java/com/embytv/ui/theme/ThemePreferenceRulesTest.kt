package com.embytv.ui.theme

// 覆盖主题和可访问性偏好的纯规则，避免持久化实现变更影响业务约束。
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ThemePreferenceRulesTest {
    @Test
    fun defaultPreferencesUseCinematicNormalSettings() {
        val preferences = ThemePreferences()

        assertEquals(AppThemeId.CinematicGlass, preferences.themeId)
        assertEquals(FontScale.Normal, preferences.fontScale)
        assertFalse(preferences.highContrast)
    }

    @Test
    fun invalidThemeFallsBackToCinematicGlass() {
        assertEquals(AppThemeId.CinematicGlass, AppThemeId.fromStorageValue("missing"))
    }

    @Test
    fun invalidFontScaleFallsBackToNormal() {
        assertEquals(FontScale.Normal, FontScale.fromStorageValue("huge"))
    }

    @Test
    fun highContrastOverridesThemeColorScheme() {
        val scheme = ThemePreferenceRules.resolveColorScheme(
            ThemePreferences(themeId = AppThemeId.EmbyClassic, highContrast = true),
        )

        assertEquals(EmbyColorSchemes.HighContrast.background, scheme.background)
        assertEquals(EmbyColorSchemes.HighContrast.onSurface, scheme.onSurface)
    }
}
