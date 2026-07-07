package com.embytv.ui.player

data class PlayerQuickPanelLayoutSpec(
    val maxItemsPerRow: Int,
) {
    fun <T> rowsFor(items: List<T>): List<List<T>> =
        items.chunked(maxItemsPerRow)
}

object PlayerQuickPanelLayoutPolicy {
    val TvDefault = PlayerQuickPanelLayoutSpec(
        maxItemsPerRow = 3,
    )
}
