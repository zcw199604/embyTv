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
import com.embytv.domain.model.EmbyLibrarySummary
import com.embytv.domain.model.EmbySession
import com.embytv.domain.model.MediaItemSummary
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
        assertEquals("Movie", dashboard.libraryLatestSections.single().items.single().type)
        assertEquals("http://emby.test/Items/library-latest-1/Images/Primary?tag=movie-primary", dashboard.libraryLatestSections.single().items.single().imageUrl)
        assertNull(dashboard.libraryLatestSections.single().items.single().unplayedItemCount)
    }

    @Test
    fun loadHomeDashboardBuildsImagesFromPrimaryImageTagAndParentFields() = runTest(dispatcher) {
        api.views = listOf(
            EmbyItemDto(
                id = "library-no-tags",
                name = "无 ImageTags 媒体库",
                type = "CollectionFolder",
                collectionType = "movies",
                childCount = 7,
                overview = null,
                primaryImageTag = "view-primary",
                imageTags = null,
            ),
        )
        api.latestItemsHandler = { request ->
            if (request.parentId == "library-no-tags") {
                listOf(
                    EmbyItemDto(
                        id = "movie-no-tags",
                        name = "无 ImageTags 电影",
                        type = "Movie",
                        overview = null,
                        primaryImageTag = "movie-primary",
                        imageTags = null,
                        parentThumbItemId = "movie-parent",
                        parentThumbImageTag = "movie-thumb",
                    ),
                )
            } else {
                emptyList()
            }
        }

        val dashboard = repository.loadHomeDashboard(session, "device-1").getOrThrow()

        assertEquals("http://emby.test/Items/library-no-tags/Images/Primary?tag=view-primary", dashboard.libraries.single().imageUrl)
        assertEquals("http://emby.test/Items/movie-no-tags/Images/Primary?tag=movie-primary", dashboard.libraryLatestSections.single().items.single().imageUrl)
        assertEquals("http://emby.test/Items/movie-parent/Images/Thumb?tag=movie-thumb", dashboard.libraryLatestSections.single().items.single().thumbImageUrl)
    }

    @Test
    fun loadHomeDashboardGroupsTvLatestEpisodesAsSeriesAndRequestsGroupItems() = runTest(dispatcher) {
        api.views = listOf(
            EmbyItemDto(
                id = "tv-library",
                name = "电视剧",
                type = "CollectionFolder",
                collectionType = "tvshows",
                childCount = 2,
                overview = null,
                imageTags = mapOf("Primary" to "tv-tag"),
            ),
        )
        api.latestItemsHandler = { request ->
            if (request.parentId == "tv-library") {
                listOf(
                    episodeFromSeries(id = "episode-2", indexNumber = 2),
                    episodeFromSeries(id = "episode-1", indexNumber = 1),
                )
            } else {
                emptyList()
            }
        }

        val dashboard = repository.loadHomeDashboard(session, "device-1").getOrThrow()

        val latestRequest = api.latestRequests.single { it.parentId == "tv-library" }
        assertEquals("Episode", latestRequest.includeItemTypes)
        assertEquals(true, latestRequest.groupItems)
        assertEquals(8, latestRequest.limit)

        val series = dashboard.libraryLatestSections.single().items.single()
        assertEquals("series-1", series.id)
        assertEquals("真实剧集", series.name)
        assertEquals("Series", series.type)
        assertEquals("series-1", series.seriesId)
        assertEquals(3, series.unplayedItemCount)
        assertEquals("http://emby.test/Items/series-1/Images/Primary?tag=series-primary", series.imageUrl)
        assertEquals("http://emby.test/Items/series-1/Images/Backdrop/0?tag=series-backdrop", series.backdropImageUrl)
    }

    @Test
    fun loadLibraryContentUsesLibrarySpecificItemTypes() = runTest(dispatcher) {
        api.itemsByParentHandler = { request ->
            EmbyItemsResponse(
                items = listOf(
                    EmbyItemDto(
                        id = "${request.parentId}-item",
                        name = "资源",
                        type = request.includeItemTypes.split(",").first(),
                        overview = null,
                        imageTags = null,
                    ),
                ),
                totalRecordCount = 1,
            )
        }

        val movies = repository.loadLibraryContent(
            session = session,
            deviceId = "device-1",
            library = EmbyLibrarySummary(
                id = "movie-library",
                name = "电影",
                type = "CollectionFolder",
                collectionType = "movies",
                itemCount = 1,
                imageUrl = null,
            ),
        ).getOrThrow()
        val shows = repository.loadLibraryContent(
            session = session,
            deviceId = "device-1",
            library = EmbyLibrarySummary(
                id = "tv-library",
                name = "电视剧",
                type = "CollectionFolder",
                collectionType = "tvshows",
                itemCount = 1,
                imageUrl = null,
            ),
        ).getOrThrow()

        val movieRequest = api.itemsByParentRequests.single { it.parentId == "movie-library" }
        val tvRequest = api.itemsByParentRequests.single { it.parentId == "tv-library" }
        assertEquals("Movie", movieRequest.includeItemTypes)
        assertEquals("Series", tvRequest.includeItemTypes)
        assertEquals(60, movieRequest.limit)
        assertEquals(60, tvRequest.limit)
        assertEquals("movie-library-item", movies.items.single().id)
        assertEquals("tv-library-item", shows.items.single().id)
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

    private fun episodeFromSeries(id: String, indexNumber: Int): EmbyItemDto =
        EmbyItemDto(
            id = id,
            name = "第 $indexNumber 集",
            type = "Episode",
            overview = null,
            imageTags = null,
            parentId = "season-1",
            seriesId = "series-1",
            seriesName = "真实剧集",
            seriesPrimaryImageTag = "series-primary",
            parentBackdropItemId = "series-1",
            parentBackdropImageTags = listOf("series-backdrop"),
            userData = EmbyUserDataDto(unplayedItemCount = 3),
            parentIndexNumber = 1,
            indexNumber = indexNumber,
        )
}

