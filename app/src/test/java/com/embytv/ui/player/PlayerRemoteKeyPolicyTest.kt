package com.embytv.ui.player

import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerRemoteKeyPolicyTest {
    @Test
    fun keyDownEventsAreIgnoredToAvoidDuplicateRemoteActions() {
        val command = PlayerRemoteKeyPolicy.commandFor(
            eventType = PlayerRemoteKeyEventType.Down,
            key = PlayerRemoteKey.Center,
            osdVisible = false,
        )

        assertEquals(PlayerRemoteKeyCommand.Ignore, command)
    }

    @Test
    fun backKeyIsAlwaysRoutedToOsdReducer() {
        val hidden = PlayerRemoteKeyPolicy.commandFor(
            eventType = PlayerRemoteKeyEventType.Up,
            key = PlayerRemoteKey.Back,
            osdVisible = false,
        )
        val visible = PlayerRemoteKeyPolicy.commandFor(
            eventType = PlayerRemoteKeyEventType.Up,
            key = PlayerRemoteKey.Back,
            osdVisible = true,
        )

        assertEquals(PlayerRemoteKeyCommand.Dispatch(PlayerOsdAction.BackPressed), hidden)
        assertEquals(PlayerRemoteKeyCommand.Dispatch(PlayerOsdAction.BackPressed), visible)
    }

    @Test
    fun hiddenOsdLeftAndRightSeekWithoutFirstShowingMenu() {
        val left = PlayerRemoteKeyPolicy.commandFor(
            eventType = PlayerRemoteKeyEventType.Up,
            key = PlayerRemoteKey.Left,
            osdVisible = false,
        )
        val right = PlayerRemoteKeyPolicy.commandFor(
            eventType = PlayerRemoteKeyEventType.Up,
            key = PlayerRemoteKey.Right,
            osdVisible = false,
        )

        assertEquals(PlayerRemoteKeyCommand.SeekBy(deltaMs = -10_000L), left)
        assertEquals(PlayerRemoteKeyCommand.SeekBy(deltaMs = 10_000L), right)
    }

    @Test
    fun hiddenOsdCenterEnterUpAndDownRevealControls() {
        val keys = listOf(
            PlayerRemoteKey.Center,
            PlayerRemoteKey.Enter,
            PlayerRemoteKey.NumPadEnter,
            PlayerRemoteKey.Up,
            PlayerRemoteKey.Down,
        )

        keys.forEach { key ->
            assertEquals(
                PlayerRemoteKeyCommand.Dispatch(PlayerOsdAction.UserInteraction),
                PlayerRemoteKeyPolicy.commandFor(
                    eventType = PlayerRemoteKeyEventType.Up,
                    key = key,
                    osdVisible = false,
                ),
            )
        }
    }

    @Test
    fun visibleOsdLeavesDirectionalAndEnterKeysForFocusedControls() {
        val keys = listOf(
            PlayerRemoteKey.Center,
            PlayerRemoteKey.Enter,
            PlayerRemoteKey.NumPadEnter,
            PlayerRemoteKey.Up,
            PlayerRemoteKey.Down,
            PlayerRemoteKey.Left,
            PlayerRemoteKey.Right,
        )

        keys.forEach { key ->
            assertEquals(
                PlayerRemoteKeyCommand.Ignore,
                PlayerRemoteKeyPolicy.commandFor(
                    eventType = PlayerRemoteKeyEventType.Up,
                    key = key,
                    osdVisible = true,
                ),
            )
        }
    }
}
