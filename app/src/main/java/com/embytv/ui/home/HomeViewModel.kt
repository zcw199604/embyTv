package com.embytv.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.embytv.core.network.MobileSetupSyncServer
import com.embytv.data.local.SearchHistoryItem
import com.embytv.data.local.SearchHistoryStore
import com.embytv.data.local.ThemePreferenceStore
import com.embytv.data.repository.EmbyRepository
import com.embytv.domain.model.DiscoveryEntrySummary
import com.embytv.domain.model.DiscoveryKind
import com.embytv.domain.model.EmbySession
import com.embytv.domain.model.EmbyLibrarySummary
import com.embytv.domain.model.EmbySeasonSummary
import com.embytv.domain.model.MediaItemSummary
import com.embytv.domain.model.PlaybackSource
import com.embytv.domain.model.SavedEmbyCredential
import com.embytv.domain.model.ServerConfigDraft
import com.embytv.domain.model.ServerProtocol
import com.embytv.ui.theme.AppLanguage
import com.embytv.ui.theme.AppThemeId
import com.embytv.ui.theme.FontScale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import java.util.UUID

class HomeViewModel(
    private val repository: EmbyRepository,
    private val searchHistoryStore: SearchHistoryStore,
    private val themePreferenceStore: ThemePreferenceStore,
    private val syncServer: MobileSetupSyncServer = MobileSetupSyncServer(),
) : ViewModel() {
    private var deviceId: String = UUID.randomUUID().toString()
    private var searchJob: Job? = null
    private var mobileSetupSyncJob: Job? = null
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        observeSearchHistory()
        observeThemePreferences()
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
                        errorMessage = loginErrorMessage(error, config),
                    )
                }
            }
        }
    }

    fun selectSavedCredential(credential: SavedEmbyCredential) {
        deviceId = credential.deviceId
        val session = EmbySession(
            serverUrl = credential.serverUrl,
            userId = credential.userId,
            accessToken = credential.accessToken,
            serverId = credential.serverId,
        )
        stopMobileSetupSync()
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    session = session,
                    imageAuthorizationHeader = repository.buildImageAuthorizationHeader(session, deviceId),
                    showCredentialPicker = false,
                    confirmation = null,
                    errorMessage = null,
                )
            }
            loadDashboard(session).onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        session = null,
                        imageAuthorizationHeader = null,
                        showCredentialPicker = true,
                        errorMessage = error.message ?: "登录凭证已失效，请重新选择或重新登录",
                    )
                }
            }
        }
    }

    fun deleteSavedCredential(credential: SavedEmbyCredential) {
        _uiState.update {
            it.copy(
                confirmation = HomeConfirmationUiState(
                    kind = HomeConfirmationKind.DeleteCredential,
                    title = "删除保存身份",
                    message = "确认删除 ${credential.username} 在 ${credential.serverUrl} 的登录身份？此操作不会删除 Emby 服务器账号。",
                    confirmLabel = "确认删除",
                    credential = credential,
                ),
                errorMessage = null,
            )
        }
    }

    fun clearConfirmation() {
        _uiState.update { it.copy(confirmation = null) }
    }

    fun confirmPendingAction() {
        val confirmation = _uiState.value.confirmation ?: return
        when (confirmation.kind) {
            HomeConfirmationKind.DeleteCredential -> {
                val credential = confirmation.credential ?: return
                deleteSavedCredentialConfirmed(credential)
            }
            HomeConfirmationKind.ClearResumeProgress -> {
                val item = confirmation.item ?: return
                clearResumeProgressConfirmed(item)
            }
        }
    }

    private fun deleteSavedCredentialConfirmed(credential: SavedEmbyCredential) {
        viewModelScope.launch {
            _uiState.update { it.copy(confirmation = null) }
            repository.deleteSavedCredential(credential.uniqueKey)
                .onSuccess {
                    restoreSavedCredential()
                }
                .onFailure { error ->
                    _uiState.update { it.copy(errorMessage = error.message ?: "删除凭证失败") }
                }
        }
    }

    fun startNewConnection() {
        deviceId = UUID.randomUUID().toString()
        _uiState.update {
            it.copy(
                showCredentialPicker = false,
                session = null,
                imageAuthorizationHeader = null,
                confirmation = null,
                errorMessage = null,
            )
        }
        startMobileSetupSync()
    }

    suspend fun createPlaybackSource(item: MediaItemSummary): PlaybackSource? {
        val session = _uiState.value.session ?: return null
        val queueItems = currentQueueItemsFor(item)
        return repository.createPlaybackSourceWithDetails(session, deviceId, item, queueItems)
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

    fun openSettings() {
        _uiState.update {
            it.copy(
                settings = it.settings.open(),
                libraryContent = it.libraryContent.close(),
                favoriteContent = it.favoriteContent.close(),
                search = it.search.close(),
                discoveryContent = it.discoveryContent.close(),
                mediaDetail = it.mediaDetail.close(),
                errorMessage = null,
            )
        }
    }

    fun closeSettings() {
        _uiState.update { it.copy(settings = it.settings.close(), errorMessage = null) }
    }

    fun selectTheme(themeId: AppThemeId) {
        viewModelScope.launch {
            themePreferenceStore.setTheme(themeId)
        }
    }

    fun setHighContrast(enabled: Boolean) {
        viewModelScope.launch {
            themePreferenceStore.setHighContrast(enabled)
        }
    }

    fun selectFontScale(fontScale: FontScale) {
        viewModelScope.launch {
            themePreferenceStore.setFontScale(fontScale)
        }
    }

    fun selectLanguage(language: AppLanguage) {
        viewModelScope.launch {
            themePreferenceStore.setLanguage(language)
        }
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

    fun openSearch() {
        _uiState.update {
            it.copy(
                search = it.search.open(),
                libraryContent = it.libraryContent.close(),
                favoriteContent = it.favoriteContent.close(),
                discoveryContent = it.discoveryContent.close(),
                mediaDetail = it.mediaDetail.close(),
                errorMessage = null,
            )
        }
    }

    fun closeSearch() {
        searchJob?.cancel()
        _uiState.update { it.copy(search = it.search.close(), errorMessage = null) }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(search = it.search.copy(query = query, errorMessage = null)) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(350)
            runSearchNow(query)
        }
    }

    fun retrySearch() {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            runSearchNow(_uiState.value.search.query)
        }
    }

    fun clearSearch() {
        searchJob?.cancel()
        _uiState.update { it.copy(search = SearchUiState(isOpen = true, history = it.search.history)) }
    }

    fun selectSearchHistory(item: SearchHistoryItem) {
        updateSearchQuery(item.query)
    }

    fun removeSearchHistory(query: String) {
        viewModelScope.launch {
            searchHistoryStore.removeHistory(query)
        }
    }

    fun clearSearchHistory() {
        viewModelScope.launch {
            searchHistoryStore.clearHistory()
        }
    }

    fun openDiscovery(kind: DiscoveryKind) {
        val session = _uiState.value.session ?: return
        loadDiscoveryContent(session, kind)
    }

    fun closeDiscovery() {
        _uiState.update { it.copy(discoveryContent = it.discoveryContent.close(), errorMessage = null) }
    }

    fun backFromDiscovery() {
        _uiState.update { it.copy(discoveryContent = it.discoveryContent.back(), errorMessage = null) }
    }

    fun retryDiscovery() {
        val state = _uiState.value.discoveryContent
        val session = _uiState.value.session ?: return
        val kind = state.kind ?: return
        loadDiscoveryContent(session, kind)
    }

    fun openDiscoveryEntry(entry: DiscoveryEntrySummary) {
        val session = _uiState.value.session ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(discoveryContent = it.discoveryContent.loadingEntry(entry)) }
            repository.loadDiscoveryEntryItems(session, deviceId, entry)
                .onSuccess { items ->
                    _uiState.update { it.copy(discoveryContent = it.discoveryContent.entryLoaded(items)) }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(discoveryContent = it.discoveryContent.entryFailed(error.message ?: "发现内容加载失败"))
                    }
                }
        }
    }

    fun retryDiscoveryEntry() {
        _uiState.value.discoveryContent.selectedEntry?.let(::openDiscoveryEntry)
    }

    fun openMediaDetail(item: MediaItemSummary) {
        val session = _uiState.value.session ?: return
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    mediaDetail = it.mediaDetail.openLoading(item),
                    errorMessage = null,
                )
            }
            repository.loadMediaDetail(session, deviceId, item.id)
                .onSuccess { detail ->
                    _uiState.update {
                        it.copy(mediaDetail = it.mediaDetail.loaded(detail))
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(mediaDetail = it.mediaDetail.failed(error.message ?: "媒体详情加载失败"))
                    }
                }
        }
    }

    fun toggleFavorite(item: MediaItemSummary) {
        val session = _uiState.value.session ?: return
        viewModelScope.launch {
            repository.toggleFavorite(session, deviceId, item.id, favorite = !item.isFavorite)
                .onSuccess {
                    refreshMediaDetailIfOpen()
                    refreshDashboard(session)
                }
                .onFailure { error ->
                    _uiState.update { it.copy(errorMessage = error.message ?: "收藏状态更新失败") }
                }
        }
    }

    fun togglePlayed(item: MediaItemSummary) {
        val session = _uiState.value.session ?: return
        viewModelScope.launch {
            repository.markPlayed(session, deviceId, item.id, played = !item.played)
                .onSuccess {
                    refreshMediaDetailIfOpen()
                    refreshDashboard(session)
                }
                .onFailure { error ->
                    _uiState.update { it.copy(errorMessage = error.message ?: "播放状态更新失败") }
                }
        }
    }

    fun clearResumeProgress(item: MediaItemSummary) {
        _uiState.update {
            it.copy(
                confirmation = HomeConfirmationUiState(
                    kind = HomeConfirmationKind.ClearResumeProgress,
                    title = "清除播放进度",
                    message = "确认清除《${item.name.ifBlank { item.seriesName ?: item.id }}》的继续观看进度？",
                    confirmLabel = "确认清除",
                    item = item,
                ),
                errorMessage = null,
            )
        }
    }

    private fun clearResumeProgressConfirmed(item: MediaItemSummary) {
        val session = _uiState.value.session ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(confirmation = null) }
            repository.clearResumeProgress(session, deviceId, item.id)
                .onSuccess {
                    refreshMediaDetailIfOpen()
                    refreshDashboard(session)
                }
                .onFailure { error ->
                    _uiState.update { it.copy(errorMessage = error.message ?: "继续观看进度清除失败") }
                }
        }
    }

    fun retryMediaDetail() {
        val item = _uiState.value.mediaDetail.requestedItem ?: return
        openMediaDetail(item)
    }

    fun closeMediaDetail() {
        _uiState.update { it.copy(mediaDetail = it.mediaDetail.close(), errorMessage = null) }
    }

    fun backFromDetail() {
        _uiState.update { it.copy(mediaDetail = it.mediaDetail.back(), errorMessage = null) }
    }

    fun openSeasonEpisodes(season: EmbySeasonSummary) {
        val state = _uiState.value
        val session = state.session ?: return
        val seriesId = state.mediaDetail.detail?.item?.id ?: return
        viewModelScope.launch {
            _uiState.update {
                it.copy(mediaDetail = it.mediaDetail.loadingSeason(season))
            }
            repository.loadSeasonEpisodes(session, deviceId, seriesId, season)
                .onSuccess { episodes ->
                    _uiState.update {
                        it.copy(mediaDetail = it.mediaDetail.seasonLoaded(episodes))
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(mediaDetail = it.mediaDetail.seasonFailed(error.message ?: "剧集列表加载失败"))
                    }
                }
        }
    }

    override fun onCleared() {
        searchJob?.cancel()
        mobileSetupSyncJob?.cancel()
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
                mobileSetupSyncJob?.cancel()
                mobileSetupSyncJob = viewModelScope.launch {
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
        mobileSetupSyncJob?.cancel()
        mobileSetupSyncJob = null
        syncServer.stop()
        _uiState.update {
            it.copy(mobileSetupSync = it.mobileSetupSync.copy(isRunning = false, qrUrl = null))
        }
    }

    private fun restoreSavedCredential() {
        viewModelScope.launch {
            repository.loadSavedCredentials()
                .onSuccess { credentials ->
                    _uiState.update { it.copy(savedCredentials = credentials, confirmation = null) }
                    if (credentials.isEmpty()) {
                        startMobileSetupSync()
                        return@onSuccess
                    }
                    if (credentials.size > 1) {
                        stopMobileSetupSync()
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                session = null,
                                imageAuthorizationHeader = null,
                                showCredentialPicker = true,
                                errorMessage = null,
                            )
                        }
                        return@onSuccess
                    }
                    val credential = credentials.single()
                    deviceId = credential.deviceId
                    val session = EmbySession(
                        serverUrl = credential.serverUrl,
                        userId = credential.userId,
                        accessToken = credential.accessToken,
                        serverId = credential.serverId,
                    )
                    stopMobileSetupSync()
                    _uiState.update {
                        it.copy(
                            isLoading = true,
                            session = session,
                            imageAuthorizationHeader = repository.buildImageAuthorizationHeader(session, deviceId),
                            errorMessage = null,
                        )
                    }
                    loadDashboard(session).onFailure { error ->
                        repository.clearSavedCredential()
                        deviceId = UUID.randomUUID().toString()
                        startMobileSetupSync()
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                session = null,
                                imageAuthorizationHeader = null,
                                errorMessage = error.message ?: "登录凭证已失效，请重新登录",
                            )
                        }
                    }
                }
                .onFailure {
                    startMobileSetupSync()
                }
        }
    }

    private fun observeSearchHistory() {
        viewModelScope.launch {
            searchHistoryStore.historyFlow.collect { history ->
                _uiState.update { it.copy(search = it.search.withHistory(history)) }
            }
        }
    }

    private fun observeThemePreferences() {
        viewModelScope.launch {
            themePreferenceStore.preferencesFlow.collect { preferences ->
                _uiState.update { HomeThemePreferenceObserver.apply(it, preferences) }
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
                        imageAuthorizationHeader = repository.buildImageAuthorizationHeader(session, deviceId),
                        dashboard = dashboard,
                        libraryContent = it.libraryContent.close(),
                        favoriteContent = it.favoriteContent.close(),
                        search = it.search.close(),
                        discoveryContent = it.discoveryContent.close(),
                        mediaDetail = it.mediaDetail.close(),
                        settings = it.settings.close(),
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
                    favoriteContent = it.favoriteContent.close(),
                    search = it.search.close(),
                    discoveryContent = it.discoveryContent.close(),
                    mediaDetail = it.mediaDetail.close(),
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
                    search = it.search.close(),
                    discoveryContent = it.discoveryContent.close(),
                    mediaDetail = it.mediaDetail.close(),
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

    private suspend fun runSearchNow(query: String) {
        val session = _uiState.value.session ?: return
        val normalized = query.trim()
        if (normalized.isBlank()) {
            _uiState.update { it.copy(search = SearchUiState(isOpen = true)) }
            return
        }
        _uiState.update { it.copy(search = it.search.loading(normalized)) }
        repository.searchItems(session, deviceId, normalized)
            .onSuccess { results ->
                searchHistoryStore.addHistory(normalized, results.items.size)
                _uiState.update { current ->
                    if (current.search.query.trim() == normalized) {
                        current.copy(search = current.search.loaded(results))
                    } else {
                        current
                    }
                }
            }
            .onFailure { error ->
                _uiState.update { current ->
                    if (current.search.query.trim() == normalized) {
                        current.copy(search = current.search.failed(normalized, error.message ?: "搜索失败"))
                    } else {
                        current
                    }
                }
            }
    }

    private fun loadDiscoveryContent(session: EmbySession, kind: DiscoveryKind) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    discoveryContent = it.discoveryContent.openLoading(kind),
                    libraryContent = it.libraryContent.close(),
                    favoriteContent = it.favoriteContent.close(),
                    search = it.search.close(),
                    mediaDetail = it.mediaDetail.close(),
                    errorMessage = null,
                )
            }
            repository.loadDiscoveryContent(session, deviceId, kind)
                .onSuccess { content ->
                    _uiState.update { it.copy(discoveryContent = it.discoveryContent.loaded(content)) }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(discoveryContent = it.discoveryContent.failed(kind, error.message ?: "发现页加载失败"))
                    }
                }
        }
    }

    private suspend fun refreshDashboard(session: EmbySession) {
        repository.loadHomeDashboard(session, deviceId)
            .onSuccess { dashboard ->
                _uiState.update { it.copy(dashboard = dashboard) }
            }
    }

    private fun refreshMediaDetailIfOpen() {
        val item = _uiState.value.mediaDetail.requestedItem ?: return
        openMediaDetail(item)
    }

    private fun currentQueueItemsFor(item: MediaItemSummary): List<MediaItemSummary> {
        val state = _uiState.value
        return when {
            state.mediaDetail.seasonEpisodes?.episodes?.any { it.id == item.id } == true ->
                state.mediaDetail.seasonEpisodes.episodes
            state.discoveryContent.entryItems?.items?.any { it.id == item.id } == true ->
                state.discoveryContent.entryItems.items
            state.search.results.items.any { it.id == item.id } ->
                state.search.results.items
            else -> emptyList()
        }
    }

    class Factory(
        private val repository: EmbyRepository,
        private val searchHistoryStore: SearchHistoryStore,
        private val themePreferenceStore: ThemePreferenceStore,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(HomeViewModel::class.java))
            return HomeViewModel(repository, searchHistoryStore, themePreferenceStore) as T
        }
    }
}

private fun ServerConfigDraft.toServerConfigOrNull(deviceId: String) =
    runCatching { toServerConfig(deviceId = deviceId) }.getOrNull()

internal fun loginErrorMessage(error: Throwable, config: com.embytv.domain.model.ServerConfig): String {
    val endpoint = config.baseUrl.trimEnd('/')
    val detail = when (error) {
        is HttpException -> when (error.code()) {
            401 -> "HTTP 401 用户名或密码错误"
            else -> "HTTP ${error.code()} ${error.message()}".trim()
        }
        is IOException -> error.message?.takeIf { it.isNotBlank() } ?: "网络连接失败"
        else -> error.message?.takeIf { it.isNotBlank() } ?: error::class.java.simpleName
    }
    return "Emby 登录失败：$detail。请求地址：$endpoint，用户名：${config.username}"
}
