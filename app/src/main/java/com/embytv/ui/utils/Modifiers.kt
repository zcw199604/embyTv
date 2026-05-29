package com.embytv.ui.utils

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale

fun Modifier.focusScale(focused: Boolean): Modifier = composed {
    val scale by animateFloatAsState(
        targetValue = if (focused) 1.035f else 1f,
        animationSpec = EmbyAnimationSpecs.FastFloat,
        label = "focus-scale",
    )
    this.scale(scale)
}

fun Modifier.shimmerEffect(): Modifier = this
