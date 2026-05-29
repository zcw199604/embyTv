package com.embytv.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlaybackQueueTest {
    @Test
    fun buildsPreviousAndNextAroundCurrentEpisode() {
        val episodes = listOf(item("e1"), item("e2"), item("e3"))

        val queue = PlaybackQueue.from(episodes, "e2")

        requireNotNull(queue)
        assertEquals("e1", queue.previous?.id)
        assertEquals("e2", queue.current.id)
        assertEquals("e3", queue.next?.id)
    }

    @Test
    fun keepsQueueEdgesNullAtBoundaries() {
        val episodes = listOf(item("e1"), item("e2"))

        val first = PlaybackQueue.from(episodes, "e1")
        val missing = PlaybackQueue.from(episodes, "missing")

        requireNotNull(first)
        assertNull(first.previous)
        assertEquals("e2", first.next?.id)
        assertNull(missing)
    }

    private fun item(id: String): MediaItemSummary =
        MediaItemSummary(
            id = id,
            name = id,
            type = "Episode",
            overview = null,
            imageUrl = null,
        )
}
