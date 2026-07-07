package com.embytv.ui.player

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerScreenDanmakuSyncContractTest {
    @Test
    fun danmakuOverlayOwnsContinuousPlaybackConfigSynchronization() {
        val screenSource = File("src/main/java/com/embytv/ui/player/PlayerScreen.kt").readText()
        val overlaySource = File("src/main/java/com/embytv/ui/player/DanmakuOverlay.kt").readText()

        assertFalse(
            screenSource.contains(
                """
                LaunchedEffect(
                        osdState.danmakuEnabled,
                        osdState.danmakuPaused,
                        osdState.danmakuSettings.playbackConfigKey(),
                    )
                """.trimIndent(),
            ),
        )
        assertTrue(overlaySource.contains("LaunchedEffect(playbackConfigKey, enabled, paused)"))
        assertTrue(overlaySource.contains("DanmakuPlaybackPolicy.commandForPlayback"))
    }

    @Test
    fun danmakuSeekSyncStopsOldFrameSeeksThenRestoresPlaybackCommand() {
        val source = playerScreenSource()
        val stopIndex = source.indexOf("danmakuPlayer.stop()")
        val seekIndex = source.indexOf("danmakuPlayer.seekTo(command.positionMs)")
        val restoreIndex = source.indexOf("applyDanmakuPlaybackCommand(", startIndex = seekIndex)

        assertTrue(stopIndex >= 0)
        assertTrue(seekIndex > stopIndex)
        assertTrue(restoreIndex > seekIndex)
        assertTrue(source.contains("DanmakuPlaybackPolicy.commandForSeek(positionMs)"))
        assertTrue(source.contains("DanmakuPlaybackPolicy.commandForPlayback("))
    }

    @Test
    fun media3PositionDiscontinuityEffectSynchronizesDanmakuThroughSharedPath() {
        val source = playerScreenSource()

        assertTrue(source.contains("is PlayerPlaybackEffect.SyncDanmaku -> {"))
        assertTrue(source.contains("syncDanmakuTo(effect.positionMs)"))
    }

    private fun playerScreenSource(): String =
        File("src/main/java/com/embytv/ui/player/PlayerScreen.kt").readText()
}
