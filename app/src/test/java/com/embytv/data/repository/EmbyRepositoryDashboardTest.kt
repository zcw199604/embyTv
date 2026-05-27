package com.embytv.data.repository

import com.embytv.data.remote.EmbyApi
import com.embytv.data.remote.EmbyApiProvider
import com.embytv.data.remote.dto.EmbyAuthRequest
import com.embytv.data.remote.dto.EmbyAuthResponse
import com.embytv.data.remote.dto.EmbyItemDto
import com.embytv.data.remote.dto.EmbyItemsResponse
import com.embytv.data.remote.dto.EmbyMediaSourceDto
import com.embytv.data.remote.dto.EmbyMediaStreamDto
import com.embytv.data.remote.dto.EmbyPlaybackInfoResponse
import com.embytv.data.remote.dto.EmbyPlaybackProgressRequest
import com.embytv.data.remote.dto.EmbyPlaybackStartRequest
import com.embytv.data.remote.dto.EmbyPlaybackStoppedRequest
import com.embytv.data.remote.dto.EmbyUserDataDto
import com.embytv.data.remote.dto.EmbyViewsResponse
import com.embytv.domain.model.EmbySession
import com.embytv.domain.model.MediaItemSummary
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class EmbyRepositoryDashboardTest {
    private val dispatcher = StandardTestDispatcher()
    private val api = FakeEmbyApi()
    private val repository = EmbyRepository(
        apiFactory = FakeEmbyApiProvider(api),
        streamUrlBuilder = EmbyStreamUrlBuilder(),
        ioDispatcher = dispatcher,
    )
    private val session = EmbySession(
        serverUrl = "http://emby.test/",
        userId = "user-1",
        accessToken = "token-value",
        serverId = "server-1",
    )

    @Test
    fun loadHomeDashboardMapsViewsResumeAndLatestFromEmby() = runTest(dispatcher) {
        val dashboard = repository.loadHomeDashboard(session, "device-1").getOrThrow()

        assertEquals(1, dashboard.libraries.size)
        assertEquals("library-1", dashboard.libraries.single().id)
        assertEquals("电影", dashboard.libraries.single().name)
        assertEquals("movies", dashboard.libraries.single().collectionType)
        assertEquals(42, dashboard.libraries.single().itemCount)
        assertEquals("http://emby.test/Items/library-1/Images/Primary?tag=library-tag", dashboard.libraries.single().imageUrl)

        assertEquals("resume-1", dashboard.resumeItems.single().id)
        assertEquals("真实剧集", dashboard.resumeItems.single().seriesName)
        assertEquals(12_000L, dashboard.resumeItems.single().runTimeTicks)
        assertEquals(3_000L, dashboard.resumeItems.single().playbackPositionTicks)
        assertEquals(25.0, dashboard.resumeItems.single().playedPercentage)

        assertEquals("latest-1", dashboard.latestItems.single().id)
        assertEquals(2026, dashboard.latestItems.single().productionYear)
        assertEquals(1, dashboard.libraryLatestSections.size)
        assertEquals("library-1", dashboard.libraryLatestSections.single().library.id)
        assertEquals("library-latest-1", dashboard.libraryLatestSections.single().items.single().id)
        assertEquals("真实剧集", dashboard.libraryLatestSections.single().items.single().seriesName)
        assertEquals(1, dashboard.libraryLatestSections.single().items.single().parentIndexNumber)
        assertEquals(2, dashboard.libraryLatestSections.single().items.single().indexNumber)
        assertEquals("http://emby.test/Items/library-latest-1/Images/Thumb?tag=thumb-tag", dashboard.libraryLatestSections.single().items.single().thumbImageUrl)
        assertEquals("http://emby.test/Items/library-latest-1/Images/Backdrop/0?tag=backdrop-tag", dashboard.libraryLatestSections.single().items.single().backdropImageUrl)
    }

    @Test
    fun createPlaybackSourceWithDetailsMapsPlaybackInfoStreams() = runTest(dispatcher) {
        val source = repository.createPlaybackSourceWithDetails(
            session = session,
            deviceId = "device-1",
            item = MediaItemSummary(
                id = "resume-1",
                name = "第 1 集",
                type = "Episode",
                overview = null,
                imageUrl = null,
            ),
        ).getOrThrow()

        assertEquals("resume-1", source.itemId)
        assertEquals("play-session", source.details.playSessionId)
        assertEquals("media-source", source.details.mediaSourceId)
        assertEquals("MKV", source.details.container)
        assertEquals("Direct Play · MKV · HEVC", source.details.playbackSummaryLabel)
        assertEquals("2160p · HDR10", source.details.qualityLabel)
        assertEquals("EAC3 5.1", source.details.audioLabel)
        assertEquals("简体中文", source.details.subtitleLabel)
    }
}

