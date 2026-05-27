package com.embytv.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.embytv.domain.model.PlaybackSource
import com.embytv.ui.components.GlassPanel
import com.embytv.ui.components.LibraryCard
import com.embytv.ui.components.MediaPosterCard
import com.embytv.ui.components.NavigationDrawerPanel
import com.embytv.ui.components.PrimaryTvButton
import com.embytv.ui.components.RemoteHint
import com.embytv.ui.components.TopChromeBar
import com.embytv.ui.setup.SetupScreen
import com.embytv.ui.theme.CinematicGlassColors
import com.embytv.ui.theme.CinematicGlassSpacing

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onPlay: (PlaybackSource) -> Unit,
    onPlaySample: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()

    if (state.session == null) {
        SetupScreen(
            state = state,
            onServerUrlChange = viewModel::updateServerUrl,
            onUsernameChange = viewModel::updateUsername,
            onPasswordChange = viewModel::updatePassword,
            onConnect = viewModel::connect,
            onPlaySample = onPlaySample,
        )
    } else {
        HomeDashboardScreen(
            state = state,
            onPlay = { item ->
                viewModel.createPlaybackSource(item)?.let(onPlay)
            },
            onPlaySample = onPlaySample,
        )
    }
}

@Composable
private fun HomeDashboardScreen(
    state: HomeUiState,
    onPlay: (com.embytv.domain.model.MediaItemSummary) -> Unit,
    onPlaySample: () -> Unit,
) {
    val dashboard = remember(state.items) { HomeDashboardMapper.map(state.items) }
    var drawerState by remember { mutableStateOf(DrawerUiState()) }
    var hintMessage by remember { mutableStateOf<String?>(null) }
    val menuFocusRequester = remember { FocusRequester() }

    LaunchedEffect(drawerState.restoreMenuFocus) {
        if (drawerState.restoreMenuFocus) {
            menuFocusRequester.requestFocus()
            drawerState = drawerState.menuFocusRestored()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CinematicGlassColors.Background),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            CinematicGlassColors.Primary.copy(alpha = 0.14f),
                            CinematicGlassColors.Background,
                        ),
                        radius = 1500f,
                    ),
                ),
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    horizontal = CinematicGlassSpacing.SafeAreaX,
                    vertical = CinematicGlassSpacing.SafeAreaY,
                ),
            verticalArrangement = Arrangement.spacedBy(30.dp),
        ) {
            TopChromeBar(
                title = "EMBY",
                subtitle = "Subtitles: ON · v0.1.0",
                onMenuClick = { drawerState = drawerState.open() },
                menuFocusRequester = menuFocusRequester,
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Welcome back",
                    color = CinematicGlassColors.Primary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                )
                Text(
                    text = "Your Personal Home Cinema.",
                    color = CinematicGlassColors.OnSurface,
                    fontSize = 52.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            SectionHeader(title = "Media Libraries")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(CinematicGlassSpacing.CardGap),
            ) {
                dashboard.libraries.forEach { library ->
                    LibraryCard(
                        library = library,
                        modifier = Modifier.weight(1f),
                        onClick = { hintMessage = library.disabledReason },
                        onUnsupported = { hintMessage = it },
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SectionHeader(title = "Continue Watching")
                PrimaryTvButton(
                    text = "样例播放",
                    icon = Icons.Filled.PlayArrow,
                    onClick = onPlaySample,
                )
            }
            if (state.items.isEmpty()) {
                EmptyDashboardPanel()
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(CinematicGlassSpacing.CardGap)) {
                    items(state.items, key = { it.id }) { item ->
                        val card = dashboard.continueWatching.first { it.id == item.id }
                        MediaPosterCard(
                            card = card,
                            modifier = Modifier.fillParentMaxWidth(0.16f),
                            onClick = { onPlay(item) },
                        )
                    }
                }
            }
        }
        MiniPlayerBar(modifier = Modifier.align(Alignment.BottomCenter))
        RemoteHint(
            message = hintMessage,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 106.dp),
        )
        NavigationDrawerPanel(
            items = dashboard.navigationItems,
            visible = drawerState.isOpen,
            onClose = { drawerState = drawerState.close() },
            onItemClick = { item ->
                if (item.id == HomeNavigationId.Home) {
                    drawerState = drawerState.close()
                }
            },
            onUnsupported = { hintMessage = it },
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        color = CinematicGlassColors.OnSurface,
        fontSize = 28.sp,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
private fun EmptyDashboardPanel() {
    GlassPanel(modifier = Modifier.fillMaxWidth(), cornerRadius = 12.dp) {
        Column(
            modifier = Modifier.padding(28.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(text = "尚未加载媒体", color = CinematicGlassColors.OnSurface, fontSize = 22.sp)
            Text(
                text = "Emby 已连接，但当前接口没有返回可播放条目。你仍可使用样例播放验证播放器和弹幕层。",
                color = CinematicGlassColors.OnSurfaceVariant,
                fontSize = 16.sp,
            )
        }
    }
}

@Composable
private fun MiniPlayerBar(modifier: Modifier = Modifier) {
    GlassPanel(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = CinematicGlassSpacing.SafeAreaX, vertical = 28.dp),
        cornerRadius = 12.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text("Select a title to begin", color = CinematicGlassColors.OnSurface, fontSize = 15.sp)
                Text("No media playing", color = CinematicGlassColors.OnSurfaceVariant, fontSize = 12.sp)
            }
            Text("Direct Play Ready", color = CinematicGlassColors.Primary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}
