package com.embytv.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ServerConfigDraftTest {
    @Test
    fun protocolDefaultsUseEmbyPorts() {
        assertEquals(443, ServerProtocol.Https.defaultPort)
        assertEquals(8096, ServerProtocol.Http.defaultPort)

        val initial = ServerConfigDraft()
        assertEquals(ServerProtocol.Https, initial.protocol)
        assertEquals("443", initial.port)

        val switchedToHttp = initial.withProtocol(ServerProtocol.Http)
        assertEquals("8096", switchedToHttp.port)

        val switchedBackToHttps = switchedToHttp.withProtocol(ServerProtocol.Https)
        assertEquals("443", switchedBackToHttps.port)
    }

    @Test
    fun protocolSwitchKeepsCustomPort() {
        val draft = ServerConfigDraft(port = "8920")

        val switched = draft.withProtocol(ServerProtocol.Http)

        assertEquals(ServerProtocol.Http, switched.protocol)
        assertEquals("8920", switched.port)
    }

    @Test
    fun toServerConfigNormalizesPathAndKeepsCredentials() {
        val config = ServerConfigDraft(
            protocol = ServerProtocol.Https,
            host = " media.example.com ",
            port = "443",
            path = "/emby/",
            username = "alice",
            password = "secret",
        ).toServerConfig(deviceId = "device-1")

        assertEquals("https://media.example.com:443/emby/", config.baseUrl)
        assertEquals("alice", config.username)
        assertEquals("secret", config.password)
        assertEquals("device-1", config.deviceId)
    }

    @Test
    fun toServerConfigSupportsEmptyPath() {
        val config = ServerConfigDraft(
            protocol = ServerProtocol.Http,
            host = "192.168.1.10",
            port = "8096",
            username = "alice",
        ).toServerConfig(deviceId = "device-1")

        assertEquals("http://192.168.1.10:8096/", config.baseUrl)
    }

    @Test
    fun validateRejectsBlankHostUsernameAndInvalidPort() {
        assertTrue(ServerConfigDraft(host = "", username = "alice").validate().isFailure)
        assertTrue(ServerConfigDraft(host = "server.local", username = "").validate().isFailure)
        assertTrue(ServerConfigDraft(host = "server.local", port = "0", username = "alice").validate().isFailure)
        assertTrue(ServerConfigDraft(host = "server.local", port = "65536", username = "alice").validate().isFailure)
        assertTrue(ServerConfigDraft(host = "server.local", port = "abc", username = "alice").validate().isFailure)
        assertFalse(ServerConfigDraft(host = "server.local", port = "443", username = "alice").validate().isFailure)
    }
}
