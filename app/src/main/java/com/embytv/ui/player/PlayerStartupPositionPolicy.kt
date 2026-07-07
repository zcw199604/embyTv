package com.embytv.ui.player

object PlayerStartupPositionPolicy {
    fun normalize(positionMs: Long): Long = positionMs.coerceAtLeast(0L)
}
