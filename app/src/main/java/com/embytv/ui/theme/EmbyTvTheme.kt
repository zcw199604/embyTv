package com.embytv.ui.theme

// 定义 Emby TV 的主题令牌、主题偏好规则和兼容式颜色访问入口。
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme

enum class AppThemeId(val storageValue: String, val displayName: String) {
    CinematicGlass("cinematic_glass", "Cinematic Glass"),
    DarkMinimal("dark_minimal", "Dark Minimal"),
    EmbyClassic("emby_classic", "Emby Classic"),

    ;

    companion object {
        fun fromStorageValue(value: String?): AppThemeId =
            entries.firstOrNull { it.storageValue == value } ?: CinematicGlass
    }
}

enum class FontScale(val storageValue: String, val displayName: String, val scale: Float) {
    Small("small", "小", 0.9f),
    Normal("normal", "标准", 1.0f),
    Large("large", "大", 1.15f),
    ExtraLarge("extra_large", "超大", 1.3f),

    ;

    companion object {
        fun fromStorageValue(value: String?): FontScale =
            entries.firstOrNull { it.storageValue == value } ?: Normal
    }
}

enum class AppLanguage(val storageValue: String, val displayName: String, val localeTag: String?) {
    System("system", "跟随系统", null),
    SimplifiedChinese("zh-CN", "简体中文", "zh-CN"),
    English("en", "English", "en"),

    ;

    companion object {
        fun fromStorageValue(value: String?): AppLanguage =
            entries.firstOrNull { it.storageValue == value } ?: System
    }
}

data class ThemePreferences(
    val themeId: AppThemeId = AppThemeId.CinematicGlass,
    val highContrast: Boolean = false,
    val fontScale: FontScale = FontScale.Normal,
    val language: AppLanguage = AppLanguage.System,
)

data class EmbyColorScheme(
    val background: Color,
    val surface: Color,
    val surfaceHigh: Color,
    val glass: Color,
    val glassStrong: Color,
    val onSurface: Color,
    val onSurfaceVariant: Color,
    val onSurfaceMedium: Color,
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val secondary: Color,
    val error: Color,
    val outline: Color,
    val outlineVariant: Color,
)

object EmbyColorSchemes {
    val CinematicGlass = EmbyColorScheme(
        background = Color(0xFF131313),
        surface = Color(0xFF201F1F),
        surfaceHigh = Color(0xFF353534),
        glass = Color(0x14FFFFFF),
        glassStrong = Color(0x26000000),
        onSurface = Color(0xFFE5E2E1),
        onSurfaceVariant = Color(0xFFBECAB7),
        onSurfaceMedium = Color(0xFFD0CCC9),
        primary = Color(0xFF78DD6D),
        onPrimary = Color(0xFF003A04),
        primaryContainer = Color(0xFF52B54B),
        secondary = Color(0xFFE9C349),
        error = Color(0xFFFFB4AB),
        outline = Color(0xFF899483),
        outlineVariant = Color(0xFF3F4A3B),
    )

    val DarkMinimal = EmbyColorScheme(
        background = Color(0xFF090A0C),
        surface = Color(0xFF15171A),
        surfaceHigh = Color(0xFF24282D),
        glass = Color(0x1FFFFFFF),
        glassStrong = Color(0x33000000),
        onSurface = Color(0xFFF0F2F4),
        onSurfaceVariant = Color(0xFFB5BDC6),
        onSurfaceMedium = Color(0xFFD5D9DE),
        primary = Color(0xFF7FD7FF),
        onPrimary = Color(0xFF003142),
        primaryContainer = Color(0xFF1A6988),
        secondary = Color(0xFFFFD36B),
        error = Color(0xFFFFB4AB),
        outline = Color(0xFF83909B),
        outlineVariant = Color(0xFF323A42),
    )

