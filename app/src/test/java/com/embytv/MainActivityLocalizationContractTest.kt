package com.embytv

// 保护 Compose 本地化上下文在系统配置变化时同步刷新，避免固定语言下资源配置陈旧。
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class MainActivityLocalizationContractTest {
    @Test
    fun localizedAppRebuildsLocalizedContextWhenConfigurationChanges() {
        val source = File("src/main/java/com/embytv/MainActivity.kt").readText()

        assertTrue(source.contains("remember(baseContext, baseConfiguration, language)"))
        assertTrue(source.contains("baseContext.localized(language, baseConfiguration)"))
    }
}