private class FakeEmbyApiProvider(
    private val api: EmbyApi,
) : EmbyApiProvider {
    override fun create(baseUrl: String, accessToken: String?): EmbyApi = api
}

private class FakeEmbyApi : EmbyApi {
    override suspend fun authenticateByName(
        authorization: String,
        request: EmbyAuthRequest,
    ): EmbyAuthResponse = error("Not used")

    override suspend fun getItems(
        authorization: String,
        userId: String,
        recursive: Boolean,
        includeItemTypes: String,
        fields: String,
    ): EmbyItemsResponse = error("Not used")

    override suspend fun getViews(
        authorization: String,
        userId: String,
    ): EmbyViewsResponse = EmbyViewsResponse(
        items = listOf(
            EmbyItemDto(
                id = "library-1",
                name = "电影",
                type = "CollectionFolder",
                collectionType = "movies",
                childCount = 42,
                overview = null,
                imageTags = mapOf("Primary" to "library-tag"),
            ),
        ),
    )

    override suspend fun getItemsByParent(
        authorization: String,
        userId: String,
        parentId: String,
        recursive: Boolean,
        includeItemTypes: String,
        limit: Int,
        sortBy: String?,
        sortOrder: String?,
        fields: String,
    ): EmbyItemsResponse = if (limit == 0) {
        EmbyItemsResponse(totalRecordCount = 42)
    } else {
        EmbyItemsResponse(
            items = listOf(
                EmbyItemDto(
                    id = "library-latest-1",
                    name = "第 2 集",
                    type = "Episode",
                    overview = "库内最新",
                    imageTags = mapOf("Primary" to "primary-tag", "Thumb" to "thumb-tag"),
                    backdropImageTags = listOf("backdrop-tag"),
                    parentId = "library-1",
                    seriesName = "真实剧集",
                    seasonName = "Season 1",
                    parentIndexNumber = 1,
                    indexNumber = 2,
                ),
            ),
        )
    }

    override suspend fun getResumeItems(
        authorization: String,
        userId: String,
        recursive: Boolean,
        mediaTypes: String,
        fields: String,
        limit: Int,
    ): EmbyItemsResponse = EmbyItemsResponse(
        items = listOf(
            EmbyItemDto(
                id = "resume-1",
                name = "第 1 集",
                type = "Episode",
                overview = "简介",
                imageTags = mapOf("Primary" to "resume-tag"),
                userData = EmbyUserDataDto(
                    playbackPositionTicks = 3_000L,
                    playedPercentage = 25.0,
                ),
                runTimeTicks = 12_000L,
                seriesName = "真实剧集",
                seasonName = "Season 1",
                productionYear = 2026,
            ),
        ),
    )

    override suspend fun getLatestItems(
        authorization: String,
        userId: String,
        includeItemTypes: String,
        fields: String,
        limit: Int,
    ): List<EmbyItemDto> = listOf(
        EmbyItemDto(
            id = "latest-1",
            name = "新电影",
            type = "Movie",
            overview = null,
            imageTags = null,
            productionYear = 2026,
        ),
    )

    override suspend fun getPlaybackInfo(
        authorization: String,
        itemId: String,
        userId: String,
    ): EmbyPlaybackInfoResponse = EmbyPlaybackInfoResponse(
        playSessionId = "play-session",
        mediaSources = listOf(
            EmbyMediaSourceDto(
                id = "media-source",
                container = "MKV",
                bitrate = 4_000_000,
                mediaStreams = listOf(
                    EmbyMediaStreamDto(
                        index = 0,
                        type = "Video",
                        codec = "hevc",
                        displayTitle = null,
                        language = null,
                        channels = null,
                        width = 3840,
                        height = 2160,
                        videoRange = "HDR10",
                    ),
                    EmbyMediaStreamDto(
                        index = 1,
                        type = "Audio",
                        codec = "eac3",
                        displayTitle = "EAC3 5.1",
                        language = "chi",
                        channels = 6,
                        width = null,
                        height = null,
                        videoRange = null,
                        isDefault = true,
                    ),
                    EmbyMediaStreamDto(
                        index = 2,
                        type = "Subtitle",
                        codec = "srt",
                        displayTitle = "简体中文",
                        language = "chi",
                        channels = null,
                        width = null,
                        height = null,
                        videoRange = null,
                        isDefault = true,
                    ),
                ),
            ),
        ),
    )

    override suspend fun reportPlaybackStarted(
        authorization: String,
        request: EmbyPlaybackStartRequest,
    ) {
        error("Not used")
    }

    override suspend fun reportPlaybackProgress(
        authorization: String,
        request: EmbyPlaybackProgressRequest,
    ) {
        error("Not used")
    }

    override suspend fun reportPlaybackStopped(
        authorization: String,
        request: EmbyPlaybackStoppedRequest,
    ) {
        error("Not used")
    }
}
