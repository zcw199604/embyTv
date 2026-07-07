package com.embytv.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.border
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tv
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.embytv.domain.model.DiscoveryEntrySummary
import com.embytv.domain.model.DiscoveryKind
import com.embytv.domain.model.MediaItemSummary
import com.embytv.domain.model.PlaybackSource
import com.embytv.domain.model.SavedEmbyCredential
import com.embytv.data.local.SearchHistoryItem
import com.embytv.ui.components.GlassPanel
import com.embytv.ui.components.FocusableGlassSurface
import com.embytv.ui.components.LocalEmbyImageAuthorizationHeader
import com.embytv.ui.components.MediaPosterCard
import com.embytv.ui.components.NavigationDrawerPanel
import com.embytv.ui.components.NetworkBackdropImage
import com.embytv.ui.components.PrimaryTvButton
import com.embytv.ui.components.RemoteHint
import com.embytv.ui.components.RoundIconButton
import com.embytv.ui.components.loading.DetailSkeleton
import com.embytv.ui.components.loading.MediaGridSkeleton
import com.embytv.ui.components.loading.MediaListSkeleton
import com.embytv.ui.components.navigation.AlphabetIndexBar
import com.embytv.ui.components.navigation.ScrollPositionIndicator
import com.embytv.ui.components.navigation.findIndexByLetter
import com.embytv.ui.components.panels.EmptyStatePanel
import com.embytv.ui.components.panels.ErrorStatePanel
import com.embytv.ui.components.panels.ErrorType
import com.embytv.ui.setup.SetupScreen
import com.embytv.ui.theme.CinematicGlassColors
import com.embytv.ui.theme.CinematicGlassSpacing
import com.embytv.ui.theme.AppLanguage
import com.embytv.ui.theme.AppThemeId
import com.embytv.ui.theme.FontScale
import com.embytv.ui.utils.accessibilityLabel
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onPlay: (PlaybackSource) -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    if (state.session == null) {
        if (state.showCredentialPicker) {
            CredentialPickerScreen(
                state = state,
                onSelect = viewModel::selectSavedCredential,
                onDelete = viewModel::deleteSavedCredential,
                onConfirm = viewModel::confirmPendingAction,
                onCancelConfirmation = viewModel::clearConfirmation,
                onAdd = viewModel::startNewConnection,
            )
        } else {
            SetupScreen(
                state = state,
                onServerHostChange = viewModel::updateServerHost,
                onServerProtocolChange = viewModel::updateServerProtocol,
                onServerPortChange = viewModel::updateServerPort,
                onServerPathChange = viewModel::updateServerPath,
                onUsernameChange = viewModel::updateUsername,
                onPasswordChange = viewModel::updatePassword,
                onConnect = viewModel::connect,
            )
        }
    } else {
        CompositionLocalProvider(LocalEmbyImageAuthorizationHeader provides state.imageAuthorizationHeader) {
            HomeDashboardScreen(
                state = state,
                onOpenFavorites = viewModel::openFavorites,
                onCloseFavorites = viewModel::closeFavorites,
                onSelectFavoriteCategory = viewModel::selectFavoriteCategory,
                onRetryFavorites = viewModel::retryFavorites,
                onOpenSearch = viewModel::openSearch,
                onCloseSearch = viewModel::closeSearch,
                onSearchQueryChange = viewModel::updateSearchQuery,
                onRetrySearch = viewModel::retrySearch,
                onClearSearch = viewModel::clearSearch,
                onSelectSearchHistory = viewModel::selectSearchHistory,
                onRemoveSearchHistory = viewModel::removeSearchHistory,
                onClearSearchHistory = viewModel::clearSearchHistory,
                onOpenSettings = viewModel::openSettings,
                onCloseSettings = viewModel::closeSettings,
                onSelectTheme = viewModel::selectTheme,
                onSetHighContrast = viewModel::setHighContrast,
                onSelectFontScale = viewModel::selectFontScale,
                onSelectLanguage = viewModel::selectLanguage,
                onOpenDiscovery = viewModel::openDiscovery,
                onBackFromDiscovery = viewModel::backFromDiscovery,
                onCloseDiscovery = viewModel::closeDiscovery,
                onRetryDiscovery = viewModel::retryDiscovery,
                onOpenDiscoveryEntry = viewModel::openDiscoveryEntry,
                onRetryDiscoveryEntry = viewModel::retryDiscoveryEntry,
                onOpenLibrary = viewModel::openLibrary,
                onCloseLibrary = viewModel::closeLibrary,
                onRetryLibrary = viewModel::retryLibrary,
                onOpenMediaDetail = viewModel::openMediaDetail,
                onCloseMediaDetail = viewModel::closeMediaDetail,
                onBackFromDetail = viewModel::backFromDetail,
                onRetryMediaDetail = viewModel::retryMediaDetail,
                onOpenSeasonEpisodes = viewModel::openSeasonEpisodes,
                onToggleFavorite = viewModel::toggleFavorite,
                onTogglePlayed = viewModel::togglePlayed,
                onClearResumeProgress = viewModel::clearResumeProgress,
                onConfirm = viewModel::confirmPendingAction,
                onCancelConfirmation = viewModel::clearConfirmation,
                onPlay = { item ->
                    coroutineScope.launch {
                        viewModel.createPlaybackSource(item)?.let(onPlay)
                    }
                },
            )
        }
    }
}

@Composable
private fun CredentialPickerScreen(
    state: HomeUiState,
    onSelect: (SavedEmbyCredential) -> Unit,
    onDelete: (SavedEmbyCredential) -> Unit,
    onConfirm: () -> Unit,
    onCancelConfirmation: () -> Unit,
    onAdd: () -> Unit,
) {
    val addFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        addFocusRequester.requestFocus()
    }
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(CinematicGlassColors.Background)
                .padding(
                    horizontal = CinematicGlassSpacing.SafeAreaX,
                    vertical = CinematicGlassSpacing.SafeAreaY,
                ),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "选择 Emby 身份",
                            color = CinematicGlassColors.OnSurface,
                            fontSize = 42.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "选择已保存的服务器和用户，或添加新的连接。",
                            color = CinematicGlassColors.OnSurfaceVariant,
                            fontSize = 16.sp,
                        )
                    }
                    PrimaryTvButton(
                        text = "添加服务器",
                        icon = Icons.Filled.Search,
                        onClick = onAdd,
                        modifier = Modifier.focusRequester(addFocusRequester),
                    )
                }
            }
            if (state.errorMessage != null) {
                item { LibraryStatePanel(title = "凭证恢复失败", subtitle = state.errorMessage, errorType = ErrorType.Auth) }
            }
            if (state.savedCredentials.isEmpty()) {
                item { LibraryStatePanel(title = "暂无已保存身份", subtitle = "请添加服务器并登录。", empty = true) }
            } else {
                items(state.savedCredentials, key = { it.uniqueKey }) { credential ->
                    CredentialCard(
                        credential = credential,
                        onSelect = { onSelect(credential) },
                        onDelete = { onDelete(credential) },
                    )
                }
            }
        }
        ConfirmationOverlay(
            confirmation = state.confirmation,
            onConfirm = onConfirm,
            onCancel = onCancelConfirmation,
        )
    }
}

