package com.embytv.ui.home

import com.embytv.ui.theme.ThemePreferences

internal object HomeThemePreferenceObserver {
    fun apply(
        state: HomeUiState,
        preferences: ThemePreferences,
    ): HomeUiState =
        state.copy(themePreferences = preferences)
}
