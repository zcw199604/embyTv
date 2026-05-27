package com.embytv.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.embytv.core.di.AppContainer
import com.embytv.domain.model.PlaybackSource
import com.embytv.ui.home.HomeScreen
import com.embytv.ui.home.HomeViewModel
import com.embytv.ui.player.PlayerScreen

@Composable
fun EmbyTvApp(container: AppContainer) {
    var playbackSource by remember { mutableStateOf<PlaybackSource?>(null) }

    val selectedPlaybackSource = playbackSource
    if (selectedPlaybackSource == null) {
        val homeViewModel: HomeViewModel = viewModel(
            factory = HomeViewModel.Factory(container.embyRepository),
        )
        HomeScreen(
            viewModel = homeViewModel,
            onPlay = { playbackSource = it },
        )
    } else {
        PlayerScreen(
            container = container,
            playbackSource = selectedPlaybackSource,
            onBack = { playbackSource = null },
        )
    }
}