@Composable
private fun CredentialCard(
    credential: SavedEmbyCredential,
    onSelect: () -> Unit,
    onDelete: () -> Unit,
) {
    GlassPanel(modifier = Modifier.fillMaxWidth(), cornerRadius = 12.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
                Text(
                    text = credential.username,
                    color = CinematicGlassColors.OnSurface,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = credential.serverUrl,
                    color = CinematicGlassColors.OnSurfaceVariant,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PrimaryTvButton(text = "进入", icon = Icons.Filled.PlayArrow, onClick = onSelect)
                PrimaryTvButton(text = "删除", icon = Icons.Filled.Clear, onClick = onDelete)
            }
        }
    }
}

@Composable
private fun HomeDashboardScreen(
    state: HomeUiState,
    onOpenFavorites: () -> Unit,
    onCloseFavorites: () -> Unit,
    onSelectFavoriteCategory: (FavoriteCategory) -> Unit,
    onRetryFavorites: () -> Unit,
    onOpenSearch: () -> Unit,
    onCloseSearch: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onRetrySearch: () -> Unit,
    onClearSearch: () -> Unit,
    onSelectSearchHistory: (SearchHistoryItem) -> Unit,
    onRemoveSearchHistory: (String) -> Unit,
    onClearSearchHistory: () -> Unit,
    onOpenSettings: () -> Unit,
    onCloseSettings: () -> Unit,
    onSelectTheme: (AppThemeId) -> Unit,
    onSetHighContrast: (Boolean) -> Unit,
    onSelectFontScale: (FontScale) -> Unit,
    onSelectLanguage: (AppLanguage) -> Unit,
    onOpenDiscovery: (DiscoveryKind) -> Unit,
    onBackFromDiscovery: () -> Unit,
    onCloseDiscovery: () -> Unit,
    onRetryDiscovery: () -> Unit,
    onOpenDiscoveryEntry: (DiscoveryEntrySummary) -> Unit,
    onRetryDiscoveryEntry: () -> Unit,
    onOpenLibrary: (String) -> Unit,
    onCloseLibrary: () -> Unit,
    onRetryLibrary: () -> Unit,
    onOpenMediaDetail: (MediaItemSummary) -> Unit,
    onCloseMediaDetail: () -> Unit,
    onBackFromDetail: () -> Unit,
    onRetryMediaDetail: () -> Unit,
    onOpenSeasonEpisodes: (com.embytv.domain.model.EmbySeasonSummary) -> Unit,
    onToggleFavorite: (MediaItemSummary) -> Unit,
    onTogglePlayed: (MediaItemSummary) -> Unit,
    onClearResumeProgress: (MediaItemSummary) -> Unit,
    onConfirm: () -> Unit,
    onCancelConfirmation: () -> Unit,
    onPlay: (MediaItemSummary) -> Unit,
) {
    val dashboard = remember(state.dashboard) { HomeDashboardMapper.map(state.dashboard) }
    val mediaItems = remember(state.dashboard) {
        if (state.dashboard.resumeItems.isNotEmpty()) {
            state.dashboard.resumeItems
        } else {
            state.dashboard.latestItems
        }.take(12)
    }
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
        if (state.mediaDetail.isOpen) {
            MediaDetailScreen(
                state = state.mediaDetail,
                onBack = onBackFromDetail,
                onClose = onCloseMediaDetail,
                onRetry = onRetryMediaDetail,
                onOpenSeason = onOpenSeasonEpisodes,
                onToggleFavorite = onToggleFavorite,
                onTogglePlayed = onTogglePlayed,
                onClearResumeProgress = onClearResumeProgress,
                onPlay = onPlay,
            )
        } else if (state.search.isOpen) {
            SearchScreen(
                state = state.search,
                onBack = onCloseSearch,
                onQueryChange = onSearchQueryChange,
                onRetry = onRetrySearch,
                onClear = onClearSearch,
                onSelectHistory = onSelectSearchHistory,
                onRemoveHistory = onRemoveSearchHistory,
                onClearHistory = onClearSearchHistory,
                onPlay = onPlay,
                onOpenMediaDetail = onOpenMediaDetail,
                onUnsupported = { hintMessage = it },
            )
        } else if (state.settings.isOpen) {
            SettingsScreen(
                preferences = state.themePreferences,
                onBack = onCloseSettings,
                onSelectTheme = onSelectTheme,
                onSetHighContrast = onSetHighContrast,
                onSelectFontScale = onSelectFontScale,
                onSelectLanguage = onSelectLanguage,
            )
        } else if (state.discoveryContent.isOpen) {
            DiscoveryScreen(
                state = state.discoveryContent,
                onBack = onBackFromDiscovery,
                onClose = onCloseDiscovery,
                onRetry = onRetryDiscovery,
                onRetryEntry = onRetryDiscoveryEntry,
                onOpenEntry = onOpenDiscoveryEntry,
                onPlay = onPlay,
                onOpenMediaDetail = onOpenMediaDetail,
                onUnsupported = { hintMessage = it },
            )
        } else if (state.favoriteContent.isOpen) {
            FavoriteContentScreen(
                state = state.favoriteContent,
                onBack = onCloseFavorites,
                onRetry = onRetryFavorites,
                onSelectCategory = onSelectFavoriteCategory,
                onPlay = onPlay,
                onOpenMediaDetail = onOpenMediaDetail,
                onUnsupported = { hintMessage = it },
            )
        } else if (state.libraryContent.isOpen) {
            LibraryContentScreen(
                state = state.libraryContent,
                onBack = onCloseLibrary,
                onRetry = onRetryLibrary,
                onPlay = onPlay,
                onOpenMediaDetail = onOpenMediaDetail,
                onUnsupported = { hintMessage = it },
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        horizontal = 28.dp,
                        vertical = 34.dp,
                    ),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                item {
                    HomeReferenceTopBar(
                        title = "Emby",
                        onMenuClick = { drawerState = drawerState.open() },
                        onSearchClick = onOpenSearch,
                        onFavoritesClick = onOpenFavorites,
                        onSettingsClick = onOpenSettings,
                        menuFocusRequester = menuFocusRequester,
                    )
                }

                item {
                    SectionHeader(title = "媒体库")
                    Spacer(modifier = Modifier.height(8.dp))
                    if (state.isLoading) {
                        MediaListSkeleton(itemCount = 3)
                    } else {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(dashboard.libraries, key = { it.id }) { library ->
                                HomeLibraryTile(
                                    library = library,
                                    modifier = Modifier.fillParentMaxWidth(0.145f),
                                    onClick = { onOpenLibrary(library.id) },
                                    onUnsupported = { hintMessage = it },
                                )
                            }
                        }
                    }
                }

                item {
                    SectionHeader(title = dashboard.mediaSectionTitle.toHomeSectionTitle())
                    Spacer(modifier = Modifier.height(8.dp))
                    if (mediaItems.isEmpty()) {
                        if (state.isLoading) {
                            MediaListSkeleton()
                        } else {
                            EmptyDashboardPanel()
                        }
                    } else {
                        ContinueWatchingRow(
                            cards = dashboard.continueWatching,
                            mediaItems = mediaItems,
                            onPlay = onPlay,
                            onOpenMediaDetail = onOpenMediaDetail,
                        )
                    }
                }

                items(dashboard.libraryLatestSections, key = { it.id }) { section ->
                    val sourceItems = state.dashboard.libraryLatestSections
                        .firstOrNull { it.library.id == section.id }
                        ?.items
                        .orEmpty()
                    SectionHeader(
                        title = section.title.toHomeLibrarySectionTitle(),
                        actionLabel = "更多",
                        onAction = { onOpenLibrary(section.id) },
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    PosterShelfRow(
                        cards = section.items,
                        mediaItems = section.items.mapNotNull { card ->
                            sourceItems.firstOrNull { it.id == card.id }
                        },
                        onPlay = onPlay,
                        onOpenMediaDetail = onOpenMediaDetail,
                    )
                }

                item {
                    Box(modifier = Modifier.padding(bottom = 108.dp))
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
                if (item.enabled) {
                    when (item.id) {
                        SEARCH_NAVIGATION_ID -> onOpenSearch()
                        FAVORITES_NAVIGATION_ID -> onOpenFavorites()
                        SETTINGS_NAVIGATION_ID -> onOpenSettings()
                        else -> {
                            val discoveryKind = HomeDashboardMapper.discoveryKindFromNavigationId(item.id)
                            if (discoveryKind != null) {
                                onOpenDiscovery(discoveryKind)
                            } else {
                                onOpenLibrary(item.id)
                            }
                        }
                    }
                    drawerState = drawerState.close()
                }
            },
            onUnsupported = { hintMessage = it },
        )
        ConfirmationOverlay(
            confirmation = state.confirmation,
            onConfirm = onConfirm,
            onCancel = onCancelConfirmation,
        )
    }
}

