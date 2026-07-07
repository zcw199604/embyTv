package com.embytv.ui.player

enum class PlayerRemoteKey {
    Back,
    Center,
    Enter,
    NumPadEnter,
    Up,
    Down,
    Left,
    Right,
    Other,
}

enum class PlayerRemoteKeyEventType {
    Down,
    Up,
    Other,
}

sealed interface PlayerRemoteKeyCommand {
    data object Ignore : PlayerRemoteKeyCommand
    data class Dispatch(val action: PlayerOsdAction) : PlayerRemoteKeyCommand
    data class SeekBy(val deltaMs: Long) : PlayerRemoteKeyCommand
}

object PlayerRemoteKeyPolicy {
    fun commandFor(
        eventType: PlayerRemoteKeyEventType,
        key: PlayerRemoteKey,
        osdVisible: Boolean,
    ): PlayerRemoteKeyCommand {
        if (eventType != PlayerRemoteKeyEventType.Up) return PlayerRemoteKeyCommand.Ignore
        if (key == PlayerRemoteKey.Back) {
            return PlayerRemoteKeyCommand.Dispatch(PlayerOsdAction.BackPressed)
        }
        if (osdVisible) return PlayerRemoteKeyCommand.Ignore
        return when (key) {
            PlayerRemoteKey.Left -> PlayerRemoteKeyCommand.SeekBy(deltaMs = -10_000L)
            PlayerRemoteKey.Right -> PlayerRemoteKeyCommand.SeekBy(deltaMs = 10_000L)
            PlayerRemoteKey.Center,
            PlayerRemoteKey.Enter,
            PlayerRemoteKey.NumPadEnter,
            PlayerRemoteKey.Up,
            PlayerRemoteKey.Down,
            -> PlayerRemoteKeyCommand.Dispatch(PlayerOsdAction.UserInteraction)
            PlayerRemoteKey.Back,
            PlayerRemoteKey.Other,
            -> PlayerRemoteKeyCommand.Ignore
        }
    }
}
