package com.embytv.ui.player

import androidx.media3.common.C
import androidx.media3.common.MimeTypes
import com.embytv.domain.model.PlaybackDetails
import com.embytv.domain.model.PlaybackSource
import com.embytv.domain.model.PlaybackTrack
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class PlayerMediaItemFactoryTest {
    @Test
    fun createCarriesStableMediaIdAndTitleForMedia3Callbacks() {
        val source = PlaybackSource(
            itemId = "movie-1",
            title = "Movie",
            streamUrl = "http://emby.test/Videos/movie-1/stream",
            details = PlaybackDetails(),
        )

        val mediaItem = PlayerMediaItemFactory.create(source)

        assertEquals("movie-1", mediaItem.mediaId)
        assertEquals("Movie", mediaItem.mediaMetadata.title.toString())
    }

    @Test
    fun createIncludesSupportedExternalSubtitleConfigurations() {
        val source = PlaybackSource(
            itemId = "movie-1",
            title = "Movie",
            streamUrl = "http://emby.test/Videos/movie-1/stream",
            details = PlaybackDetails(
                subtitleTracks = listOf(
                    PlaybackTrack(
                        index = 2,
                        codec = "srt",
                        language = "chi",
                        isDefault = true,
                        isExternal = true,
                        externalUrl = "http://emby.test/subtitle.srt?api_key=token",
                    ),
                    PlaybackTrack(
                        index = 3,
                        codec = "ass",
                        language = "eng",
                        isExternal = true,
                        externalUrl = "http://emby.test/subtitle.ass?api_key=token",
                    ),
                ),
            ),
        )

        PlayerMediaItemFactory.create(source)

        val subtitles = PlayerMediaItemFactory.externalSubtitlesFor(source)
        assertEquals(2, subtitles.size)
        assertEquals("http://emby.test/subtitle.srt?api_key=token", subtitles[0].url)
        assertEquals(MimeTypes.APPLICATION_SUBRIP, subtitles[0].mimeType)
        assertEquals("zh-CN", subtitles[0].language)
        assertEquals("Chinese (Simplified) · SRT · External", subtitles[0].label)
        assertEquals(C.SELECTION_FLAG_DEFAULT, subtitles[0].selectionFlags)
        assertEquals("http://emby.test/subtitle.ass?api_key=token", subtitles[1].url)
        assertEquals(MimeTypes.TEXT_SSA, subtitles[1].mimeType)
        assertEquals("en", subtitles[1].language)
    }

    @Test
    fun externalSubtitleSelectionFlagsCombineDefaultAndForced() {
        val source = PlaybackSource(
            itemId = "movie-1",
            title = "Movie",
            streamUrl = "http://emby.test/Videos/movie-1/stream",
            details = PlaybackDetails(
                subtitleTracks = listOf(
                    PlaybackTrack(
                        index = 2,
                        codec = "srt",
                        language = "eng",
                        isDefault = true,
                        isForced = true,
                        isExternal = true,
                        externalUrl = "http://emby.test/subtitle.forced.srt?api_key=token",
                    ),
                ),
            ),
        )

        val subtitles = PlayerMediaItemFactory.externalSubtitlesFor(source)

        assertEquals(C.SELECTION_FLAG_DEFAULT or C.SELECTION_FLAG_FORCED, subtitles.single().selectionFlags)
    }

    @Test
    fun externalSubtitleLanguagesNormalizeUnderscoreVariantsForMedia3() {
        val source = PlaybackSource(
            itemId = "movie-1",
            title = "Movie",
            streamUrl = "http://emby.test/Videos/movie-1/stream",
            details = PlaybackDetails(
                subtitleTracks = listOf(
                    PlaybackTrack(
                        index = 2,
                        codec = "srt",
                        language = "zh_Hans",
                        isExternal = true,
                        externalUrl = "http://emby.test/subtitle.zh.srt?api_key=token",
                    ),
                    PlaybackTrack(
                        index = 3,
                        codec = "vtt",
                        language = "en_US",
                        isExternal = true,
                        externalUrl = "http://emby.test/subtitle.en.vtt?api_key=token",
                    ),
                ),
            ),
        )

        val subtitles = PlayerMediaItemFactory.externalSubtitlesFor(source)

        assertEquals("zh-CN", subtitles[0].language)
        assertEquals("en-US", subtitles[1].language)
    }

    @Test
    fun externalSubtitleLanguagesNormalizeChineseScriptAndRegionVariantsForMedia3() {
        val source = PlaybackSource(
            itemId = "movie-1",
            title = "Movie",
            streamUrl = "http://emby.test/Videos/movie-1/stream",
            details = PlaybackDetails(
                subtitleTracks = listOf(
                    PlaybackTrack(
                        index = 2,
                        codec = "srt",
                        language = "zh_Hans_CN",
                        isExternal = true,
                        externalUrl = "http://emby.test/subtitle.zh-hans-cn.srt?api_key=token",
                    ),
                    PlaybackTrack(
                        index = 3,
                        codec = "ass",
                        language = "zh_Hant_HK",
                        isExternal = true,
                        externalUrl = "http://emby.test/subtitle.zh-hant-hk.ass?api_key=token",
                    ),
                ),
            ),
        )

        val subtitles = PlayerMediaItemFactory.externalSubtitlesFor(source)

        assertEquals("zh-CN", subtitles[0].language)
        assertEquals("zh-TW", subtitles[1].language)
    }

    @Test
    fun externalSubtitleLanguageTagsUseStableAsciiCaseAcrossLocales() {
        val previousLocale = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"))
            val source = PlaybackSource(
                itemId = "movie-1",
                title = "Movie",
                streamUrl = "http://emby.test/Videos/movie-1/stream",
                details = PlaybackDetails(
                    subtitleTracks = listOf(
                        PlaybackTrack(
                            index = 2,
                            codec = "srt",
                            language = "az_ir",
                            isExternal = true,
                            externalUrl = "http://emby.test/subtitle.az.srt?api_key=token",
                        ),
                    ),
                ),
            )

            val subtitles = PlayerMediaItemFactory.externalSubtitlesFor(source)

            assertEquals("az-IR", subtitles.single().language)
        } finally {
            Locale.setDefault(previousLocale)
        }
    }

    @Test
    fun createKeepsVideoOnlyMediaItemWhenNoSupportedExternalSubtitleExists() {
        val source = PlaybackSource(
            itemId = "movie-1",
            title = "Movie",
            streamUrl = "http://emby.test/Videos/movie-1/stream",
            details = PlaybackDetails(
                subtitleTracks = listOf(
                    PlaybackTrack(
                        index = 2,
                        codec = "pgs",
                        isExternal = true,
                        externalUrl = "http://emby.test/subtitle.sup?api_key=token",
                    ),
                ),
            ),
        )

        PlayerMediaItemFactory.create(source)

        assertTrue(PlayerMediaItemFactory.externalSubtitlesFor(source).isEmpty())
    }

    @Test
    fun externalSubtitleMimeTypeFallsBackToDeliveryUrlExtensionWhenCodecIsMissing() {
        val source = PlaybackSource(
            itemId = "movie-1",
            title = "Movie",
            streamUrl = "http://emby.test/Videos/movie-1/stream",
            details = PlaybackDetails(
                subtitleTracks = listOf(
                    PlaybackTrack(
                        index = 2,
                        codec = null,
                        language = "zh_CN",
                        isExternal = true,
                        externalUrl = "http://emby.test/Videos/movie-1/Subtitles/2/Stream.vtt?api_key=token",
                    ),
                    PlaybackTrack(
                        index = 3,
                        codec = "",
                        language = "eng",
                        isExternal = true,
                        externalUrl = "/Videos/movie-1/Subtitles/3/Stream.ass?api_key=token",
                    ),
                ),
            ),
        )

        val subtitles = PlayerMediaItemFactory.externalSubtitlesFor(source)

        assertEquals(2, subtitles.size)
        assertEquals(MimeTypes.TEXT_VTT, subtitles[0].mimeType)
        assertEquals("zh-CN", subtitles[0].language)
        assertEquals(MimeTypes.TEXT_SSA, subtitles[1].mimeType)
        assertEquals("en", subtitles[1].language)
    }

    @Test
    fun externalSubtitleUrlIsTrimmedBeforeMedia3Configuration() {
        val source = PlaybackSource(
            itemId = "movie-1",
            title = "Movie",
            streamUrl = "http://emby.test/Videos/movie-1/stream",
            details = PlaybackDetails(
                subtitleTracks = listOf(
                    PlaybackTrack(
                        index = 2,
                        codec = "srt",
                        language = "eng",
                        isExternal = true,
                        externalUrl = "  http://emby.test/subtitle.srt?api_key=token  ",
                    ),
                ),
            ),
        )

        val subtitles = PlayerMediaItemFactory.externalSubtitlesFor(source)

        assertEquals("http://emby.test/subtitle.srt?api_key=token", subtitles.single().url)
    }
}