    val EmbyClassic = EmbyColorScheme(
        background = Color(0xFF101710),
        surface = Color(0xFF1B261C),
        surfaceHigh = Color(0xFF304232),
        glass = Color(0x1A78DD6D),
        glassStrong = Color(0x2A000000),
        onSurface = Color(0xFFEAF5E7),
        onSurfaceVariant = Color(0xFFC4D8BF),
        onSurfaceMedium = Color(0xFFD8E7D3),
        primary = Color(0xFF52B54B),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFF367D32),
        secondary = Color(0xFFE9C349),
        error = Color(0xFFFFB4AB),
        outline = Color(0xFF8AA184),
        outlineVariant = Color(0xFF43583F),
    )

    val HighContrast = EmbyColorScheme(
        background = Color.Black,
        surface = Color(0xFF111111),
        surfaceHigh = Color(0xFF242424),
        glass = Color(0x33000000),
        glassStrong = Color(0x66000000),
        onSurface = Color.White,
        onSurfaceVariant = Color(0xFFE8E8E8),
        onSurfaceMedium = Color.White,
        primary = Color(0xFF00FF66),
        onPrimary = Color.Black,
        primaryContainer = Color(0xFF00CC55),
        secondary = Color(0xFFFFFF00),
        error = Color(0xFFFF4D4D),
        outline = Color.White,
        outlineVariant = Color(0xFFBDBDBD),
    )
}

object ThemePreferenceRules {
    fun resolveColorScheme(preferences: ThemePreferences): EmbyColorScheme =
        if (preferences.highContrast) {
            EmbyColorSchemes.HighContrast
        } else {
            when (preferences.themeId) {
                AppThemeId.CinematicGlass -> EmbyColorSchemes.CinematicGlass
                AppThemeId.DarkMinimal -> EmbyColorSchemes.DarkMinimal
                AppThemeId.EmbyClassic -> EmbyColorSchemes.EmbyClassic
            }
        }
}

val LocalEmbyColorScheme = compositionLocalOf { EmbyColorSchemes.CinematicGlass }
val LocalEmbyFontScale = compositionLocalOf { FontScale.Normal }

object CinematicGlassColors {
    val Background: Color @Composable get() = LocalEmbyColorScheme.current.background
    val Surface: Color @Composable get() = LocalEmbyColorScheme.current.surface
    val SurfaceHigh: Color @Composable get() = LocalEmbyColorScheme.current.surfaceHigh
    val Glass: Color @Composable get() = LocalEmbyColorScheme.current.glass
    val GlassStrong: Color @Composable get() = LocalEmbyColorScheme.current.glassStrong
    val OnSurface: Color @Composable get() = LocalEmbyColorScheme.current.onSurface
    val OnSurfaceVariant: Color @Composable get() = LocalEmbyColorScheme.current.onSurfaceVariant
    val OnSurfaceMedium: Color @Composable get() = LocalEmbyColorScheme.current.onSurfaceMedium
    val InfoText: Color @Composable get() = OnSurfaceVariant
    val MetadataText: Color @Composable get() = OnSurfaceMedium
    val DisabledText: Color @Composable get() = OnSurfaceVariant.copy(alpha = 0.55f)
    val Primary: Color @Composable get() = LocalEmbyColorScheme.current.primary
    val OnPrimary: Color @Composable get() = LocalEmbyColorScheme.current.onPrimary
    val PrimaryContainer: Color @Composable get() = LocalEmbyColorScheme.current.primaryContainer
    val Secondary: Color @Composable get() = LocalEmbyColorScheme.current.secondary
    val Error: Color @Composable get() = LocalEmbyColorScheme.current.error
    val ErrorContainer: Color @Composable get() = Error.copy(alpha = 0.12f)
    val OnErrorContainer: Color @Composable get() = Error
    val Outline: Color @Composable get() = LocalEmbyColorScheme.current.outline
    val OutlineVariant: Color @Composable get() = LocalEmbyColorScheme.current.outlineVariant
}

object CinematicGlassSpacing {
    val SafeAreaX = 80.dp
    val SafeAreaY = 60.dp
    val Gutter = 32.dp
    val CardGap = 24.dp
    val SidebarWidth = 320.dp
    val IconButtonSize = 44.dp
    val IconButtonLargeSize = 58.dp
    val IconButtonPrimarySize = 76.dp
    val ProgressRailHeight = 8.dp
    val ProgressRailHeightCompact = 4.dp
    val ErrorIconSize = 56.dp
    val PlaceholderIconSize = 72.dp
    val PlaceholderIconSizeCompact = 42.dp
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun EmbyTvTheme(
    preferences: ThemePreferences = ThemePreferences(),
    content: @Composable () -> Unit,
) {
    androidx.compose.runtime.CompositionLocalProvider(
        LocalEmbyColorScheme provides ThemePreferenceRules.resolveColorScheme(preferences),
        LocalEmbyFontScale provides preferences.fontScale,
    ) {
        MaterialTheme(content = content)
    }
}
