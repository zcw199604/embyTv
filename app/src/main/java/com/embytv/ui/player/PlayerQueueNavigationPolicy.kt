package com.embytv.ui.player

import com.embytv.domain.model.MediaItemSummary
import com.embytv.domain.model.PlaybackQueue

data class PlayerQueueNavigationItemState(
    val enabled: Boolean,
    val disabledReason: String?,
    val target: MediaItemSummary?,
)

data class PlayerQueueNavigationState(
    val previous: PlayerQueueNavigationItemState,
    val next: PlayerQueueNavigationItemState,
    val autoPlayNextTarget: MediaItemSummary?,
)

object PlayerQueueNavigationPolicy {
    fun resolve(
        queue: PlaybackQueue?,
        noPreviousReason: String,
        noNextReason: String,
    ): PlayerQueueNavigationState {
        val currentId = queue?.current?.id
        val previous = queue?.previous?.takeUnless { it.id == currentId }
        val next = queue?.next?.takeUnless { it.id == currentId }
        return PlayerQueueNavigationState(
            previous = PlayerQueueNavigationItemState(
                enabled = previous != null,
                disabledReason = if (previous != null) null else noPreviousReason,
                target = previous,
            ),
            next = PlayerQueueNavigationItemState(
                enabled = next != null,
                disabledReason = if (next != null) null else noNextReason,
                target = next,
            ),
            autoPlayNextTarget = next.takeIf { queue?.autoPlayNext == true },
        )
    }
}
