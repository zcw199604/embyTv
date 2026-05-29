package com.embytv.ui.components.preview

// 提供核心 TV 组件的 Compose Preview，便于在 IDE 中快速检查主题和焦点组件外观。
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.embytv.ui.components.FocusableGlassSurface
import com.embytv.ui.components.GlassPanel
import com.embytv.ui.components.PrimaryTvButton
import com.embytv.ui.theme.AppThemeId
import com.embytv.ui.theme.EmbyTvTheme
import com.embytv.ui.theme.ThemePreferences
import androidx.tv.material3.Text

@Preview(widthDp = 960, heightDp = 540)
@Composable
private fun ComponentLibraryPreview() {
    EmbyTvTheme(preferences = ThemePreferences(themeId = AppThemeId.CinematicGlass)) {
        Column(
            modifier = Modifier.padding(32.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            GlassPanel {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("GlassPanel")
                    Text("Cinematic Glass 主题下的基础面板")
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PrimaryTvButton(text = "播放", onClick = {})
                FocusableGlassSurface(onClick = {}, cornerRadius = 999.dp) {
                    Text("Focusable Chip", modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp))
                }
            }
        }
    }
}
