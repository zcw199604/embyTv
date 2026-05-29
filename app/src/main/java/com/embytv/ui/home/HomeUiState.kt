package com.embytv.ui.home

import com.embytv.domain.model.EmbyHomeDashboard
import com.embytv.domain.model.EmbySession
import com.embytv.domain.model.MediaItemSummary
import com.embytv.domain.model.SavedEmbyCredential
import com.embytv.domain.model.ServerConfigDraft
import com.embytv.ui.theme.ThemePreferences

data class HomeUiState(
    val serverConfig: ServerConfigDraft = ServerConfigDraft(),
    val mobileSetupSync: MobileSetupSyncUiState = MobileSetupSyncUiState(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val savedCredentials: List<SavedEmbyCredential> = emptyList(),
    val showCredentialPicker: Boolean = false,
    val session: EmbySession? = null,
    val imageAuthorizationHeader: String? = null,
    val dashboard: EmbyHomeDashboard = EmbyHomeDashboard(),
    val libraryContent: LibraryContentUiState = LibraryContentUiState(),
    val favoriteContent: FavoriteContentUiState = FavoriteContentUiState(),
    val search: SearchUiState = SearchUiState(),
    val discoveryContent: DiscoveryContentUiState = DiscoveryContentUiState(),
    val mediaDetail: MediaDetailUiState = MediaDetailUiState(),
    val themePreferences: ThemePreferences = ThemePreferences(),
    val settings: SettingsUiState = SettingsUiState(),
    val confirmation: HomeConfirmationUiState? = null,
)

data class SettingsUiState(
    val isOpen: Boolean = false,
) {
    fun open(): SettingsUiState = copy(isOpen = true)
    fun close(): SettingsUiState = SettingsUiState()
}

data class MobileSetupSyncUiState(
    val isRunning: Boolean = false,
    val qrUrl: String? = null,
    val errorMessage: String? = null,
)

enum class HomeConfirmationKind {
    DeleteCredential,
    ClearResumeProgress,
}

data class HomeConfirmationUiState(
    val kind: HomeConfirmationKind,
    val title: String,
    val message: String,
    val confirmLabel: String,
    val credential: SavedEmbyCredential? = null,
    val item: MediaItemSummary? = null,
)
