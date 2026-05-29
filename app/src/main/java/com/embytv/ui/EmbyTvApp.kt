package com.embytv.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.embytv.core.di.AppContainer
import com.embytv.domain.model.PlaybackSource
import com.embytv.ui.home.HomeScreen
import com.embytv.ui.home.HomeViewModel
import com.embytv.ui.player.PlayerScreen
import kotlinx.coroutines.launch

@Composable
fun EmbyTvApp(container: AppContainer) {
    var playbackSource by remember { mutableStateOf<PlaybackSource?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val homeViewModel: HomeViewModel = viewModel(
        factory = HomeViewModel.Factory(container.embyRepository),
    )

    val selectedPlaybackSource = playbackSource
    if (selectedPlaybackSource == null) {
        HomeScreen(
            viewModel = homeViewModel,
            onPlay = { playbackSource = it },
        )
    } else {
        PlayerScreen(
            container = container,
            playbackSource = selectedPlaybackSource,
            onBack = { playbackSource = null },
            onPlayNext = { nextItem ->
                coroutineScope.launch {
                    homeViewModel.createPlaybackSource(nextItem)?.let { playbackSource = it }
                }
            },
        )
    }
}
