package com.embytv.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.embytv.core.network.MobileSetupSyncServer
import com.embytv.data.repository.EmbyRepository
import com.embytv.domain.model.EmbySession
import com.embytv.domain.model.EmbyLibrarySummary
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
                loadDashboard(session)
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

    suspend fun createPlaybackSource(item: MediaItemSummary): PlaybackSource? {
        val session = _uiState.value.session ?: return null
        return repository.createPlaybackSourceWithDetails(session, deviceId, item)
            .getOrElse { error ->
                _uiState.update { it.copy(errorMessage = error.message ?: "播放信息加载失败") }
                null
            }
    }

    fun openLibrary(libraryId: String) {
        val session = _uiState.value.session ?: return
        val library = _uiState.value.dashboard.libraries.firstOrNull { it.id == libraryId } ?: return
        loadLibraryContent(session, library)
    }

    fun closeLibrary() {
        _uiState.update { it.copy(libraryContent = it.libraryContent.close(), errorMessage = null) }
    }

    fun retryLibrary() {
        val state = _uiState.value
        val session = state.session ?: return
        val library = state.libraryContent.content?.library
            ?: state.dashboard.libraries.firstOrNull { it.id == state.libraryContent.selectedLibraryId }
            ?: return
        loadLibraryContent(session, library)
    }

    fun openFavorites() {
        val session = _uiState.value.session ?: return
        loadFavorites(session)
    }

    fun closeFavorites() {
        _uiState.update { it.copy(favoriteContent = it.favoriteContent.close(), errorMessage = null) }
    }

    fun selectFavoriteCategory(category: FavoriteCategory) {
        _uiState.update {
            it.copy(favoriteContent = it.favoriteContent.select(category))
        }
    }

    fun retryFavorites() {
        val session = _uiState.value.session ?: return
        loadFavorites(session)
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
                    loadDashboard(session).onFailure { error ->
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

    private suspend fun loadDashboard(session: EmbySession): Result<Unit> {
        return repository.loadHomeDashboard(session, deviceId)
            .onSuccess { dashboard ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        session = session,
                        dashboard = dashboard,
                        libraryContent = it.libraryContent.close(),
                        favoriteContent = it.favoriteContent.close(),
                    )
                }
            }
            .onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        session = session,
                        errorMessage = error.message ?: "媒体数据加载失败",
                    )
                }
            }
            .map { Unit }
    }

    private fun loadLibraryContent(session: EmbySession, library: EmbyLibrarySummary) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    libraryContent = LibraryContentUiState(
                        selectedLibraryId = library.id,
                        content = null,
                        isLoading = true,
                        errorMessage = null,
                    ),
                    errorMessage = null,
                )
            }
            repository.loadLibraryContent(session, deviceId, library)
                .onSuccess { content ->
                    _uiState.update {
                        it.copy(
                            libraryContent = LibraryContentUiState(
                                selectedLibraryId = content.library.id,
                                content = content,
                                isLoading = false,
                                errorMessage = null,
                            ),
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            libraryContent = LibraryContentUiState(
                                selectedLibraryId = library.id,
                                content = null,
                                isLoading = false,
                                errorMessage = error.message ?: "媒体库加载失败",
                            ),
                        )
                    }
                }
        }
    }

    private fun loadFavorites(session: EmbySession) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    favoriteContent = it.favoriteContent.openLoading(),
                    libraryContent = it.libraryContent.close(),
                    errorMessage = null,
                )
            }
            repository.loadFavoriteDashboard(session, deviceId)
                .onSuccess { dashboard ->
                    _uiState.update {
                        it.copy(
                            favoriteContent = it.favoriteContent.loaded(dashboard),
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            favoriteContent = it.favoriteContent.failed(error.message ?: "收藏加载失败"),
                        )
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
