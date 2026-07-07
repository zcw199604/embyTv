package com.embytv.ui.player

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class PlayerTechnicalLocaleContractTest {
    @Test
    fun playerTechnicalLabelsUseExplicitStableLocaleForCaseConversion() {
        val checkedFiles = listOf(
            File("src/main/java/com/embytv/domain/model/PlaybackSource.kt"),
            File("src/main/java/com/embytv/ui/player/PlayerMediaItemFactory.kt"),
            File("src/main/java/com/embytv/ui/player/PlayerPlaybackDetailsLabels.kt"),
            File("src/main/java/com/embytv/ui/player/PlayerTrackOptionMapper.kt"),
        )
        val bareCaseConversion = Regex("""\.(uppercase|lowercase)\(\)""")
        val offenders = checkedFiles
            .flatMap { file ->
                file.readLines().mapIndexedNotNull { index, line ->
                    if (bareCaseConversion.containsMatchIn(line)) {
                        "${file.name}:${index + 1}:$line"
                    } else {
                        null
                    }
                }
            }

        assertTrue(
            "Technical media labels and language tags must use Locale.US explicitly:\n" +
                offenders.joinToString(separator = "\n"),
            offenders.isEmpty(),
        )
    }
}
