package com.embytv.data.local

// 覆盖播放历史纯规则，保证最近播放记录去重、置顶和数量上限稳定。
import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackHistoryRulesTest {
    @Test
    fun addMovesDuplicateMediaToFront() {
        val history = listOf(
            PlaybackHistoryItem(mediaId = "m1", mediaTitle = "Movie 1", positionMs = 1_000, durationMs = 4_000, timestamp = 1L),
            PlaybackHistoryItem(mediaId = "m2", mediaTitle = "Movie 2", positionMs = 2_000, durationMs = 5_000, timestamp = 2L),
        )

        val updated = PlaybackHistoryRules.add(
            history = history,
            item = PlaybackHistoryItem(
                mediaId = "m1",
                mediaTitle = "Movie 1 updated",
                positionMs = 3_000,
                durationMs = 6_000,
                timestamp = 3L,
            ),
        )

        assertEquals(2, updated.size)
        assertEquals("m1", updated.first().mediaId)
        assertEquals(3_000, updated.first().positionMs)
        assertEquals("m2", updated.last().mediaId)
    }

    @Test
    fun addLimitsPlaybackHistorySize() {
        val history = (0 until 55).map { index ->
            PlaybackHistoryItem(
                mediaId = "m$index",
                mediaTitle = "Movie $index",
                positionMs = index.toLong(),
                durationMs = 100L,
                timestamp = index.toLong(),
            )
        }

        val updated = PlaybackHistoryRules.add(
            history,
            PlaybackHistoryItem("latest", "Latest", 10L, 100L, 100L),
        )

        assertEquals(PlaybackHistoryStore.MAX_HISTORY_SIZE, updated.size)
        assertEquals("latest", updated.first().mediaId)
    }
}
