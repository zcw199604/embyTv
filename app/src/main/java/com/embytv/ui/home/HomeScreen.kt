package com.embytv.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tv
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.embytv.domain.model.MediaItemSummary
import com.embytv.domain.model.PlaybackSource
import com.embytv.ui.components.GlassPanel
import com.embytv.ui.components.LibraryCard
import com.embytv.ui.components.MediaPosterCard
import com.embytv.ui.components.NavigationDrawerPanel
import com.embytv.ui.components.NetworkBackdropImage
import com.embytv.ui.components.PrimaryTvButton
import com.embytv.ui.components.RemoteHint
import com.embytv.ui.components.RoundIconButton
import com.embytv.ui.components.TopChromeBar
import com.embytv.ui.setup.SetupScreen
import com.embytv.ui.theme.CinematicGlassColors
import com.embytv.ui.theme.CinematicGlassSpacing
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onPlay: (PlaybackSource) -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    if (state.session == null) {
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
    } else {
        HomeDashboardScreen(
            state = state,
            onOpenFavorites = viewModel::openFavorites,
            onCloseFavorites = viewModel::closeFavorites,
            onSelectFavoriteCategory = viewModel::selectFavoriteCategory,
            onRetryFavorites = viewModel::retryFavorites,
            onOpenLibrary = viewModel::openLibrary,
            onCloseLibrary = viewModel::closeLibrary,
            onRetryLibrary = viewModel::retryLibrary,
            onOpenMediaDetail = viewModel::openMediaDetail,
            onCloseMediaDetail = viewModel::closeMediaDetail,
            onBackFromDetail = viewModel::backFromDetail,
            onRetryMediaDetail = viewModel::retryMediaDetail,
            onOpenSeasonEpisodes = viewModel::openSeasonEpisodes,
            onPlay = { item ->
                coroutineScope.launch {
                    viewModel.createPlaybackSource(item)?.let(onPlay)
                }
            },
        )
    }
}

