package com.embytv.ui.player

enum class OsdFocusTone {
    Primary,
    OnPrimary,
    OnSurface,
    OnSurfaceVariant,
    Disabled,
}

data class OsdFocusVisualState(
    val focused: Boolean = false,
    val selected: Boolean = false,
    val primary: Boolean = false,
    val enabled: Boolean = true,
)

data class OsdFocusVisuals(
    val foregroundTone: OsdFocusTone,
    val containerHighlighted: Boolean,
    val selectedIndicator: Boolean,
    val emphasizedLabel: Boolean,
    val contentAlpha: Float,
)

object OsdFocusVisualResolver {
    fun resolve(state: OsdFocusVisualState): OsdFocusVisuals {
        val enabled = state.enabled
        return OsdFocusVisuals(
            foregroundTone = when {
                !enabled -> OsdFocusTone.Disabled
                state.primary -> OsdFocusTone.OnPrimary
                state.selected -> OsdFocusTone.Primary
                state.focused -> OsdFocusTone.OnSurface
                else -> OsdFocusTone.OnSurfaceVariant
            },
            containerHighlighted = state.focused,
            selectedIndicator = state.primary || state.selected,
            emphasizedLabel = state.focused || state.selected || state.primary,
            contentAlpha = if (enabled) 1f else 0.56f,
        )
    }
}
