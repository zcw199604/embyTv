package com.embytv.core.di

import org.junit.Assert.assertFalse
import org.junit.Test

class AppContainerContractTest {
    @Test
    fun appContainerDoesNotExposeSamplePlaybackSource() {
        val methods = AppContainer::class.java.methods.map { it.name }

        assertFalse(methods.contains("samplePlaybackSource"))
    }
}
