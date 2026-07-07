package com.embytv.ui.player

data class PlayerOsdMotionSpec(
    val enterDurationMs: Int,
    val exitDurationMs: Int,
    val slideDistanceFraction: Float,
) {
    fun enterOffsetPx(fullHeight: Int): Int =
        (fullHeight * slideDistanceFraction).toInt().coerceAtLeast(0)

    fun exitOffsetPx(fullHeight: Int): Int =
        (fullHeight * slideDistanceFraction).toInt().coerceAtLeast(0)
}

object PlayerOsdMotionPolicy {
    val TvDefault = PlayerOsdMotionSpec(
        enterDurationMs = 160,
        exitDurationMs = 120,
        slideDistanceFraction = 0.025f,
    )
}
