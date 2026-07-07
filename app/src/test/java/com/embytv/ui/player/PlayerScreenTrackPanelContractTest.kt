package com.embytv.ui.player

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class PlayerScreenTrackPanelContractTest {
    @Test
    fun trackQuickPanelDoesNotHardCapAudioOrSubtitleOptions() {
        val source = File("src/main/java/com/embytv/ui/player/PlayerScreen.kt").readText()

        assertFalse(source.contains("tracks.take(6)"))
    }

    @Test
    fun primaryPlayButtonUsesDedicatedFocusRequester() {
        val source = playerScreenSource()

        assertTrue(source.contains("val playFocusRequester = remember { FocusRequester() }"))
        assertTrue(source.contains("focusRequester = playFocusRequester"))
    }

    @Test
    fun quickPanelFirstOptionUsesDedicatedFocusRequester() {
        val source = playerScreenSource()

        assertTrue(source.contains("val quickPanelFocusRequester = remember { FocusRequester() }"))
        assertTrue(source.contains("firstOptionFocusRequester = quickPanelFocusRequester"))
        assertTrue(source.contains("focusRequester = if (rowIndex == 0 && actionIndex == 0)"))
        assertTrue(source.contains("firstOptionFocusRequester"))
    }

    @Test
    fun primaryFocusEffectObservesQuickPanelChangesForRestore() {
        val source = playerScreenSource()

        assertTrue(source.contains("LaunchedEffect(playbackItemId, state.visible, state.interactionRevision, state.selectedQuickPanel)"))
    }

    @Test
    fun detailOverlayUsesResponsiveMaxWidthInsteadOfFixedWidth() {
        val source = playerScreenSource()

        assertFalse(source.contains("modifier.width(420.dp)"))
        assertTrue(source.contains("modifier.fillMaxWidth().widthIn(max = 420.dp)"))
    }

    @Test
    fun previousAndNextButtonsKeepDisabledReasonsWiredToFocusableSurface() {
        val source = playerScreenSource()

        assertTrue(source.contains("disabledReason = previousNavigation.disabledReason"))
        assertTrue(source.contains("disabledReason = nextNavigation.disabledReason"))
        assertTrue(source.contains("disabledReason = disabledReason"))
        assertTrue(source.contains("onDisabledClick = onUnsupported"))
    }

    @Test
    fun quickSettingLabelsUseStableUppercaseLocale() {
        val source = playerScreenSource()

        assertFalse(source.contains("label.uppercase()"))
        assertTrue(source.contains("label.uppercase(Locale.US)"))
    }

    @Test
    fun trackSelectionUsesMedia3SelectionHelpersAndUpdatesOsdState() {
        val source = playerScreenSource()

        assertTrue(source.contains("player.trackSelectionParameters = player.trackSelectionParameters.selectTrack(option)"))
        assertTrue(source.contains("dispatch(PlayerOsdAction.SelectTrack(option))"))
    }

    @Test
    fun disablingSubtitlesUsesMedia3DisableHelperAndUpdatesOsdState() {
        val source = playerScreenSource()

        assertTrue(source.contains("player.trackSelectionParameters = player.trackSelectionParameters.disableSubtitles()"))
        assertTrue(source.contains("dispatch(PlayerOsdAction.DisableSubtitles(subtitlesDisabledFeedback))"))
    }

    private fun playerScreenSource(): String =
        File("src/main/java/com/embytv/ui/player/PlayerScreen.kt").readText()
}
