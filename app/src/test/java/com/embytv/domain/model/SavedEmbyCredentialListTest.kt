package com.embytv.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class SavedEmbyCredentialListTest {
    @Test
    fun upsertReplacesSameServerAndUserAndSortsNewestFirst() {
        val older = credential(
            serverUrl = "http://emby.test/",
            userId = "user-1",
            username = "旧用户",
            savedAt = 1L,
        )
        val newerSameIdentity = older.copy(username = "新用户", savedAtEpochMillis = 3L)
        val other = credential(
            serverUrl = "http://other.test/",
            userId = "user-2",
            username = "其他用户",
            savedAt = 2L,
        )

        val list = SavedEmbyCredentialList()
            .upsert(older)
            .upsert(other)
            .upsert(newerSameIdentity)

        assertEquals(listOf("新用户", "其他用户"), list.credentials.map { it.username })
        assertEquals("新用户 · http://emby.test", list.credentials.first().displayLabel)
    }

    @Test
    fun removeUsesStableUniqueKey() {
        val first = credential("http://emby.test/", "user-1", "用户1", 1L)
        val second = credential("http://emby.test/path", "user-2", "用户2", 2L)

        val list = SavedEmbyCredentialList(listOf(first, second)).remove(first.uniqueKey)

        assertEquals(listOf("用户2"), list.credentials.map { it.username })
    }

    private fun credential(
        serverUrl: String,
        userId: String,
        username: String,
        savedAt: Long,
    ): SavedEmbyCredential =
        SavedEmbyCredential(
            serverUrl = serverUrl,
            userId = userId,
            username = username,
            accessToken = "token-$userId",
            serverId = "server",
            deviceId = "device-$userId",
            savedAtEpochMillis = savedAt,
        )
}
