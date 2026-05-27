package com.embytv.ui.home

import com.embytv.domain.model.EmbySession
import com.embytv.domain.model.MediaItemSummary
import com.embytv.domain.model.ServerConfigDraft

data class HomeUiState(
    val serverConfig: ServerConfigDraft = ServerConfigDraft(),
    val mobileSetupSync: MobileSetupSyncUiState = MobileSetupSyncUiState(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val session: EmbySession? = null,
    val items: List<MediaItemSummary> = emptyList(),
)

data class MobileSetupSyncUiState(
    val isRunning: Boolean = false,
    val qrUrl: String? = null,
    val errorMessage: String? = null,
)
