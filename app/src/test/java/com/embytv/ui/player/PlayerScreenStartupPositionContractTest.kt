package com.embytv.ui.player

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class PlayerScreenStartupPositionContractTest {
    @Test
    fun playerScreenStartsMediaDanmakuAndReportingFromPlaybackSourceResumePosition() {
        val source = File("src/main/java/com/embytv/ui/player/PlayerScreen.kt").readText()

        assertTrue(source.contains("PlayerStartupPositionPolicy.normalize(playbackSource.startPositionMs)"))
        assertTrue(source.contains("player.seekTo(startPositionMs)"))
        assertTrue(source.contains("reportingCoordinator.onStarted(positionMs = startPositionMs)"))
        assertTrue(source.contains("DanmakuPlaybackPolicy.commandForSeek(startPositionMs)"))
        assertFalse(source.contains("reportingCoordinator.onStarted(positionMs = 0L)"))
        assertFalse(source.contains("DanmakuPlaybackPolicy.commandForSeek(0)"))
    }

    @Test
    fun playerManagerResetsForAnyPlaybackSourceChange() {
        val source = File("src/main/java/com/embytv/ui/player/PlayerScreen.kt").readText()

        assertTrue(
            Regex("""val playerManager = remember\(\s*playbackSource,\s*startPositionMs,?\s*\)""")
                .containsMatchIn(source),
        )
    }
}