@Composable
private fun SettingsScreen(
    preferences: com.embytv.ui.theme.ThemePreferences,
    onBack: () -> Unit,
    onSelectTheme: (AppThemeId) -> Unit,
    onSetHighContrast: (Boolean) -> Unit,
    onSelectFontScale: (FontScale) -> Unit,
    onSelectLanguage: (AppLanguage) -> Unit,
) {
    val backFocusRequester = remember { FocusRequester() }
    BackHandler(enabled = true, onBack = onBack)
    LaunchedEffect(Unit) {
        backFocusRequester.requestFocus()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                horizontal = CinematicGlassSpacing.SafeAreaX,
                vertical = CinematicGlassSpacing.SafeAreaY,
            ),
        verticalArrangement = Arrangement.spacedBy(26.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    RoundIconButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回首页",
                        onClick = onBack,
                        modifier = Modifier.focusRequester(backFocusRequester),
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "显示与辅助设置",
                            color = CinematicGlassColors.OnSurface,
                            fontSize = 34.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "当前主题：${preferences.themeId.displayName}",
                            color = CinematicGlassColors.OnSurfaceVariant,
                            fontSize = 14.sp,
                        )
                    }
                }
            }
        }
        item {
            SettingsSection(title = "主题") {
                AppThemeId.entries.forEach { themeId ->
                    SettingsOptionChip(
                        title = themeId.displayName,
                        selected = preferences.themeId == themeId && !preferences.highContrast,
                        onClick = { onSelectTheme(themeId) },
                    )
                }
            }
        }
        item {
            SettingsSection(title = "可访问性") {
                SettingsOptionChip(
                    title = "高对比度",
                    selected = preferences.highContrast,
                    onClick = { onSetHighContrast(!preferences.highContrast) },
                )
                FontScale.entries.forEach { scale ->
                    SettingsOptionChip(
                        title = "字体 ${scale.displayName}",
                        selected = preferences.fontScale == scale,
                        onClick = { onSelectFontScale(scale) },
                    )
                }
            }
        }
        item {
            SettingsSection(title = "语言") {
                AppLanguage.entries.forEach { language ->
                    SettingsOptionChip(
                        title = language.displayName,
                        selected = preferences.language == language,
                        onClick = { onSelectLanguage(language) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader(title = title)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            content()
        }
    }
}

@Composable
private fun SettingsOptionChip(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    FocusableGlassSurface(
        cornerRadius = 999.dp,
        onClick = onClick,
    ) {
        Text(
            text = if (selected) "$title ✓" else title,
            color = if (selected) CinematicGlassColors.Primary else CinematicGlassColors.OnSurface,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 11.dp),
        )
    }
}

@Composable
private fun FavoriteContentScreen(
    state: FavoriteContentUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onSelectCategory: (FavoriteCategory) -> Unit,
    onPlay: (MediaItemSummary) -> Unit,
    onOpenMediaDetail: (MediaItemSummary) -> Unit,
    onUnsupported: (String) -> Unit,
) {
    val backFocusRequester = remember { FocusRequester() }
    val uiModel = remember(state.dashboard, state.selectedCategory) {
        HomeFavoritesMapper.map(state.dashboard, state.selectedCategory)
    }
    val mediaItems = remember(state.dashboard, state.selectedCategory) {
        when (state.selectedCategory) {
            FavoriteCategory.Movie -> state.dashboard.movies
            FavoriteCategory.Series -> state.dashboard.series
        }
    }
    BackHandler(enabled = true, onBack = onBack)
    LaunchedEffect(Unit) {
        backFocusRequester.requestFocus()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                horizontal = CinematicGlassSpacing.SafeAreaX,
                vertical = CinematicGlassSpacing.SafeAreaY,
            ),
        verticalArrangement = Arrangement.spacedBy(28.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    RoundIconButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回首页",
                        onClick = onBack,
                        modifier = Modifier.focusRequester(backFocusRequester),
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = uiModel.title,
                            color = CinematicGlassColors.OnSurface,
                            fontSize = 34.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = state.favoriteStatusLabel(),
                            color = CinematicGlassColors.OnSurfaceVariant,
                            fontSize = 14.sp,
                        )
                    }
                }
                if (state.errorMessage != null) {
                    PrimaryTvButton(
                        text = "重试",
                        icon = Icons.Filled.Refresh,
                        onClick = onRetry,
                    )
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                uiModel.categoryTabs.forEach { tab ->
                    PrimaryTvButton(
                        text = "${if (tab.selected) "当前 " else ""}${tab.title} · ${tab.countLabel}",
                        icon = when (tab.category) {
                            FavoriteCategory.Movie -> Icons.Filled.Movie
                            FavoriteCategory.Series -> Icons.Filled.Tv
                        },
                        onClick = { onSelectCategory(tab.category) },
                    )
                }
            }
        }

        when {
            state.isLoading -> item { MediaGridSkeleton(rowCount = 2) }
            state.errorMessage != null -> item {
                LibraryStatePanel(
                    title = "收藏加载失败",
                    subtitle = state.errorMessage,
                    errorType = ErrorType.Network,
                    onRetry = onRetry,
                )
            }
            uiModel.isEmpty -> item { LibraryStatePanel(title = uiModel.emptyTitle, subtitle = uiModel.emptySubtitle, empty = true) }
            else -> items(mediaItems.chunked(5), key = { row -> row.joinToString { it.id } }) { rowItems ->
                FavoriteGridRow(
                    items = rowItems,
                    cards = uiModel.items,
                    onPlay = onPlay,
                    onOpenMediaDetail = onOpenMediaDetail,
                    onUnsupported = onUnsupported,
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(108.dp))
        }
    }
}

