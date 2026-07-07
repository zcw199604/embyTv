package com.embytv.ui.home

import com.embytv.ui.theme.AppLanguage
import com.embytv.ui.theme.AppThemeId
import com.embytv.ui.theme.ThemePreferences
import org.junit.Assert.assertEquals
import org.junit.Test

class HomeThemePreferenceObserverTest {
    @Test
    fun applyPreferencesUpdatesLanguageInHomeUiState() {
        val state = HomeUiState()
        val preferences = ThemePreferences(
            themeId = AppThemeId.EmbyClassic,
            language = AppLanguage.English,
        )

        val updated = HomeThemePreferenceObserver.apply(state, preferences)

        assertEquals(AppLanguage.English, updated.themePreferences.language)
        assertEquals(AppThemeId.EmbyClassic, updated.themePreferences.themeId)
    }
}