@Composable
private fun HomeDashboardScreen(
    state: HomeUiState,
    onOpenFavorites: () -> Unit,
    onCloseFavorites: () -> Unit,
    onSelectFavoriteCategory: (FavoriteCategory) -> Unit,
    onRetryFavorites: () -> Unit,
    onOpenLibrary: (String) -> Unit,
    onCloseLibrary: () -> Unit,
    onRetryLibrary: () -> Unit,
    onOpenMediaDetail: (MediaItemSummary) -> Unit,
    onCloseMediaDetail: () -> Unit,
    onBackFromDetail: () -> Unit,
    onRetryMediaDetail: () -> Unit,
    onOpenSeasonEpisodes: (com.embytv.domain.model.EmbySeasonSummary) -> Unit,
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
                onPlay = onPlay,
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
                    horizontal = CinematicGlassSpacing.SafeAreaX,
                    vertical = CinematicGlassSpacing.SafeAreaY,
                ),
            verticalArrangement = Arrangement.spacedBy(30.dp),
        ) {
            item {
                TopChromeBar(
                    title = "EMBY",
                    subtitle = "Subtitles: ON · v0.2.0",
                    onMenuClick = { drawerState = drawerState.open() },
                    menuFocusRequester = menuFocusRequester,
                )
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Welcome back",
                        color = CinematicGlassColors.Primary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                    )
                    Text(
                        text = "Your Personal Home Cinema.",
                        color = CinematicGlassColors.OnSurface,
                        fontSize = 52.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            item {
                SectionHeader(title = "Media Libraries")
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(CinematicGlassSpacing.CardGap)) {
                    items(dashboard.libraries, key = { it.id }) { library ->
                        LibraryCard(
                            library = library,
                            modifier = Modifier.fillParentMaxWidth(0.29f),
                            onClick = { onOpenLibrary(library.id) },
                            onUnsupported = { hintMessage = it },
                        )
                    }
                }
            }

            item {
                SectionHeader(title = dashboard.mediaSectionTitle)
            }
            item {
                if (mediaItems.isEmpty()) {
                    EmptyDashboardPanel()
                } else {
                    MediaRow(
                        cards = dashboard.continueWatching,
                        mediaItems = mediaItems,
                        onPlay = onPlay,
                        onOpenMediaDetail = onOpenMediaDetail,
                    )
                }
            }

            items(dashboard.libraryLatestSections, key = { it.id }) { section ->
                SectionHeader(title = section.title)
                MediaRow(
                    cards = section.items,
                    mediaItems = section.items.mapNotNull { card ->
                        state.dashboard.libraryLatestSections
                            .firstOrNull { it.library.id == section.id }
                            ?.items
                            ?.firstOrNull { it.id == card.id }
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
                    if (item.id == FAVORITES_NAVIGATION_ID) {
                        onOpenFavorites()
                    } else {
                        onOpenLibrary(item.id)
                    }
                    drawerState = drawerState.close()
                }
            },
            onUnsupported = { hintMessage = it },
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
            state.isLoading -> item { LibraryStatePanel(title = "正在加载收藏", subtitle = "正在从 Emby 获取你的收藏资源。") }
            state.errorMessage != null -> item { LibraryStatePanel(title = "收藏加载失败", subtitle = state.errorMessage) }
            uiModel.isEmpty -> item { LibraryStatePanel(title = uiModel.emptyTitle, subtitle = uiModel.emptySubtitle) }
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
    BackHandler(enabled = true, onBack = onBack)
    LaunchedEffect(state.selectedLibraryId) {
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
            state.isLoading -> item { LibraryStatePanel(title = "正在加载媒体库", subtitle = "正在从 Emby 获取该媒体库的资源列表。") }
            state.errorMessage != null -> item { LibraryStatePanel(title = "媒体库加载失败", subtitle = state.errorMessage) }
            state.content?.items.isNullOrEmpty() -> item { LibraryStatePanel(title = "该媒体库暂无可展示资源", subtitle = "当前列表只展示电影和剧集资源。") }
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
) {
    GlassPanel(modifier = Modifier.fillMaxWidth(), cornerRadius = 12.dp) {
        Column(
            modifier = Modifier.padding(28.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(text = title, color = CinematicGlassColors.OnSurface, fontSize = 22.sp)
            Text(text = subtitle, color = CinematicGlassColors.OnSurfaceVariant, fontSize = 16.sp)
        }
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
            state.isLoading -> item { LibraryStatePanel(title = "正在加载详情", subtitle = "正在从 Emby 获取媒体简介和人物信息。") }
            state.errorMessage != null -> item { LibraryStatePanel(title = "详情加载失败", subtitle = state.errorMessage) }
            state.detail == null -> item { LibraryStatePanel(title = "暂无详情", subtitle = "Emby 没有返回该媒体的详情数据。") }
            else -> item {
                MediaDetailContent(
                    detail = state.detail,
                    onOpenSeason = onOpenSeason,
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
                    maxLines = 6,
                    overflow = TextOverflow.Ellipsis,
                )
                if (uiModel.people.isNotEmpty()) {
                    Text(
                        text = uiModel.people.joinToString("  /  "),
                        color = CinematicGlassColors.OnSurface,
                        fontSize = 15.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (uiModel.isMovie) {
                    PrimaryTvButton(
                        text = "播放",
                        icon = Icons.Filled.PlayArrow,
                        onClick = { onPlay(detail.item) },
                        modifier = Modifier.focusRequester(playFocusRequester),
                    )
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
            state.isSeasonLoading -> item { LibraryStatePanel(title = "正在加载剧集", subtitle = "正在从 Emby 获取该季的 Episode 列表。") }
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
private fun SectionHeader(title: String) {
    Text(
        text = title,
        color = CinematicGlassColors.OnSurface,
        fontSize = 28.sp,
        fontWeight = FontWeight.SemiBold,
    )
}

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