@Composable
private fun FavoriteGridRow(
    items: List<MediaItemSummary>,
    cards: List<MediaCardUiModel>,
    onPlay: (MediaItemSummary) -> Unit,
    onOpenMediaDetail: (MediaItemSummary) -> Unit,
    onUnsupported: (String) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(CinematicGlassSpacing.CardGap),
    ) {
        items.forEach { item ->
            val card = cards.firstOrNull { it.id == item.id } ?: HomeDashboardMapper.mapMediaItem(item)
            MediaPosterCard(
                card = card,
                modifier = Modifier.weight(1f),
                onClick = {
                    if (item.opensDetail()) {
                        onOpenMediaDetail(item)
                    } else if (item.type.equals("Episode", ignoreCase = true)) {
                        onPlay(item)
                    } else {
                        onUnsupported("该资源暂不支持打开")
                    }
                },
            )
        }
        repeat(5 - items.size) {
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun LibraryContentScreen(
    state: LibraryContentUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onPlay: (MediaItemSummary) -> Unit,
    onOpenMediaDetail: (MediaItemSummary) -> Unit,
    onUnsupported: (String) -> Unit,
) {
    val backFocusRequester = remember { FocusRequester() }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var scrollIndicatorVisible by remember { mutableStateOf(false) }
    BackHandler(enabled = true, onBack = onBack)
    LaunchedEffect(state.selectedLibraryId) {
        backFocusRequester.requestFocus()
    }
    LaunchedEffect(scrollIndicatorVisible) {
        if (scrollIndicatorVisible) {
            kotlinx.coroutines.delay(2_000)
            scrollIndicatorVisible = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    horizontal = CinematicGlassSpacing.SafeAreaX,
                    vertical = CinematicGlassSpacing.SafeAreaY,
                ),
            verticalArrangement = Arrangement.spacedBy(28.dp),
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.CenterVertically) {
                        RoundIconButton(
                            icon = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回首页",
                            onClick = onBack,
                            modifier = Modifier.focusRequester(backFocusRequester),
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = state.content?.library?.name ?: "媒体库",
                                color = CinematicGlassColors.OnSurface,
                                fontSize = 34.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = state.statusLabel(),
                                color = CinematicGlassColors.OnSurfaceVariant,
                                fontSize = 14.sp,
                            )
                        }
                    }
                    if (state.errorMessage != null) {
                        PrimaryTvButton(
                            text = "重试",
                            icon = Icons.Filled.Refresh,
                            onClick = onRetry,
                        )
                    }
                }
            }

            when {
                state.isLoading -> item { MediaGridSkeleton(rowCount = 3) }
                state.errorMessage != null -> item {
                    LibraryStatePanel(
                        title = "媒体库加载失败",
                        subtitle = state.errorMessage,
                        errorType = ErrorType.Network,
                        onRetry = onRetry,
                    )
                }
                state.content?.items.isNullOrEmpty() -> item { LibraryStatePanel(title = "该媒体库暂无可展示资源", subtitle = "当前列表只展示电影和剧集资源。", empty = true) }
                else -> items(state.content.items.chunked(5), key = { row -> row.joinToString { it.id } }) { rowItems ->
                    LibraryGridRow(
                        items = rowItems,
                        onPlay = onPlay,
                        onOpenMediaDetail = onOpenMediaDetail,
                        onUnsupported = onUnsupported,
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(108.dp))
            }
        }
        val contentItems = state.content?.items.orEmpty()
        if (contentItems.size >= 20) {
            AlphabetIndexBar(
                items = contentItems,
                onIndexClick = { letter ->
                    val itemIndex = contentItems.findIndexByLetter(letter)
                    val rowIndex = (itemIndex / 5) + 1
                    coroutineScope.launch {
                        listState.animateScrollToItem(rowIndex)
                        scrollIndicatorVisible = true
                    }
                },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 18.dp),
            )
        }
        ScrollPositionIndicator(
            currentIndex = ((listState.firstVisibleItemIndex - 1).coerceAtLeast(0) * 5 + 1)
                .coerceAtMost(contentItems.size.coerceAtLeast(1)),
            totalCount = contentItems.size,
            visible = scrollIndicatorVisible,
            modifier = Modifier.align(Alignment.Center),
        )
    }
}

@Composable
private fun LibraryGridRow(
    items: List<MediaItemSummary>,
    onPlay: (MediaItemSummary) -> Unit,
    onOpenMediaDetail: (MediaItemSummary) -> Unit,
    onUnsupported: (String) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(CinematicGlassSpacing.CardGap),
    ) {
        items.forEach { item ->
            val card = HomeDashboardMapper.mapMediaItem(item)
            MediaPosterCard(
                card = card,
                modifier = Modifier.weight(1f),
                onClick = {
                    if (item.opensDetail()) {
                        onOpenMediaDetail(item)
                    } else if (item.type.equals("Episode", ignoreCase = true)) {
                        onPlay(item)
                    } else {
                        onUnsupported("该资源暂不支持打开")
                    }
                },
            )
        }
        repeat(5 - items.size) {
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun LibraryStatePanel(
    title: String,
    subtitle: String,
    errorType: ErrorType? = null,
    empty: Boolean = false,
    onRetry: (() -> Unit)? = null,
) {
    val inferredErrorType = errorType ?: if ("失败" in title) ErrorType.Network else null
    when {
        inferredErrorType != null -> ErrorStatePanel(
            title = title,
            subtitle = subtitle,
            errorType = inferredErrorType,
            onRetry = onRetry,
        )
        empty -> EmptyStatePanel(
            title = title,
            subtitle = subtitle,
        )
        else -> EmptyStatePanel(
            title = title,
            subtitle = subtitle,
        )
    }
}

private fun LibraryContentUiState.statusLabel(): String {
    if (isLoading) return "正在加载"
    errorMessage?.let { return "加载失败" }
    val content = content ?: return "等待加载"
    val type = when (content.library.collectionType?.lowercase()) {
        "movies" -> "电影"
        "tvshows" -> "剧集"
        else -> "资源"
    }
    return "${content.items.size} 个$type"
}

private fun FavoriteContentUiState.favoriteStatusLabel(): String {
    if (isLoading) return "正在加载"
    errorMessage?.let { return "加载失败" }
    return "${dashboard.movies.size} 部电影 · ${dashboard.series.size} 部剧集"
}

@Composable
private fun SearchScreen(
    state: SearchUiState,
    onBack: () -> Unit,
    onQueryChange: (String) -> Unit,
    onRetry: () -> Unit,
    onClear: () -> Unit,
    onSelectHistory: (SearchHistoryItem) -> Unit,
    onRemoveHistory: (String) -> Unit,
    onClearHistory: () -> Unit,
    onPlay: (MediaItemSummary) -> Unit,
    onOpenMediaDetail: (MediaItemSummary) -> Unit,
    onUnsupported: (String) -> Unit,
) {
    val backFocusRequester = remember { FocusRequester() }
    val queryFocusRequester = remember { FocusRequester() }
    val cards = remember(state.results) { SearchMapper.map(state.results) }
    BackHandler(enabled = true, onBack = onBack)
    LaunchedEffect(Unit) {
        queryFocusRequester.requestFocus()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                horizontal = CinematicGlassSpacing.SafeAreaX,
                vertical = CinematicGlassSpacing.SafeAreaY,
            ),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        item {
            DetailTopBar(
                title = "搜索",
                subtitle = state.searchStatusLabel(),
                backFocusRequester = backFocusRequester,
                onBack = onBack,
                onRetry = if (state.errorMessage != null) onRetry else null,
            )
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GlassPanel(modifier = Modifier.weight(1f), cornerRadius = 12.dp) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        androidx.tv.material3.Icon(
                            Icons.Filled.Search,
                            contentDescription = null,
                            tint = CinematicGlassColors.Primary,
                        )
                        BasicTextField(
                            value = state.query,
                            onValueChange = onQueryChange,
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(queryFocusRequester),
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(
                                color = CinematicGlassColors.OnSurface,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.SemiBold,
                            ),
                            decorationBox = { inner ->
                                if (state.query.isBlank()) {
                                    Text(
                                        text = "输入电影、剧集、合集或播放列表",
                                        color = CinematicGlassColors.OnSurfaceVariant,
                                        fontSize = 20.sp,
                                    )
                                }
                                inner()
                            },
                        )
                    }
                }
                PrimaryTvButton(
                    text = "清空",
                    icon = Icons.Filled.Clear,
                    enabled = state.query.isNotBlank(),
                    onClick = onClear,
                )
            }
        }

        if (state.query.isBlank() && state.history.isNotEmpty()) {
            item {
                SearchHistoryPanel(
                    history = state.history,
                    onHistoryClick = onSelectHistory,
                    onHistoryRemove = onRemoveHistory,
                    onClearAll = onClearHistory,
                )
            }
        }

        when {
            state.query.isBlank() -> item {
                LibraryStatePanel(
                    title = if (state.history.isEmpty()) "输入关键词开始搜索" else "选择最近搜索或输入关键词",
                    subtitle = "可用遥控器选择输入框，也可以连接实体键盘输入。",
                )
            }
            state.isLoading -> item { MediaGridSkeleton(rowCount = 2) }
            state.errorMessage != null -> item { LibraryStatePanel(title = "搜索失败", subtitle = state.errorMessage) }
            state.results.items.isEmpty() -> item { LibraryStatePanel(title = "没有找到结果", subtitle = "换一个关键词再试。") }
            else -> items(state.results.items.chunked(5), key = { row -> row.joinToString { it.id } }) { rowItems ->
                MediaGridRow(
                    items = rowItems,
                    cards = cards,
                    onPlay = onPlay,
                    onOpenMediaDetail = onOpenMediaDetail,
                    onUnsupported = onUnsupported,
                )
            }
        }

        item { Spacer(modifier = Modifier.height(108.dp)) }
    }
}

