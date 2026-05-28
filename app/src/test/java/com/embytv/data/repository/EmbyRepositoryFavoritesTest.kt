package com.embytv.data.repository

import com.embytv.data.remote.EmbyApi
import com.embytv.data.remote.EmbyApiProvider
import com.embytv.data.remote.dto.EmbyAuthRequest
import com.embytv.data.remote.dto.EmbyAuthResponse
import com.embytv.data.remote.dto.EmbyItemDto
import com.embytv.data.remote.dto.EmbyItemsResponse
import com.embytv.data.remote.dto.EmbyPlaybackInfoResponse
import com.embytv.data.remote.dto.EmbyPlaybackProgressRequest
import com.embytv.data.remote.dto.EmbyPlaybackStartRequest
import com.embytv.data.remote.dto.EmbyPlaybackStoppedRequest
import com.embytv.data.remote.dto.EmbyUserDataDto
import com.embytv.data.remote.dto.EmbyViewsResponse
import com.embytv.domain.model.EmbySession
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class EmbyRepositoryFavoritesTest {
    private val dispatcher = StandardTestDispatcher()
    private val api = FavoritesFakeEmbyApi()
    private val repository = EmbyRepository(
        apiFactory = FavoritesFakeEmbyApiProvider(api),
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
    fun loadFavoriteDashboardRequestsFavoriteMoviesSeriesAndEpisodes() = runTest(dispatcher) {
        repository.loadFavoriteDashboard(session, "device-1").getOrThrow()

        val request = api.itemsRequests.single()
        assertEquals(true, request.recursive)
        assertEquals("Movie,Series,Episode", request.includeItemTypes)
        assertEquals("IsFavorite", request.filters)
        assertEquals(0, request.startIndex)
        assertEquals(60, request.limit)
        assertEquals("DateCreated", request.sortBy)
        assertEquals("Descending", request.sortOrder)
        assertEquals(true, request.enableUserData)
    }

    @Test
    fun loadFavoriteDashboardGroupsFavoriteEpisodesAsSeriesWithoutBlankTitle() = runTest(dispatcher) {
        api.items = listOf(
            EmbyItemDto(
                id = "movie-1",
                name = "收藏电影",
                type = "Movie",
                overview = null,
                imageTags = mapOf("Primary" to "movie-primary"),
            ),
            EmbyItemDto(
                id = "series-raw",
                name = "已收藏剧集",
                type = "Series",
                overview = null,
                imageTags = mapOf("Primary" to "series-primary"),
                userData = EmbyUserDataDto(unplayedItemCount = 5),
            ),
            favoriteEpisode(id = "episode-1", seriesId = "series-episode", seriesName = "单集收藏剧"),
            favoriteEpisode(id = "episode-2", seriesId = "series-episode", seriesName = "单集收藏剧"),
            favoriteEpisode(id = "episode-blank-name", seriesId = "series-fallback", seriesName = ""),
        )

        val dashboard = repository.loadFavoriteDashboard(session, "device-1").getOrThrow()

        assertEquals(1, dashboard.movies.size)
        assertEquals("movie-1", dashboard.movies.single().id)
        assertEquals("收藏电影", dashboard.movies.single().name)
        assertEquals("http://emby.test/Items/movie-1/Images/Primary?tag=movie-primary", dashboard.movies.single().imageUrl)

        assertEquals(listOf("series-raw", "series-episode", "series-fallback"), dashboard.series.map { it.id })
        assertEquals("已收藏剧集", dashboard.series[0].name)
        assertEquals("单集收藏剧", dashboard.series[1].name)
        assertEquals("series-fallback", dashboard.series[2].name)
        assertEquals("http://emby.test/Items/series-episode/Images/Primary?tag=episode-series-primary", dashboard.series[1].imageUrl)
        assertEquals(5, dashboard.series[0].unplayedItemCount)
        assertNotNull(dashboard.series[1].imageUrl)
    }

    @Test
    fun loadFavoriteDashboardDoesNotBuildSeriesImageFromNameWhenSeriesIdIsMissing() = runTest(dispatcher) {
        api.items = listOf(
            EmbyItemDto(
                id = "episode-only",
                name = "第 1 集",
                type = "Episode",
                overview = null,
                imageTags = mapOf("Primary" to "episode-primary"),
                seriesName = "只有剧名的收藏",
            ),
        )

        val dashboard = repository.loadFavoriteDashboard(session, "device-1").getOrThrow()

        val series = dashboard.series.single()
        assertEquals("只有剧名的收藏", series.id)
        assertEquals("只有剧名的收藏", series.name)
        assertEquals("http://emby.test/Items/episode-only/Images/Primary?tag=episode-primary", series.imageUrl)
    }

    private fun favoriteEpisode(id: String, seriesId: String, seriesName: String): EmbyItemDto =
        EmbyItemDto(
            id = id,
            name = "第 1 集",
            type = "Episode",
            overview = null,
            imageTags = null,
            seriesId = seriesId,
            seriesName = seriesName,
            seriesPrimaryImageTag = "episode-series-primary",
            userData = EmbyUserDataDto(unplayedItemCount = 3),
            parentIndexNumber = 1,
            indexNumber = 1,
        )
}

private class FavoritesFakeEmbyApiProvider(
    private val api: EmbyApi,
) : EmbyApiProvider {
    override fun create(baseUrl: String, accessToken: String?): EmbyApi = api
}

private data class FavoriteItemsRequest(
    val recursive: Boolean,
    val includeItemTypes: String,
    val fields: String,
    val filters: String?,
    val startIndex: Int,
    val limit: Int?,
    val sortBy: String?,
    val sortOrder: String?,
    val enableUserData: Boolean,
)

private class FavoritesFakeEmbyApi : EmbyApi {
    val itemsRequests = mutableListOf<FavoriteItemsRequest>()
    var items: List<EmbyItemDto> = emptyList()

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
    ): EmbyItemsResponse {
        itemsRequests += FavoriteItemsRequest(
            recursive = recursive,
            includeItemTypes = includeItemTypes,
            fields = fields,
            filters = filters,
            startIndex = startIndex,
            limit = limit,
            sortBy = sortBy,
            sortOrder = sortOrder,
            enableUserData = enableUserData,
        )
        return EmbyItemsResponse(items = items, totalRecordCount = items.size)
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

    override suspend fun getPlaybackInfo(
        authorization: String,
        itemId: String,
        userId: String,
    ): EmbyPlaybackInfoResponse = error("Not used")

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
