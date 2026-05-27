package com.embytv.core.network

import com.embytv.domain.model.ServerProtocol
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MobileSetupPayloadTest {
    @Test
    fun parseFormRejectsMismatchedPairingToken() {
        val result = MobileSetupPayload.fromForm(
            body = "pair=bad&protocol=https&host=media.example.com&port=443&path=&username=alice&password=secret",
            expectedPair = "expected",
        )

        assertTrue(result.isFailure)
    }

    @Test
    fun parseFormRejectsExpiredPairingToken() {
        val result = MobileSetupPayload.fromForm(
            body = "pair=&protocol=https&host=media.example.com&port=443&path=&username=alice&password=secret",
            expectedPair = "",
        )

        assertTrue(result.isFailure)
    }

    @Test
    fun parseFormRejectsInvalidRequiredFields() {
        assertTrue(
            MobileSetupPayload.fromForm(
                body = "pair=ok&protocol=https&host=&port=443&path=&username=alice&password=",
                expectedPair = "ok",
            ).isFailure,
        )
        assertTrue(
            MobileSetupPayload.fromForm(
                body = "pair=ok&protocol=http&host=media.example.com&port=99999&path=&username=alice&password=",
                expectedPair = "ok",
            ).isFailure,
        )
    }

    @Test
    fun parseFormBuildsDraftForValidPayload() {
        val payload = MobileSetupPayload.fromForm(
            body = "pair=ok&protocol=http&host=media.example.com&port=8096&path=emby&username=alice&password=secret",
            expectedPair = "ok",
        ).getOrThrow()

        assertEquals(ServerProtocol.Http, payload.draft.protocol)
        assertEquals("media.example.com", payload.draft.host)
        assertEquals("8096", payload.draft.port)
        assertEquals("emby", payload.draft.path)
        assertEquals("alice", payload.draft.username)
        assertEquals("secret", payload.draft.password)
    }
}