@Composable
private fun SearchHistoryPanel(
    history: List<SearchHistoryItem>,
    onHistoryClick: (SearchHistoryItem) -> Unit,
    onHistoryRemove: (String) -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "最近搜索",
                color = CinematicGlassColors.OnSurface,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
            )
            PrimaryTvButton(
                text = "清空",
                icon = Icons.Filled.Clear,
                onClick = onClearAll,
            )
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(history, key = { it.query }) { item ->
                SearchHistoryChip(
                    item = item,
                    onClick = { onHistoryClick(item) },
                    onRemove = { onHistoryRemove(item.query) },
                )
            }
        }
    }
}

@Composable
private fun SearchHistoryChip(
    item: SearchHistoryItem,
    onClick: () -> Unit,
    onRemove: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    FocusableGlassSurface(
        modifier = Modifier.onFocusChanged { focused = it.isFocused },
        cornerRadius = 999.dp,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            androidx.tv.material3.Icon(
                imageVector = Icons.Filled.History,
                contentDescription = null,
                tint = CinematicGlassColors.OnSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = item.query,
                color = CinematicGlassColors.OnSurface,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${item.resultCount} 个结果",
                color = CinematicGlassColors.OnSurfaceVariant,
                fontSize = 12.sp,
                maxLines = 1,
            )
            if (focused) {
                androidx.tv.material3.Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "删除搜索历史",
                    tint = CinematicGlassColors.Error,
                    modifier = Modifier
                        .size(16.dp)
                        .clickable(onClick = onRemove),
                )
            }
        }
    }
}

@Composable
private fun DiscoveryScreen(
    state: DiscoveryContentUiState,
    onBack: () -> Unit,
    onClose: () -> Unit,
    onRetry: () -> Unit,
    onRetryEntry: () -> Unit,
    onOpenEntry: (DiscoveryEntrySummary) -> Unit,
    onPlay: (MediaItemSummary) -> Unit,
    onOpenMediaDetail: (MediaItemSummary) -> Unit,
    onUnsupported: (String) -> Unit,
) {
    val backFocusRequester = remember { FocusRequester() }
    BackHandler(enabled = true, onBack = onBack)
    LaunchedEffect(state.kind, state.isEntryOpen) {
        backFocusRequester.requestFocus()
    }

    if (state.isEntryOpen) {
        DiscoveryEntryItemsScreen(
            state = state,
            backFocusRequester = backFocusRequester,
            onBack = onBack,
            onRetry = onRetryEntry,
            onPlay = onPlay,
            onOpenMediaDetail = onOpenMediaDetail,
            onUnsupported = onUnsupported,
        )
        return
    }

    val kind = state.kind ?: DiscoveryKind.Collections
    val content = state.content
    val cards = remember(content) { content?.let(DiscoveryMapper::entryCards).orEmpty() }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                horizontal = CinematicGlassSpacing.SafeAreaX,
                vertical = CinematicGlassSpacing.SafeAreaY,
            ),
        verticalArrangement = Arrangement.spacedBy(28.dp),
    ) {
        item {
            DetailTopBar(
                title = DiscoveryMapper.title(kind),
                subtitle = state.discoveryStatusLabel(),
                backFocusRequester = backFocusRequester,
                onBack = onClose,
                onRetry = if (state.errorMessage != null) onRetry else null,
            )
        }
        when {
            state.isLoading -> item { MediaGridSkeleton(rowCount = 2) }
            state.errorMessage != null -> item { LibraryStatePanel(title = "${DiscoveryMapper.title(kind)}加载失败", subtitle = state.errorMessage) }
            content?.entries.isNullOrEmpty() -> item { LibraryStatePanel(title = "暂无${DiscoveryMapper.title(kind)}", subtitle = "当前服务器没有返回可展示内容。") }
            else -> items(content.entries.chunked(5), key = { row -> row.joinToString { it.id } }) { rowEntries ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(CinematicGlassSpacing.CardGap),
                ) {
                    rowEntries.forEach { entry ->
                        val card = cards.firstOrNull { it.id == entry.id }
                            ?: MediaCardUiModel(entry.id, entry.name, entry.kind.titleLabel(), entry.imageUrl, 0f, entry.type)
                        MediaPosterCard(
                            card = card,
                            modifier = Modifier.weight(1f),
                            onClick = { onOpenEntry(entry) },
                        )
                    }
                    repeat(5 - rowEntries.size) { Spacer(modifier = Modifier.weight(1f)) }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(108.dp)) }
    }
}

@Composable
private fun DiscoveryEntryItemsScreen(
    state: DiscoveryContentUiState,
    backFocusRequester: FocusRequester,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onPlay: (MediaItemSummary) -> Unit,
    onOpenMediaDetail: (MediaItemSummary) -> Unit,
    onUnsupported: (String) -> Unit,
) {
    val entry = state.selectedEntry
    val items = state.entryItems
    val cards = remember(items) { items?.let(DiscoveryMapper::itemCards).orEmpty() }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                horizontal = CinematicGlassSpacing.SafeAreaX,
                vertical = CinematicGlassSpacing.SafeAreaY,
            ),
        verticalArrangement = Arrangement.spacedBy(28.dp),
    ) {
        item {
            DetailTopBar(
                title = entry?.name ?: "发现内容",
                subtitle = state.discoveryEntryStatusLabel(),
                backFocusRequester = backFocusRequester,
                onBack = onBack,
                onRetry = if (state.entryErrorMessage != null) onRetry else null,
            )
        }
        when {
            state.isEntryLoading -> item { MediaGridSkeleton(rowCount = 2) }
            state.entryErrorMessage != null -> item { LibraryStatePanel(title = "资源加载失败", subtitle = state.entryErrorMessage) }
            items?.items.isNullOrEmpty() -> item { LibraryStatePanel(title = "暂无资源", subtitle = "Emby 没有返回该入口下的媒体资源。") }
            else -> items(items.items.chunked(5), key = { row -> row.joinToString { it.id } }) { rowItems ->
                MediaGridRow(
                    items = rowItems,
                    cards = cards,
                    onPlay = onPlay,
                    onOpenMediaDetail = onOpenMediaDetail,
                    onUnsupported = onUnsupported,
                )
            }
        }
        item { Spacer(modifier = Modifier.height(108.dp)) }
    }
}

