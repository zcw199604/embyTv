package com.embytv.ui.player

enum class DanmakuQuickOption {
    Enabled,
    Disabled,
    Opacity60,
    Opacity100,
    TextSmall,
    TextNormal,
    TextLarge,
    AreaTop,
    AreaFull,
}

data class DanmakuQuickPanelLayout(
    val rows: List<List<DanmakuQuickOption>>,
)

object DanmakuQuickPanelLayoutPolicy {
    val TvDefault = DanmakuQuickPanelLayout(
        rows = listOf(
            listOf(DanmakuQuickOption.Enabled, DanmakuQuickOption.Disabled),
            listOf(DanmakuQuickOption.Opacity60, DanmakuQuickOption.Opacity100),
            listOf(
                DanmakuQuickOption.TextSmall,
                DanmakuQuickOption.TextNormal,
                DanmakuQuickOption.TextLarge,
            ),
            listOf(DanmakuQuickOption.AreaTop, DanmakuQuickOption.AreaFull),
        ),
    )
}
