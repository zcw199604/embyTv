package com.embytv.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SeekThumbnailTimelineTest {
    @Test
    fun previewThumbnailForUsesNearestChapterAtOrBeforeTarget() {
        val source = PlaybackSource(
            itemId = "movie-1",
            title = "Movie",
            streamUrl = "http://emby.test/Videos/movie-1/stream",
            previewThumbnailUrl = "fallback.jpg",
            seekThumbnails = listOf(
                SeekThumbnail(positionMs = 0L, imageUrl = "chapter-0.jpg"),
                SeekThumbnail(positionMs = 60_000L, imageUrl = "chapter-1.jpg"),
                SeekThumbnail(positionMs = 120_000L, imageUrl = "chapter-2.jpg"),
            ),
        )

        assertEquals("chapter-0.jpg", source.previewThumbnailFor(15_000L))
        assertEquals("chapter-1.jpg", source.previewThumbnailFor(75_000L))
        assertEquals("chapter-2.jpg", source.previewThumbnailFor(180_000L))
    }

    @Test
    fun previewThumbnailForFallsBackWhenApiProvidesNoChapterImages() {
        val source = PlaybackSource(
            itemId = "movie-1",
            title = "Movie",
            streamUrl = "http://emby.test/Videos/movie-1/stream",
            previewThumbnailUrl = "fallback.jpg",
        )

        assertEquals("fallback.jpg", source.previewThumbnailFor(75_000L))
    }

    @Test
    fun previewThumbnailForIgnoresBlankChapterAndFallbackUrls() {
        val source = PlaybackSource(
            itemId = "movie-1",
            title = "Movie",
            streamUrl = "http://emby.test/Videos/movie-1/stream",
            previewThumbnailUrl = " ",
            seekThumbnails = listOf(
                SeekThumbnail(positionMs = 0L, imageUrl = ""),
                SeekThumbnail(positionMs = 60_000L, imageUrl = " "),
            ),
        )

        assertNull(source.previewThumbnailFor(75_000L))
    }

    @Test
    fun previewThumbnailForTrimsChapterAndFallbackUrls() {
        val source = PlaybackSource(
            itemId = "movie-1",
            title = "Movie",
            streamUrl = "http://emby.test/Videos/movie-1/stream",
            previewThumbnailUrl = "  fallback.jpg  ",
            seekThumbnails = listOf(
                SeekThumbnail(positionMs = 60_000L, imageUrl = "  chapter-1.jpg  "),
            ),
        )

        assertEquals("chapter-1.jpg", source.previewThumbnailFor(75_000L))
        assertEquals("fallback.jpg", source.copy(seekThumbnails = emptyList()).previewThumbnailFor(75_000L))
    }
}
