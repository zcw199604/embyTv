package com.embytv.ui.utils

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween

object EmbyAnimationSpecs {
    val StandardFloat = tween<Float>(
        durationMillis = 300,
        easing = FastOutSlowInEasing,
    )

    val FastFloat = tween<Float>(
        durationMillis = 200,
        easing = FastOutSlowInEasing,
    )

    val SlowFloat = tween<Float>(
        durationMillis = 400,
        easing = FastOutSlowInEasing,
    )

    val FocusDp = tween<androidx.compose.ui.unit.Dp>(
        durationMillis = 200,
        easing = FastOutSlowInEasing,
    )

    val BounceFloat = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow,
    )
}
