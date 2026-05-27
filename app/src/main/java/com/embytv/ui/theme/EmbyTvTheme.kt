package com.embytv.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme

object CinematicGlassColors {
    val Background = Color(0xFF131313)
    val Surface = Color(0xFF201F1F)
    val SurfaceHigh = Color(0xFF353534)
    val Glass = Color(0x14FFFFFF)
    val GlassStrong = Color(0x26000000)
    val OnSurface = Color(0xFFE5E2E1)
    val OnSurfaceVariant = Color(0xFFBECAB7)
    val Primary = Color(0xFF78DD6D)
    val OnPrimary = Color(0xFF003A04)
    val PrimaryContainer = Color(0xFF52B54B)
    val Secondary = Color(0xFFE9C349)
    val Error = Color(0xFFFFB4AB)
    val Outline = Color(0xFF899483)
    val OutlineVariant = Color(0xFF3F4A3B)
}

object CinematicGlassSpacing {
    val SafeAreaX = 80.dp
    val SafeAreaY = 60.dp
    val Gutter = 32.dp
    val CardGap = 24.dp
    val SidebarWidth = 320.dp
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun EmbyTvTheme(content: @Composable () -> Unit) {
    MaterialTheme(content = content)
}
