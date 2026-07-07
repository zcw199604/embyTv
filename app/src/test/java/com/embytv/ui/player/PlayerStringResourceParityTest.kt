package com.embytv.ui.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class PlayerStringResourceParityTest {
    @Test
    fun playerAndLanguageStringResourcesHaveEnglishTranslations() {
        val defaultStrings = stringResourceNames(File("src/main/res/values/strings.xml"))
            .filterRelevantPlayerKeys()
        val englishStrings = stringResourceNames(File("src/main/res/values-en/strings.xml"))
            .filterRelevantPlayerKeys()

        assertTrue(defaultStrings.isNotEmpty())
        assertEquals(defaultStrings, englishStrings)
    }

    private fun Set<String>.filterRelevantPlayerKeys(): Set<String> =
        filterTo(sortedSetOf()) { key ->
            key.startsWith("player_") || key.startsWith("settings_language")
        }

    private fun stringResourceNames(file: File): Set<String> {
        val document = DocumentBuilderFactory.newInstance()
            .newDocumentBuilder()
            .parse(file)
        val strings = document.getElementsByTagName("string")
        return buildSet {
            for (index in 0 until strings.length) {
                val item = strings.item(index)
                val name = item.attributes?.getNamedItem("name")?.nodeValue
                if (!name.isNullOrBlank()) {
                    add(name)
                }
            }
        }
    }
}
