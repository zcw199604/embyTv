package com.embytv.data.local

// 持久化 TV 主题和可访问性偏好，供应用启动和设置页即时同步。
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.embytv.ui.theme.AppThemeId
import com.embytv.ui.theme.FontScale
import com.embytv.ui.theme.ThemePreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.themePreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "theme_preferences",
)

class ThemePreferenceStore(private val context: Context) {
    val preferencesFlow: Flow<ThemePreferences> = context.themePreferencesDataStore.data
        .map { preferences ->
            ThemePreferences(
                themeId = AppThemeId.fromStorageValue(preferences[THEME_KEY]),
                highContrast = preferences[HIGH_CONTRAST_KEY] ?: false,
                fontScale = FontScale.fromStorageValue(preferences[FONT_SCALE_KEY]),
            )
        }

    suspend fun setTheme(themeId: AppThemeId) {
        context.themePreferencesDataStore.edit { preferences ->
            preferences[THEME_KEY] = themeId.storageValue
        }
    }

    suspend fun setHighContrast(enabled: Boolean) {
        context.themePreferencesDataStore.edit { preferences ->
            preferences[HIGH_CONTRAST_KEY] = enabled
        }
    }

    suspend fun setFontScale(fontScale: FontScale) {
        context.themePreferencesDataStore.edit { preferences ->
            preferences[FONT_SCALE_KEY] = fontScale.storageValue
        }
    }

    companion object {
        private val THEME_KEY = stringPreferencesKey("theme_id")
        private val HIGH_CONTRAST_KEY = booleanPreferencesKey("high_contrast")
        private val FONT_SCALE_KEY = stringPreferencesKey("font_scale")
    }
}
