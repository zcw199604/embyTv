package com.embytv.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class SavedEmbyCredentialTest {
    @Test
    fun savedCredentialKeepsUsernameForDisplayButHasNoPasswordField() {
        val credential = SavedEmbyCredential(
            serverUrl = "https://media.example.com:443/",
            userId = "user-1",
            username = "alice",
            accessToken = "token-1",
            serverId = "server-1",
            deviceId = "device-1",
            savedAtEpochMillis = 123L,
        )

        assertEquals("alice", credential.username)
        assertEquals("token-1", credential.accessToken)
        assertFalse(
            SavedEmbyCredential::class.java.declaredFields.any { field ->
                field.name.equals("password", ignoreCase = true)
            },
        )
    }
}