@Composable
private fun MediaGridRow(
    items: List<MediaItemSummary>,
    cards: List<MediaCardUiModel>,
    onPlay: (MediaItemSummary) -> Unit,
    onOpenMediaDetail: (MediaItemSummary) -> Unit,
    onUnsupported: (String) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(CinematicGlassSpacing.CardGap),
    ) {
        items.forEach { item ->
            val card = cards.firstOrNull { it.id == item.id } ?: HomeDashboardMapper.mapMediaItem(item)
            MediaPosterCard(
                card = card,
                modifier = Modifier.weight(1f),
                onClick = {
                    when {
                        item.opensDetail() -> onOpenMediaDetail(item)
                        item.type.equals("Episode", ignoreCase = true) -> onPlay(item)
                        else -> onUnsupported("该资源暂不支持打开")
                    }
                },
            )
        }
        repeat(5 - items.size) { Spacer(modifier = Modifier.weight(1f)) }
    }
}

private fun SearchUiState.searchStatusLabel(): String = when {
    isLoading -> "正在搜索"
    errorMessage != null -> "搜索失败"
    query.isBlank() -> "等待输入"
    else -> "${results.items.size} 个结果"
}

private fun DiscoveryContentUiState.discoveryStatusLabel(): String = when {
    isLoading -> "正在加载"
    errorMessage != null -> "加载失败"
    else -> "${content?.entries?.size ?: 0} 个入口"
}

private fun DiscoveryContentUiState.discoveryEntryStatusLabel(): String = when {
    isEntryLoading -> "正在加载"
    entryErrorMessage != null -> "加载失败"
    else -> "${entryItems?.items?.size ?: 0} 个资源"
}

@Composable
private fun HomeReferenceTopBar(
    title: String,
    onMenuClick: () -> Unit,
    onSearchClick: () -> Unit,
    onFavoritesClick: () -> Unit,
    onSettingsClick: () -> Unit,
    menuFocusRequester: FocusRequester,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp),
    ) {
        Row(
            modifier = Modifier.align(Alignment.CenterStart),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RoundIconButton(
                icon = Icons.Filled.Menu,
                contentDescription = "打开导航",
                onClick = onMenuClick,
                modifier = Modifier.focusRequester(menuFocusRequester),
            )
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(CinematicGlassColors.SurfaceHigh)
                    .border(1.dp, Color.White.copy(alpha = 0.16f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                androidx.tv.material3.Icon(
                    imageVector = Icons.Filled.Tv,
                    contentDescription = null,
                    tint = CinematicGlassColors.OnSurface,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
        Text(
            text = title,
            color = CinematicGlassColors.OnSurfaceVariant,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Center),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Row(
            modifier = Modifier.align(Alignment.CenterEnd),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RoundIconButton(Icons.Filled.Search, "搜索", onSearchClick)
            RoundIconButton(Icons.Filled.Star, "收藏", onFavoritesClick)
            RoundIconButton(Icons.Filled.Settings, "设置", onSettingsClick)
        }
    }
}

@Composable
private fun HomeLibraryTile(
    library: LibrarySummaryUiModel,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onUnsupported: (String) -> Unit,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FocusableGlassSurface(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .accessibilityLabel(label = library.title, state = library.countLabel),
            cornerRadius = 8.dp,
            enabled = library.enabled,
            disabledReason = library.disabledReason,
            onClick = onClick,
            onDisabledClick = onUnsupported,
        ) { focused ->
            Box(modifier = Modifier.fillMaxSize()) {
                NetworkBackdropImage(
                    imageUrl = library.imageUrl,
                    contentDescription = library.title,
                    modifier = Modifier.fillMaxSize(),
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = if (focused) 0.45f else 0.28f),
                                ),
                            ),
                        ),
                )
            }
        }
        Text(
            text = library.title,
            color = CinematicGlassColors.OnSurface,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ContinueWatchingRow(
    cards: List<MediaCardUiModel>,
    mediaItems: List<MediaItemSummary>,
    onPlay: (MediaItemSummary) -> Unit,
    onOpenMediaDetail: (MediaItemSummary) -> Unit,
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(mediaItems, key = { it.id }) { item ->
            val card = cards.firstOrNull { it.id == item.id } ?: return@items
            ContinueWatchingTile(
                card = card,
                modifier = Modifier.fillParentMaxWidth(0.185f),
                onClick = {
                    if (item.opensDetail()) {
                        onOpenMediaDetail(item)
                    } else {
                        onPlay(item)
                    }
                },
            )
        }
    }
}

@Composable
private fun ContinueWatchingTile(
    card: MediaCardUiModel,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        FocusableGlassSurface(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .accessibilityLabel(label = card.title, state = card.subtitle),
            cornerRadius = 8.dp,
            onClick = onClick,
        ) { focused ->
            Box(modifier = Modifier.fillMaxSize()) {
                NetworkBackdropImage(
                    imageUrl = card.imageUrl,
                    contentDescription = card.title,
                    modifier = Modifier.fillMaxSize(),
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = if (focused) 0.32f else 0.18f),
                                ),
                            ),
                        ),
                )
                card.remainingLabel()?.let { label ->
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(10.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(CinematicGlassColors.Primary.copy(alpha = 0.86f))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    ) {
                        Text(
                            text = label,
                            color = Color.White,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                if (card.progressFraction > 0f) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .height(4.dp)
                            .background(Color.White.copy(alpha = 0.25f)),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(card.progressFraction.coerceIn(0f, 1f))
                                .fillMaxHeight()
                                .background(CinematicGlassColors.Primary),
                        )
                    }
                }
            }
        }
        Text(
            text = card.title,
            color = CinematicGlassColors.OnSurface,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = card.subtitle,
            color = CinematicGlassColors.OnSurfaceVariant,
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun PosterShelfRow(
    cards: List<MediaCardUiModel>,
    mediaItems: List<MediaItemSummary>,
    onPlay: (MediaItemSummary) -> Unit,
    onOpenMediaDetail: (MediaItemSummary) -> Unit,
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        items(mediaItems, key = { it.id }) { item ->
            val card = cards.firstOrNull { it.id == item.id } ?: return@items
            CompactPosterTile(
                card = card,
                modifier = Modifier.fillParentMaxWidth(0.102f),
                onClick = {
                    if (item.opensDetail()) {
                        onOpenMediaDetail(item)
                    } else {
                        onPlay(item)
                    }
                },
            )
        }
    }
}

