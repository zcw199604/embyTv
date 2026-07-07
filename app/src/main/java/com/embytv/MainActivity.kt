package com.embytv

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import com.embytv.ui.EmbyTvApp
import com.embytv.ui.theme.AppLanguage
import com.embytv.ui.theme.ThemePreferences
import com.embytv.ui.theme.EmbyTvTheme
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val appContainer = (application as EmbyTvApplication).container
        setContent {
            val themePreferences by appContainer.themePreferenceStore.preferencesFlow
                .collectAsState(initial = ThemePreferences())
            LocalizedApp(language = themePreferences.language) {
                EmbyTvTheme(preferences = themePreferences) {
                    EmbyTvApp(container = appContainer)
                }
            }
        }
    }
}

@Composable
private fun LocalizedApp(
    language: AppLanguage,
    content: @Composable () -> Unit,
) {
    val baseContext = LocalContext.current
    val baseConfiguration = LocalConfiguration.current
    val localizedContext = remember(baseContext, baseConfiguration, language) {
        baseContext.localized(language, baseConfiguration)
    }
    val localizedConfiguration = remember(baseConfiguration, language) {
        Configuration(localizedContext.resources.configuration)
    }
    CompositionLocalProvider(
        LocalContext provides localizedContext,
        LocalConfiguration provides localizedConfiguration,
        content = content,
    )
}

private fun Context.localized(language: AppLanguage, baseConfiguration: Configuration): Context {
    val localeTag = language.localeTag ?: return this
    val locale = Locale.forLanguageTag(localeTag)
    Locale.setDefault(locale)
    val configuration = Configuration(baseConfiguration)
    configuration.setLocale(locale)
    return createConfigurationContext(configuration)
}
