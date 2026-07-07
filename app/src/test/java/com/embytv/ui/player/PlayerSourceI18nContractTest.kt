package com.embytv.ui.player

// 保护播放器生产 UI 文案继续通过 Android string resource 注入，避免英文模式混入硬编码中文。
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class PlayerSourceI18nContractTest {
    @Test
    fun playerProductionSourcesDoNotContainHardcodedChineseText() {
        val playerSourceRoot = File("src/main/java/com/embytv/ui/player")
        val offenders = playerSourceRoot
            .walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .mapNotNull { file ->
                val lines = file.readLines()
                    .mapIndexedNotNull { index, line ->
                        if (line.contains(Regex("\\p{IsHan}"))) "${file.name}:${index + 1}:$line" else null
                    }
                lines.takeIf { it.isNotEmpty() }
            }
            .flatten()
            .toList()

        assertTrue(offenders.joinToString(separator = "\n"), offenders.isEmpty())
    }
}
