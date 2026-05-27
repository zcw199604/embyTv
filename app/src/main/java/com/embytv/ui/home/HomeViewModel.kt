package com.embytv.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.embytv.core.network.MobileSetupSyncServer
import com.embytv.data.repository.EmbyRepository
import com.embytv.domain.model.EmbySession
import com.embytv.domain.model.MediaItemSummary
import com.embytv.domain.model.PlaybackSource
import com.embytv.domain.model.ServerConfigDraft
import com.embytv.domain.model.ServerProtocol
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class HomeViewModel(
    private val repository: EmbyRepository,
    private val syncServer: MobileSetupSyncServer = MobileSetupSyncServer(),
) : ViewModel() {
    private var deviceId: String = UUID.randomUUID().toString()
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        startMobileSetupSync()
        restoreSavedCredential()
    }

    fun updateServerHost(value: String) {
        updateDraft { it.copy(host = value) }
    }

    fun updateServerProtocol(value: ServerProtocol) {
        updateDraft { it.withProtocol(value) }
    }

    fun updateServerPort(value: String) {
        updateDraft { it.copy(port = value) }
    }

    fun updateServerPath(value: String) {
        updateDraft { it.copy(path = value) }
    }

    fun updateUsername(value: String) {
        updateDraft { it.copy(username = value) }
    }

    fun updatePassword(value: String) {
        updateDraft { it.copy(password = value) }
    }

    fun connect() {
        val state = _uiState.value
        val config = state.serverConfig.toServerConfigOrNull(deviceId)
        if (config == null) {
            _uiState.update {
                it.copy(errorMessage = state.serverConfig.validate().exceptionOrNull()?.message ?: "请检查 Emby 配置")
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            repository.authenticate(config).onSuccess { session ->
                stopMobileSetupSync()
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

    override fun onCleared() {
        syncServer.stop()
        super.onCleared()
    }

    private fun updateDraft(transform: (ServerConfigDraft) -> ServerConfigDraft) {
        _uiState.update {
            it.copy(serverConfig = transform(it.serverConfig), errorMessage = null)
        }
    }

    private fun startMobileSetupSync() {
        syncServer.start()
            .onSuccess { endpoint ->
                _uiState.update {
                    it.copy(
                        mobileSetupSync = MobileSetupSyncUiState(
                            isRunning = true,
                            qrUrl = endpoint.url,
                        ),
                    )
                }
                viewModelScope.launch {
                    syncServer.payloads.collect { draft ->
                        _uiState.update {
                            it.copy(
                                serverConfig = draft,
                                errorMessage = null,
                                mobileSetupSync = it.mobileSetupSync.copy(errorMessage = "已从手机同步配置"),
                            )
                        }
                    }
                }
            }
            .onFailure { error ->
                _uiState.update {
                    it.copy(
                        mobileSetupSync = MobileSetupSyncUiState(
                            isRunning = false,
                            errorMessage = error.message ?: "手机同步服务启动失败",
                        ),
                    )
                }
            }
    }

    private fun stopMobileSetupSync() {
        syncServer.stop()
        _uiState.update {
            it.copy(mobileSetupSync = it.mobileSetupSync.copy(isRunning = false, qrUrl = null))
        }
    }

    private fun restoreSavedCredential() {
        viewModelScope.launch {
            repository.loadSavedCredential()
                .onSuccess { credential ->
                    if (credential == null) return@onSuccess
                    deviceId = credential.deviceId
                    val session = EmbySession(
                        serverUrl = credential.serverUrl,
                        userId = credential.userId,
                        accessToken = credential.accessToken,
                        serverId = credential.serverId,
                    )
                    stopMobileSetupSync()
                    _uiState.update { it.copy(isLoading = true, session = session, errorMessage = null) }
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
                            repository.clearSavedCredential()
                            deviceId = UUID.randomUUID().toString()
                            startMobileSetupSync()
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    session = null,
                                    errorMessage = error.message ?: "登录凭证已失效，请重新登录",
                                )
                            }
                        }
                }
        }
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

private fun ServerConfigDraft.toServerConfigOrNull(deviceId: String) =
    runCatching { toServerConfig(deviceId = deviceId) }.getOrNull()
