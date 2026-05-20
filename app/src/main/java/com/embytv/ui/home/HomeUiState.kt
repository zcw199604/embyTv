package com.embytv.ui.home

import com.embytv.domain.model.EmbySession
import com.embytv.domain.model.MediaItemSummary

data class HomeUiState(
    val serverUrl: String = "",
    val username: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val session: EmbySession? = null,
    val items: List<MediaItemSummary> = emptyList(),
)