private class FakeEmbyApiProvider(
    private val api: EmbyApi,
) : EmbyApiProvider {
    override fun create(baseUrl: String, accessToken: String?): EmbyApi = api
}

private data class ItemsByParentRequest(
    val parentId: String,
    val includeItemTypes: String,
    val startIndex: Int,
    val limit: Int,
    val sortBy: String?,
    val sortOrder: String?,
)

private data class LatestItemsRequest(
    val parentId: String?,
    val includeItemTypes: String,
    val groupItems: Boolean?,
    val limit: Int,
)

private class FakeEmbyApi : EmbyApi {
    var views: List<EmbyItemDto> = listOf(
        EmbyItemDto(
            id = "library-1",
            name = "电影",
            type = "CollectionFolder",
            collectionType = "movies",
            childCount = 42,
            overview = null,
            imageTags = mapOf("Primary" to "library-tag"),
        ),
    )
    val itemsByParentRequests = mutableListOf<ItemsByParentRequest>()
    val latestRequests = mutableListOf<LatestItemsRequest>()
    var itemsByParentHandler: (ItemsByParentRequest) -> EmbyItemsResponse = { request ->
        if (request.limit == 0) {
            EmbyItemsResponse(totalRecordCount = 42)
        } else {
            EmbyItemsResponse(
                items = listOf(
                    EmbyItemDto(
                        id = "library-latest-1",
                        name = "库内新电影",
                        type = "Movie",
                        overview = "库内最新",
                        imageTags = mapOf("Primary" to "movie-primary"),
                        parentId = request.parentId,
                        productionYear = 2026,
                    ),
                ),
            )
        }
    }
    var latestItemsHandler: (LatestItemsRequest) -> List<EmbyItemDto> = { request ->
        if (request.parentId == null) {
            listOf(
                EmbyItemDto(
                    id = "latest-1",
                    name = "新电影",
                    type = "Movie",
                    overview = null,
                    imageTags = null,
                    productionYear = 2026,
                ),
            )
        } else {
            itemsByParentHandler(
                ItemsByParentRequest(
                    parentId = request.parentId,
                    includeItemTypes = request.includeItemTypes,
                    startIndex = 0,
                    limit = request.limit,
                    sortBy = "DateCreated",
                    sortOrder = "Descending",
                ),
            ).items
        }
    }

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
    ): EmbyViewsResponse = EmbyViewsResponse(items = views)

    override suspend fun getItemsByParent(
        authorization: String,
        userId: String,
        parentId: String,
        recursive: Boolean,
        includeItemTypes: String,
        startIndex: Int,
        limit: Int,
        sortBy: String?,
        sortOrder: String?,
        fields: String,
    ): EmbyItemsResponse {
        val request = ItemsByParentRequest(
            parentId = parentId,
            includeItemTypes = includeItemTypes,
            startIndex = startIndex,
            limit = limit,
            sortBy = sortBy,
            sortOrder = sortOrder,
        )
        itemsByParentRequests += request
        return itemsByParentHandler(request)
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
        parentId: String?,
        includeItemTypes: String,
        groupItems: Boolean?,
        fields: String,
        limit: Int,
    ): List<EmbyItemDto> {
        val request = LatestItemsRequest(
            parentId = parentId,
            includeItemTypes = includeItemTypes,
            groupItems = groupItems,
            limit = limit,
        )
        latestRequests += request
        return latestItemsHandler(request)
    }

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