@Composable
private fun CompactPosterTile(
    card: MediaCardUiModel,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        FocusableGlassSurface(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .accessibilityLabel(label = card.title, state = card.subtitle),
            cornerRadius = 8.dp,
            onClick = onClick,
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                NetworkBackdropImage(
                    imageUrl = card.imageUrl,
                    contentDescription = card.title,
                    modifier = Modifier.fillMaxSize(),
                )
                card.cornerBadge?.takeIf { it.isNotBlank() }?.let { badge ->
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .clip(CircleShape)
                            .background(CinematicGlassColors.Primary.copy(alpha = 0.9f))
                            .size(30.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = badge,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
        Text(
            text = card.title,
            color = CinematicGlassColors.OnSurface,
            fontSize = 16.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = card.subtitle,
            color = CinematicGlassColors.OnSurfaceVariant,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun MediaRow(
    cards: List<MediaCardUiModel>,
    mediaItems: List<MediaItemSummary>,
    onPlay: (MediaItemSummary) -> Unit,
    onOpenMediaDetail: (MediaItemSummary) -> Unit,
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(CinematicGlassSpacing.CardGap)) {
        items(mediaItems, key = { it.id }) { item ->
            val card = cards.firstOrNull { it.id == item.id } ?: return@items
            MediaPosterCard(
                card = card,
                modifier = Modifier.fillParentMaxWidth(0.16f),
                onClick = {
                    if (item.opensDetail()) {
                        onOpenMediaDetail(item)
                    } else {
                        onPlay(item)
                    }
                },
            )
        }
    }
}

@Composable
private fun MediaDetailScreen(
    state: MediaDetailUiState,
    onBack: () -> Unit,
    onClose: () -> Unit,
    onRetry: () -> Unit,
    onOpenSeason: (com.embytv.domain.model.EmbySeasonSummary) -> Unit,
    onToggleFavorite: (MediaItemSummary) -> Unit,
    onTogglePlayed: (MediaItemSummary) -> Unit,
    onClearResumeProgress: (MediaItemSummary) -> Unit,
    onPlay: (MediaItemSummary) -> Unit,
) {
    BackHandler(enabled = true, onBack = onBack)
    val backFocusRequester = remember { FocusRequester() }
    LaunchedEffect(state.requestedItem?.id, state.isSeasonOpen) {
        backFocusRequester.requestFocus()
    }

    if (state.isSeasonOpen) {
        SeasonEpisodesScreen(
            state = state,
            backFocusRequester = backFocusRequester,
            onBack = onBack,
            onRetry = {
                state.selectedSeason?.let(onOpenSeason)
            },
            onPlay = onPlay,
        )
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                horizontal = CinematicGlassSpacing.SafeAreaX,
                vertical = CinematicGlassSpacing.SafeAreaY,
            ),
        verticalArrangement = Arrangement.spacedBy(28.dp),
    ) {
        item {
            DetailTopBar(
                title = state.requestedItem?.name ?: state.detail?.item?.name ?: "媒体详情",
                subtitle = state.detailStatusLabel(),
                backFocusRequester = backFocusRequester,
                onBack = onClose,
                onRetry = if (state.errorMessage != null) onRetry else null,
            )
        }

        when {
            state.isLoading -> item { DetailSkeleton() }
            state.errorMessage != null -> item { LibraryStatePanel(title = "详情加载失败", subtitle = state.errorMessage) }
            state.detail == null -> item { LibraryStatePanel(title = "暂无详情", subtitle = "Emby 没有返回该媒体的详情数据。") }
            else -> item {
                MediaDetailContent(
                    detail = state.detail,
                    onOpenSeason = onOpenSeason,
                    onToggleFavorite = onToggleFavorite,
                    onTogglePlayed = onTogglePlayed,
                    onClearResumeProgress = onClearResumeProgress,
                    onPlay = onPlay,
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(108.dp))
        }
    }
}

@Composable
private fun MediaDetailContent(
    detail: com.embytv.domain.model.EmbyMediaDetail,
    onOpenSeason: (com.embytv.domain.model.EmbySeasonSummary) -> Unit,
    onToggleFavorite: (MediaItemSummary) -> Unit,
    onTogglePlayed: (MediaItemSummary) -> Unit,
    onClearResumeProgress: (MediaItemSummary) -> Unit,
    onPlay: (MediaItemSummary) -> Unit,
) {
    val uiModel = remember(detail) { HomeMediaDetailMapper.map(detail) }
    val playFocusRequester = remember { FocusRequester() }
    LaunchedEffect(detail.item.id) {
        if (uiModel.isMovie) {
            playFocusRequester.requestFocus()
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(28.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(28.dp),
        ) {
            GlassPanel(
                modifier = Modifier
                    .width(260.dp)
                    .aspectRatio(2f / 3f),
                cornerRadius = 10.dp,
            ) {
                NetworkBackdropImage(
                    imageUrl = uiModel.imageUrl,
                    contentDescription = uiModel.title,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = uiModel.title,
                    color = CinematicGlassColors.OnSurface,
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (uiModel.metadata.isNotBlank()) {
                    Text(
                        text = uiModel.metadata,
                        color = CinematicGlassColors.Primary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Text(
                    text = uiModel.overview,
                    color = CinematicGlassColors.OnSurfaceVariant,
                    fontSize = 17.sp,
                    lineHeight = 24.sp,
                    maxLines = 8,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (uiModel.isMovie) {
                        PrimaryTvButton(
                            text = "播放",
                            icon = Icons.Filled.PlayArrow,
                            onClick = { onPlay(detail.item) },
                            modifier = Modifier.focusRequester(playFocusRequester),
                        )
                    } else {
                        val firstSeason = detail.seasons.firstOrNull()
                        PrimaryTvButton(
                            text = "查看季列表",
                            icon = Icons.Filled.Tv,
                            enabled = firstSeason != null,
                            onClick = { firstSeason?.let(onOpenSeason) },
                            modifier = Modifier.focusRequester(playFocusRequester),
                        )
                    }
                    PrimaryTvButton(
                        text = if (detail.item.isFavorite) "取消收藏" else "收藏",
                        icon = Icons.Filled.Star,
                        onClick = { onToggleFavorite(detail.item) },
                    )
                    PrimaryTvButton(
                        text = if (detail.item.played) "标记未播放" else "标记已播放",
                        icon = Icons.Filled.Refresh,
                        onClick = { onTogglePlayed(detail.item) },
                    )
                    if (detail.item.playbackPositionTicks > 0L) {
                        PrimaryTvButton(
                            text = "清除进度",
                            icon = Icons.Filled.Clear,
                            onClick = { onClearResumeProgress(detail.item) },
                        )
                    }
                }
            }
        }

        if (uiModel.mediaFacts.isNotEmpty()) {
            DetailInfoSection(title = "媒体信息") {
                uiModel.mediaFacts.chunked(4).forEach { rowFacts ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        rowFacts.forEach { fact ->
                            DetailFactCard(
                                label = fact.label,
                                value = fact.value,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        repeat(4 - rowFacts.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        if (uiModel.castMembers.isNotEmpty()) {
            DetailInfoSection(title = "演员信息") {
                uiModel.castMembers.chunked(4).forEach { rowMembers ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        rowMembers.forEach { member ->
                            DetailCastCard(
                                name = member.name,
                                role = member.role,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        repeat(4 - rowMembers.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        if (!uiModel.isMovie) {
            SectionHeader(title = "季")
            if (detail.seasons.isEmpty()) {
                LibraryStatePanel(title = "暂无季列表", subtitle = "Emby 没有返回该剧集的季数据。")
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(CinematicGlassSpacing.CardGap)) {
                    items(detail.seasons, key = { it.id }) { season ->
                        val card = uiModel.seasons.firstOrNull { it.id == season.id } ?: season.toFallbackCard()
                        MediaPosterCard(
                            card = MediaCardUiModel(
                                id = card.id,
                                title = card.title,
                                subtitle = card.subtitle,
                                imageUrl = card.imageUrl ?: uiModel.imageUrl,
                                progressFraction = 0f,
                                badge = "Season",
                                cornerBadge = card.cornerBadge,
                            ),
                            modifier = Modifier.fillParentMaxWidth(0.16f),
                            onClick = { onOpenSeason(season) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailInfoSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SectionHeader(title = title)
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            content = content,
        )
    }
}

@Composable
private fun DetailFactCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    GlassPanel(modifier = modifier, cornerRadius = 10.dp) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = label,
                color = CinematicGlassColors.OnSurfaceVariant,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = value,
                color = CinematicGlassColors.OnSurface,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun DetailCastCard(
    name: String,
    role: String?,
    modifier: Modifier = Modifier,
) {
    GlassPanel(modifier = modifier, cornerRadius = 10.dp) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = name,
                color = CinematicGlassColors.OnSurface,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = role?.let { "饰 $it" } ?: "演员",
                color = CinematicGlassColors.OnSurfaceVariant,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun SeasonEpisodesScreen(
    state: MediaDetailUiState,
    backFocusRequester: FocusRequester,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onPlay: (MediaItemSummary) -> Unit,
) {
    val episodes = state.seasonEpisodes
    val cards = remember(episodes) {
        episodes?.let { HomeMediaDetailMapper.mapEpisodes(it) }.orEmpty()
    }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                horizontal = CinematicGlassSpacing.SafeAreaX,
                vertical = CinematicGlassSpacing.SafeAreaY,
            ),
        verticalArrangement = Arrangement.spacedBy(28.dp),
    ) {
        item {
            DetailTopBar(
                title = state.selectedSeason?.name ?: "剧集",
                subtitle = state.seasonStatusLabel(),
                backFocusRequester = backFocusRequester,
                onBack = onBack,
                onRetry = if (state.seasonErrorMessage != null) onRetry else null,
            )
        }
        when {
            state.isSeasonLoading -> item { MediaGridSkeleton(rowCount = 2) }
            state.seasonErrorMessage != null -> item { LibraryStatePanel(title = "剧集加载失败", subtitle = state.seasonErrorMessage) }
            episodes?.episodes.isNullOrEmpty() -> item { LibraryStatePanel(title = "暂无剧集", subtitle = "Emby 没有返回该季的 Episode 数据。") }
            else -> items(episodes.episodes.chunked(5), key = { row -> row.joinToString { it.id } }) { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(CinematicGlassSpacing.CardGap),
                ) {
                    rowItems.forEach { item ->
                        val card = cards.firstOrNull { it.id == item.id } ?: HomeDashboardMapper.mapMediaItem(item)
                        MediaPosterCard(
                            card = card,
                            modifier = Modifier.weight(1f),
                            onClick = { onPlay(item) },
                        )
                    }
                    repeat(5 - rowItems.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
        item {
            Spacer(modifier = Modifier.height(108.dp))
        }
    }
}

@Composable
private fun DetailTopBar(
    title: String,
    subtitle: String,
    backFocusRequester: FocusRequester,
    onBack: () -> Unit,
    onRetry: (() -> Unit)?,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.CenterVertically) {
            RoundIconButton(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "返回",
                onClick = onBack,
                modifier = Modifier.focusRequester(backFocusRequester),
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = title,
                    color = CinematicGlassColors.OnSurface,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subtitle,
                    color = CinematicGlassColors.OnSurfaceVariant,
                    fontSize = 14.sp,
                )
            }
        }
        if (onRetry != null) {
            PrimaryTvButton(
                text = "重试",
                icon = Icons.Filled.Refresh,
                onClick = onRetry,
            )
        }
    }
}

@Composable
private fun ConfirmationOverlay(
    confirmation: HomeConfirmationUiState?,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    if (confirmation == null) return
    val cancelFocusRequester = remember { FocusRequester() }
    BackHandler(enabled = true, onBack = onCancel)
    LaunchedEffect(confirmation) {
        cancelFocusRequester.requestFocus()
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.68f)),
        contentAlignment = Alignment.Center,
    ) {
        GlassPanel(
            modifier = Modifier
                .fillMaxWidth(0.46f)
                .focusGroup(),
            cornerRadius = 12.dp,
        ) {
            Column(
                modifier = Modifier.padding(30.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Text(
                    text = confirmation.title,
                    color = CinematicGlassColors.OnSurface,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = confirmation.message,
                    color = CinematicGlassColors.OnSurfaceVariant,
                    fontSize = 16.sp,
                    lineHeight = 23.sp,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PrimaryTvButton(
                        text = "取消",
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        onClick = onCancel,
                        modifier = Modifier.focusRequester(cancelFocusRequester),
                    )
                    PrimaryTvButton(
                        text = confirmation.confirmLabel,
                        icon = Icons.Filled.Clear,
                        onClick = onConfirm,
                    )
                }
            }
        }
    }
}

private fun MediaDetailUiState.detailStatusLabel(): String {
    if (isLoading) return "正在加载"
    errorMessage?.let { return "加载失败" }
    val item = detail?.item ?: requestedItem ?: return "媒体详情"
    return when {
        item.type.equals("Movie", ignoreCase = true) -> "电影详情"
        item.type.equals("Series", ignoreCase = true) -> "电视剧详情"
        else -> "媒体详情"
    }
}

private fun MediaDetailUiState.seasonStatusLabel(): String {
    if (isSeasonLoading) return "正在加载"
    seasonErrorMessage?.let { return "加载失败" }
    return "${seasonEpisodes?.episodes?.size ?: selectedSeason?.episodeCount ?: 0} 集"
}

private fun com.embytv.domain.model.EmbySeasonSummary.toFallbackCard(): SeasonCardUiModel =
    SeasonCardUiModel(
        id = id,
        title = name.ifBlank { indexNumber?.let { "第 $it 季" } ?: id },
        subtitle = episodeCount?.let { "$it 集" } ?: "剧集",
        imageUrl = imageUrl,
        cornerBadge = unplayedItemCount?.takeIf { it > 0 }?.let { "剩 $it 集" },
    )

private fun MediaItemSummary.opensDetail(): Boolean =
    type.equals("Movie", ignoreCase = true) || type.equals("Series", ignoreCase = true)

@Composable
private fun SectionHeader(
    title: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            color = CinematicGlassColors.OnSurface,
            fontSize = 28.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (actionLabel != null && onAction != null) {
            FocusableGlassSurface(
                modifier = Modifier
                    .width(92.dp)
                    .height(44.dp),
                cornerRadius = 999.dp,
                onClick = onAction,
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = actionLabel,
                        color = CinematicGlassColors.OnSurface,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    }
}

private fun String.toHomeSectionTitle(): String =
    when (this) {
        "Continue Watching" -> "继续观看"
        else -> this
    }

private fun String.toHomeLibrarySectionTitle(): String =
    removeSuffix(" · 最新入库")

private fun MediaCardUiModel.remainingLabel(): String? =
    progressFraction
        .takeIf { it > 0f && it < 1f }
        ?.let { "已看 ${(it.coerceIn(0f, 1f) * 100).toInt()}%" }

@Composable
private fun EmptyDashboardPanel() {
    GlassPanel(modifier = Modifier.fillMaxWidth(), cornerRadius = 12.dp) {
        Column(
            modifier = Modifier.padding(28.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(text = "尚未加载媒体", color = CinematicGlassColors.OnSurface, fontSize = 22.sp)
            Text(
                text = "Emby 已连接，但当前接口没有返回继续观看或最近入库条目。",
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
                Text("选择 Emby 媒体开始播放", color = CinematicGlassColors.OnSurface, fontSize = 15.sp)
                Text("播放信息将在打开媒体时从服务器读取", color = CinematicGlassColors.OnSurfaceVariant, fontSize = 12.sp)
            }
            Text("Emby 已连接", color = CinematicGlassColors.Primary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}
