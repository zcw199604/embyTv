package com.embytv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import com.embytv.ui.EmbyTvApp
import com.embytv.ui.theme.ThemePreferences
import com.embytv.ui.theme.EmbyTvTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val appContainer = (application as EmbyTvApplication).container
        setContent {
            val themePreferences by appContainer.themePreferenceStore.preferencesFlow
                .collectAsState(initial = ThemePreferences())
            EmbyTvTheme(preferences = themePreferences) {
                EmbyTvApp(container = appContainer)
            }
        }
    }
}
