package com.embytv.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EmbyStreamUrlBuilderTest {
    private val builder = EmbyStreamUrlBuilder()

    @Test
    fun buildVideoStreamUrl_trimsTrailingSlashAndAddsToken() {
        val url = builder.buildVideoStreamUrl(
            serverUrl = "http://127.0.0.1:8096/",
            itemId = "abc 123",
            accessToken = "token-value",
        )

        assertEquals(
            "http://127.0.0.1:8096/Videos/abc+123/stream?Static=true&api_key=token-value",
            url,
        )
    }

    @Test
    fun buildPrimaryImageUrl_returnsNullWithoutTag() {
        assertTrue(
            builder.buildPrimaryImageUrl(
                serverUrl = "http://127.0.0.1:8096",
                itemId = "item",
                tag = null,
            ) == null,
        )
    }

    @Test
    fun buildImageUrls_canUseUntaggedFallback() {
        assertEquals(
            "http://127.0.0.1:8096/Items/item/Images/Primary",
            builder.buildPrimaryImageUrl(
                serverUrl = "http://127.0.0.1:8096",
                itemId = "item",
                tag = null,
                allowUntagged = true,
            ),
        )
        assertEquals(
            "http://127.0.0.1:8096/Items/item/Images/Backdrop/0",
            builder.buildBackdropImageUrl(
                serverUrl = "http://127.0.0.1:8096",
                itemId = "item",
                tag = null,
                allowUntagged = true,
            ),
        )
    }

    @Test
    fun buildImageUrls_addsImageProfileSizing() {
        assertEquals(
            "http://127.0.0.1:8096/Items/item/Images/Primary?tag=tag-value&MaxWidth=500&MaxHeight=750&Quality=85",
            builder.buildPrimaryImageUrl(
                serverUrl = "http://127.0.0.1:8096",
                itemId = "item",
                tag = "tag-value",
                profile = EmbyImageProfile.Poster,
            ),
        )
        assertEquals(
            "http://127.0.0.1:8096/Items/item/Images/Backdrop/0?MaxWidth=960&MaxHeight=540&Quality=85",
            builder.buildBackdropImageUrl(
                serverUrl = "http://127.0.0.1:8096",
                itemId = "item",
                tag = null,
                allowUntagged = true,
                profile = EmbyImageProfile.Backdrop,
            ),
        )
    }

    @Test
    fun buildChapterImageUrl_usesChapterIndexAndTag() {
        assertEquals(
            "http://127.0.0.1:8096/Items/movie+1/Images/Chapter/3?tag=chapter-tag&MaxWidth=640&MaxHeight=360&Quality=85",
            builder.buildChapterImageUrl(
                serverUrl = "http://127.0.0.1:8096/",
                itemId = "movie 1",
                chapterIndex = 3,
                tag = "chapter-tag",
                profile = EmbyImageProfile.Thumb,
            ),
        )
    }

    @Test
    fun buildSubtitleDeliveryUrl_normalizesRelativeUrlAndAddsToken() {
        assertEquals(
            "http://127.0.0.1:8096/Videos/movie+1/media-source/Subtitles/2/Stream.srt?api_key=token+value",
            builder.buildSubtitleDeliveryUrl(
                serverUrl = "http://127.0.0.1:8096/",
                deliveryUrl = "/Videos/movie+1/media-source/Subtitles/2/Stream.srt",
                accessToken = "token value",
            ),
        )
    }

    @Test
    fun buildSubtitleDeliveryUrl_keepsAbsoluteUrlWithExistingToken() {
        assertEquals(
            "http://cdn.emby.test/subtitle.vtt?api_key=existing",
            builder.buildSubtitleDeliveryUrl(
                serverUrl = "http://127.0.0.1:8096/",
                deliveryUrl = "http://cdn.emby.test/subtitle.vtt?api_key=existing",
                accessToken = "token value",
            ),
        )
    }

    @Test
    fun buildSubtitleDeliveryUrl_keepsAbsoluteUrlWithCaseInsensitiveExistingToken() {
        assertEquals(
            "http://cdn.emby.test/subtitle.vtt?API_KEY=existing",
            builder.buildSubtitleDeliveryUrl(
                serverUrl = "http://127.0.0.1:8096/",
                deliveryUrl = "http://cdn.emby.test/subtitle.vtt?API_KEY=existing",
                accessToken = "token value",
            ),
        )
    }

    @Test
    fun buildSubtitleDeliveryUrl_doesNotTreatSimilarQueryParameterAsExistingToken() {
        assertEquals(
            "http://cdn.emby.test/subtitle.vtt?not_api_key=existing&api_key=token+value",
            builder.buildSubtitleDeliveryUrl(
                serverUrl = "http://127.0.0.1:8096/",
                deliveryUrl = "http://cdn.emby.test/subtitle.vtt?not_api_key=existing",
                accessToken = "token value",
            ),
        )
    }
}
