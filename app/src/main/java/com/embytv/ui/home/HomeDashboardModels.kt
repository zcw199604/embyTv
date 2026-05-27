package com.embytv.ui.home

import com.embytv.domain.model.EmbyHomeDashboard
import com.embytv.domain.model.MediaItemSummary

data class HomeNavigationItem(
    val id: String,
    val title: String,
    val enabled: Boolean,
    val disabledReason: String? = null,
)

data class LibrarySummaryUiModel(
    val id: String,
    val title: String,
    val countLabel: String,
    val imageUrl: String?,
    val enabled: Boolean = true,
    val disabledReason: String? = null,
)

data class MediaCardUiModel(
    val id: String,
    val title: String,
    val subtitle: String,
    val imageUrl: String?,
    val progressFraction: Float,
    val badge: String,
)

data class HomeDashboardUiModel(
    val navigationItems: List<HomeNavigationItem>,
    val libraries: List<LibrarySummaryUiModel>,
    val continueWatching: List<MediaCardUiModel>,
    val mediaSectionTitle: String,
)

data class DrawerUiState(
    val isOpen: Boolean = false,
    val restoreMenuFocus: Boolean = false,
) {
    fun open(): DrawerUiState = copy(isOpen = true, restoreMenuFocus = false)

    fun close(): DrawerUiState = copy(isOpen = false, restoreMenuFocus = true)

    fun onBack(): DrawerUiState = if (isOpen) close() else this

    fun menuFocusRestored(): DrawerUiState = copy(restoreMenuFocus = false)
}

object HomeDashboardMapper {
    fun map(dashboard: EmbyHomeDashboard): HomeDashboardUiModel {
        val mediaItems = if (dashboard.resumeItems.isNotEmpty()) {
            dashboard.resumeItems
        } else {
            dashboard.latestItems
        }
        return HomeDashboardUiModel(
            navigationItems = dashboard.libraries.map { library ->
                HomeNavigationItem(
                    id = library.id,
                    title = library.name.ifBlank { library.collectionType ?: "媒体库" },
                    enabled = false,
                    disabledReason = "媒体库详情暂未支持",
                )
            },
            libraries = dashboard.libraries.map { library ->
                LibrarySummaryUiModel(
                    id = library.id,
                    title = library.name.ifBlank { library.collectionType ?: library.type },
                    countLabel = library.itemCount.toItemCountLabel(),
                    imageUrl = library.imageUrl,
                    enabled = true,
                    disabledReason = "媒体库详情暂未支持",
                )
            },
            continueWatching = mediaItems.take(12).map { item ->
                MediaCardUiModel(
                    id = item.id,
                    title = item.name.ifBlank { item.id },
                    subtitle = item.subtitle(),
                    imageUrl = item.imageUrl,
                    progressFraction = item.progressFraction(),
                    badge = item.type.ifBlank { "Media" },
                )
            },
            mediaSectionTitle = if (dashboard.resumeItems.isNotEmpty()) "Continue Watching" else "最近入库",
        )
    }

    private fun Int.toItemCountLabel(): String = if (this == 1) "1 item" else "$this items"

    private fun MediaItemSummary.subtitle(): String =
        seriesName?.takeIf { it.isNotBlank() }
            ?: overview?.takeIf { it.isNotBlank() }
            ?: productionYear?.toString()
            ?: type

    private fun MediaItemSummary.progressFraction(): Float {
        playedPercentage?.let { return (it / 100.0).toFloat().coerceIn(0f, 1f) }
        val runtime = runTimeTicks ?: return 0f
        if (runtime <= 0L) return 0f
        return (playbackPositionTicks.toFloat() / runtime.toFloat()).coerceIn(0f, 1f)
    }
}
