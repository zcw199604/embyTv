package com.embytv.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.embytv.data.repository.EmbyRepository
import com.embytv.domain.model.MediaItemSummary
import com.embytv.domain.model.PlaybackSource
import com.embytv.domain.model.ServerConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class HomeViewModel(
    private val repository: EmbyRepository,
) : ViewModel() {
    private val deviceId: String = UUID.randomUUID().toString()
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun updateServerUrl(value: String) {
        _uiState.update { it.copy(serverUrl = value, errorMessage = null) }
    }

    fun updateUsername(value: String) {
        _uiState.update { it.copy(username = value, errorMessage = null) }
    }

    fun updatePassword(value: String) {
        _uiState.update { it.copy(password = value, errorMessage = null) }
    }

    fun connect() {
        val state = _uiState.value
        if (state.serverUrl.isBlank() || state.username.isBlank()) {
            _uiState.update { it.copy(errorMessage = "请填写 Emby 地址和用户名") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            repository.authenticate(
                ServerConfig(
                    baseUrl = state.serverUrl,
                    username = state.username,
                    password = state.password,
                    deviceId = deviceId,
                ),
            ).onSuccess { session ->
                repository.loadMediaItems(session, deviceId)
                    .onSuccess { items ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                session = session,
                                items = items,
                            )
                        }
                    }
                    .onFailure { error ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                session = session,
                                errorMessage = error.message ?: "媒体列表加载失败",
                            )
                        }
                    }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Emby 登录失败",
                    )
                }
            }
        }
    }

    fun createPlaybackSource(item: MediaItemSummary): PlaybackSource? {
        val session = _uiState.value.session ?: return null
        return repository.createPlaybackSource(session, item)
    }

    class Factory(
        private val repository: EmbyRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(HomeViewModel::class.java))
            return HomeViewModel(repository) as T
        }
    }
}
