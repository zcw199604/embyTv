package com.embytv.data.repository

import com.embytv.data.remote.EmbyApi
import com.embytv.data.remote.EmbyApiProvider
import com.embytv.data.remote.dto.EmbyAuthRequest
import com.embytv.data.remote.dto.EmbyAuthResponse
import com.embytv.data.remote.dto.EmbyItemDto
import com.embytv.data.remote.dto.EmbyItemsResponse
import com.embytv.data.remote.dto.EmbyMediaSourceDto
import com.embytv.data.remote.dto.EmbyPersonDto
import com.embytv.data.remote.dto.EmbyPlaybackInfoResponse
import com.embytv.data.remote.dto.EmbyPlaybackProgressRequest
import com.embytv.data.remote.dto.EmbyPlaybackStartRequest
import com.embytv.data.remote.dto.EmbyPlaybackStoppedRequest
import com.embytv.data.remote.dto.EmbyStudioDto
import com.embytv.data.remote.dto.EmbyUserDataDto
import com.embytv.data.remote.dto.EmbyViewsResponse
import com.embytv.domain.model.EmbySeasonSummary
import com.embytv.domain.model.EmbySession
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EmbyRepositoryMediaDetailTest {
    private val dispatcher = StandardTestDispatcher()
    private val api = MediaDetailFakeEmbyApi()
    private val repository = EmbyRepository(
        apiFactory = MediaDetailFakeEmbyApiProvider(api),
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
    fun loadMovieDetailRequestsUserItemAndMapsMetadata() = runTest(dispatcher) {
        api.item = movieDetail()

        val detail = repository.loadMediaDetail(session, "device-1", "movie-1").getOrThrow()

        val request = api.itemRequests.single()
        assertEquals("user-1", request.userId)
        assertEquals("movie-1", request.itemId)
        assertEquals(true, request.fields.contains("People"))
        assertEquals(true, request.fields.contains("Genres"))

        assertEquals("movie-1", detail.item.id)
        assertEquals("真实电影", detail.item.name)
        assertEquals("真实简介", detail.item.overview)
        assertEquals("http://emby.test/Items/movie-1/Images/Primary?tag=movie-primary", detail.item.imageUrl)
        assertEquals(listOf("剧情", "科幻"), detail.genres)
        assertEquals(listOf("电影公司"), detail.studios)
        assertEquals("PG-13", detail.officialRating)
        assertEquals(8.6, detail.communityRating)
        assertEquals("演员甲", detail.people.single().name)
        assertEquals("主角", detail.people.single().role)
        assertEquals(emptyList<EmbySeasonSummary>(), detail.seasons)
    }

    @Test
    fun loadSeriesDetailRequestsSeasonsAndMapsUnplayedSeasonBadgeSource() = runTest(dispatcher) {
        api.item = seriesDetail()
        api.seasons = listOf(
            EmbyItemDto(
                id = "season-1",
                name = "第 1 季",
                type = "Season",
                overview = null,
                imageTags = mapOf("Primary" to "season-primary"),
                indexNumber = 1,
                childCount = 12,
                userData = EmbyUserDataDto(unplayedItemCount = 4),
            ),
            EmbyItemDto(
                id = "season-2",
                name = "第 2 季",
                type = "Season",
                overview = null,
                imageTags = null,
                indexNumber = 2,
                childCount = 8,
                userData = EmbyUserDataDto(unplayedItemCount = 0),
            ),
        )

        val detail = repository.loadMediaDetail(session, "device-1", "series-1").getOrThrow()

        val seasonsRequest = api.seasonsRequests.single()
        assertEquals("series-1", seasonsRequest.seriesId)
        assertEquals("user-1", seasonsRequest.userId)
        assertEquals(true, seasonsRequest.fields.contains("UserData"))

        assertEquals("Series", detail.item.type)
        assertEquals(2, detail.seasons.size)
        assertEquals("season-1", detail.seasons[0].id)
        assertEquals("第 1 季", detail.seasons[0].name)
        assertEquals(12, detail.seasons[0].episodeCount)
        assertEquals(4, detail.seasons[0].unplayedItemCount)
        assertEquals("http://emby.test/Items/season-1/Images/Primary?tag=season-primary", detail.seasons[0].imageUrl)
        assertNull(detail.seasons[1].unplayedItemCount)
    }

    @Test
    fun loadSeasonEpisodesRequestsSeasonAndMapsEpisodeContext() = runTest(dispatcher) {
        val season = EmbySeasonSummary(
            id = "season-1",
            name = "第 1 季",
            indexNumber = 1,
            imageUrl = null,
            episodeCount = 2,
            unplayedItemCount = 1,
        )
        api.episodes = listOf(
            EmbyItemDto(
                id = "episode-1",
                name = "第一集",
                type = "Episode",
                overview = null,
                imageTags = mapOf("Thumb" to "episode-thumb"),
                seriesId = "series-1",
                seriesName = "真实剧集",
                seasonName = "第 1 季",
                parentIndexNumber = 1,
                indexNumber = 1,
                runTimeTicks = 10_000L,
                userData = EmbyUserDataDto(playbackPositionTicks = 2_500L, playedPercentage = 25.0),
            ),
        )

        val result = repository.loadSeasonEpisodes(session, "device-1", "series-1", season).getOrThrow()

        val request = api.episodeRequests.single()
        assertEquals("series-1", request.seriesId)
        assertEquals("season-1", request.seasonId)
        assertEquals("user-1", request.userId)
        assertEquals(true, request.fields.contains("ParentIndexNumber"))

        assertEquals("season-1", result.season.id)
        assertEquals("episode-1", result.episodes.single().id)
        assertEquals("真实剧集", result.episodes.single().seriesName)
        assertEquals(1, result.episodes.single().parentIndexNumber)
        assertEquals(1, result.episodes.single().indexNumber)
        assertEquals(25.0, result.episodes.single().playedPercentage)
    }

    private fun movieDetail(): EmbyItemDto =
        EmbyItemDto(
            id = "movie-1",
            name = "真实电影",
            type = "Movie",
            overview = "真实简介",
            imageTags = mapOf("Primary" to "movie-primary"),
            genres = listOf("剧情", "科幻"),
            studios = listOf(EmbyStudioDto(name = "电影公司")),
            people = listOf(
                EmbyPersonDto(
                    id = "person-1",
                    name = "演员甲",
                    role = "主角",
                    type = "Actor",
                ),
            ),
            productionYear = 2026,
            communityRating = 8.6,
            officialRating = "PG-13",
            premiereDate = "2026-05-01T00:00:00.0000000Z",
        )

    private fun seriesDetail(): EmbyItemDto =
        EmbyItemDto(
            id = "series-1",
            name = "真实剧集",
            type = "Series",
            overview = "剧集简介",
            imageTags = mapOf("Primary" to "series-primary"),
            genres = listOf("悬疑"),
            people = emptyList(),
            productionYear = 2025,
        )
}

private class MediaDetailFakeEmbyApiProvider(
    private val api: EmbyApi,
) : EmbyApiProvider {
    override fun create(baseUrl: String, accessToken: String?): EmbyApi = api
}

private data class ItemDetailRequest(
    val userId: String,
    val itemId: String,
    val fields: String,
)

private data class SeasonsRequest(
    val seriesId: String,
    val userId: String,
    val fields: String,
)

private data class EpisodesRequest(
    val seriesId: String,
    val userId: String,
    val seasonId: String,
    val fields: String,
)

private class MediaDetailFakeEmbyApi : EmbyApi {
    val itemRequests = mutableListOf<ItemDetailRequest>()
    val seasonsRequests = mutableListOf<SeasonsRequest>()
    val episodeRequests = mutableListOf<EpisodesRequest>()
    var item: EmbyItemDto? = null
    var seasons: List<EmbyItemDto> = emptyList()
    var episodes: List<EmbyItemDto> = emptyList()

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
        filters: String?,
        startIndex: Int,
        limit: Int?,
        sortBy: String?,
        sortOrder: String?,
        enableUserData: Boolean,
    ): EmbyItemsResponse = error("Not used")

    override suspend fun getItem(
        authorization: String,
        userId: String,
        itemId: String,
        fields: String,
    ): EmbyItemDto {
        itemRequests += ItemDetailRequest(userId = userId, itemId = itemId, fields = fields)
        return requireNotNull(item) { "item not configured" }
    }

    override suspend fun getViews(
        authorization: String,
        userId: String,
    ): EmbyViewsResponse = error("Not used")

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
    ): EmbyItemsResponse = error("Not used")

    override suspend fun getResumeItems(
        authorization: String,
        userId: String,
        recursive: Boolean,
        mediaTypes: String,
        fields: String,
        limit: Int,
    ): EmbyItemsResponse = error("Not used")

    override suspend fun getLatestItems(
        authorization: String,
        userId: String,
        parentId: String?,
        includeItemTypes: String,
        groupItems: Boolean?,
        fields: String,
        limit: Int,
    ): List<EmbyItemDto> = error("Not used")

    override suspend fun getSeasons(
        authorization: String,
        seriesId: String,
        userId: String,
        fields: String,
    ): EmbyItemsResponse {
        seasonsRequests += SeasonsRequest(seriesId = seriesId, userId = userId, fields = fields)
        return EmbyItemsResponse(items = seasons, totalRecordCount = seasons.size)
    }

    override suspend fun getEpisodes(
        authorization: String,
        seriesId: String,
        userId: String,
        seasonId: String,
        fields: String,
    ): EmbyItemsResponse {
        episodeRequests += EpisodesRequest(seriesId = seriesId, userId = userId, seasonId = seasonId, fields = fields)
        return EmbyItemsResponse(items = episodes, totalRecordCount = episodes.size)
    }

    override suspend fun getPlaybackInfo(
        authorization: String,
        itemId: String,
        userId: String,
    ): EmbyPlaybackInfoResponse = EmbyPlaybackInfoResponse(playSessionId = null, mediaSources = emptyList<EmbyMediaSourceDto>())

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
