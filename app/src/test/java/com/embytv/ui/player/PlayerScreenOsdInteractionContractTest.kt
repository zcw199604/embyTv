package com.embytv.ui.player

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerScreenOsdInteractionContractTest {
    @Test
    fun autoHideEffectObservesQuickPanelStateAndUsesPolicyDelay() {
        val source = playerScreenSource()

        assertTrue(
            source.contains(
                "LaunchedEffect(osdState.visible, osdState.interactionRevision, osdState.status, osdState.selectedQuickPanel)",
            ),
        )
        assertTrue(source.contains("PlayerOsdAutoHidePolicy.shouldScheduleAutoHide("))
        assertTrue(source.contains("osdState.toAutoHideSnapshot()"))
        assertTrue(source.contains("delay(PlayerOsdAutoHidePolicy.AUTO_HIDE_DELAY_MS)"))
        assertTrue(source.contains("dispatch(PlayerOsdAction.Hide)"))
    }

    @Test
    fun previewRemoteKeyLayerOnlyConsumesSeekAndBackBeforeFocusedControls() {
        val source = playerScreenSource()
        val previewHandlerIndex = source.indexOf("fun executePreviewRemoteKeyCommand")
        val seekBranchIndex = source.indexOf("is PlayerRemoteKeyCommand.SeekBy -> executeRemoteKeyCommand(command)", previewHandlerIndex)
        val backBranchIndex = source.indexOf(
            "PlayerRemoteKeyCommand.Dispatch(PlayerOsdAction.BackPressed) -> executeRemoteKeyCommand(command)",
            previewHandlerIndex,
        )
        val fallbackIndex = source.indexOf("else -> false", previewHandlerIndex)

        assertTrue(previewHandlerIndex >= 0)
        assertTrue(seekBranchIndex > previewHandlerIndex)
        assertTrue(backBranchIndex > seekBranchIndex)
        assertTrue(fallbackIndex > backBranchIndex)
    }

    @Test
    fun regularRemoteKeyLayerUsesCurrentOsdVisibilityPolicy() {
        val source = playerScreenSource()

        assertTrue(source.contains(".onPreviewKeyEvent { event ->"))
        assertTrue(source.contains(".onKeyEvent { event ->"))
        assertTrue(source.contains("PlayerRemoteKeyPolicy.commandFor("))
        assertTrue(source.contains("osdVisible = osdState.visible"))
    }

    private fun playerScreenSource(): String =
        File("src/main/java/com/embytv/ui/player/PlayerScreen.kt").readText()
}
