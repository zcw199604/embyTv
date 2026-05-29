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
import com.embytv.data.remote.dto.EmbyUserDataUpdateRequest
import com.embytv.data.remote.dto.EmbyViewsResponse
import com.embytv.domain.model.EmbySession
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EmbyRepositorySearchTest {
    private val dispatcher = StandardTestDispatcher()
    private val api = SearchFakeEmbyApi()
    private val repository = EmbyRepository(
        apiFactory = SearchFakeEmbyApiProvider(api),
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
    fun searchItemsSendsSearchTermAndMapsReturnedItemsEvenWhenTotalCountIsZero() = runTest(dispatcher) {
        api.items = listOf(
            EmbyItemDto(
                id = "movie-1",
                name = "搜索电影",
                type = "Movie",
                overview = null,
                imageTags = mapOf("Primary" to "movie-primary"),
            ),
            EmbyItemDto(
                id = "episode-1",
                name = "第 1 集",
                type = "Episode",
                overview = null,
                imageTags = null,
                seriesName = "搜索剧集",
                parentIndexNumber = 1,
                indexNumber = 1,
            ),
        )

        val results = repository.searchItems(session, "device-1", " 搜索 ").getOrThrow()

        val request = api.itemsRequests.single()
        assertEquals("搜索", request.searchTerm)
        assertEquals("Movie,Series,Episode,BoxSet,Playlist", request.includeItemTypes)
        assertEquals(60, request.limit)
        assertEquals(listOf("movie-1", "episode-1"), results.items.map { it.id })
        assertEquals("搜索", results.query)
    }

    @Test
    fun blankSearchDoesNotCallEmby() = runTest(dispatcher) {
        val results = repository.searchItems(session, "device-1", "   ").getOrThrow()

        assertTrue(results.items.isEmpty())
        assertTrue(api.itemsRequests.isEmpty())
    }
}

private class SearchFakeEmbyApiProvider(
    private val api: EmbyApi,
) : EmbyApiProvider {
    override fun create(baseUrl: String, accessToken: String?): EmbyApi = api
}

private data class SearchItemsRequest(
    val includeItemTypes: String,
    val limit: Int?,
    val searchTerm: String?,
)

private class SearchFakeEmbyApi : EmbyApi {
    val itemsRequests = mutableListOf<SearchItemsRequest>()
    var items: List<EmbyItemDto> = emptyList()

    override suspend fun authenticateByName(authorization: String, request: EmbyAuthRequest): EmbyAuthResponse = error("Not used")

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
        searchTerm: String?,
        genreIds: String?,
        personIds: String?,
    ): EmbyItemsResponse {
        itemsRequests += SearchItemsRequest(includeItemTypes = includeItemTypes, limit = limit, searchTerm = searchTerm)
        return EmbyItemsResponse(items = items, totalRecordCount = 0)
    }

    override suspend fun getItem(authorization: String, userId: String, itemId: String, fields: String): EmbyItemDto = error("Not used")
    override suspend fun getViews(authorization: String, userId: String): EmbyViewsResponse = error("Not used")
    override suspend fun getItemsByParent(authorization: String, userId: String, parentId: String, recursive: Boolean, includeItemTypes: String, startIndex: Int, limit: Int, sortBy: String?, sortOrder: String?, fields: String): EmbyItemsResponse = error("Not used")
    override suspend fun getResumeItems(authorization: String, userId: String, recursive: Boolean, mediaTypes: String, fields: String, limit: Int): EmbyItemsResponse = error("Not used")
    override suspend fun getLatestItems(authorization: String, userId: String, parentId: String?, includeItemTypes: String, groupItems: Boolean?, fields: String, limit: Int): List<EmbyItemDto> = error("Not used")
    override suspend fun getNextUp(authorization: String, userId: String, fields: String, limit: Int, seriesId: String?): EmbyItemsResponse = error("Not used")
    override suspend fun getGenres(authorization: String, userId: String, recursive: Boolean, fields: String, startIndex: Int, limit: Int, sortBy: String, sortOrder: String, enableUserData: Boolean): EmbyItemsResponse = error("Not used")
    override suspend fun getPersons(authorization: String, userId: String, recursive: Boolean, fields: String, startIndex: Int, limit: Int, sortBy: String, sortOrder: String, enableUserData: Boolean): EmbyItemsResponse = error("Not used")
    override suspend fun getPlaylistItems(authorization: String, playlistId: String, userId: String, fields: String, startIndex: Int, limit: Int): EmbyItemsResponse = error("Not used")
    override suspend fun getSeasons(authorization: String, seriesId: String, userId: String, fields: String): EmbyItemsResponse = error("Not used")
    override suspend fun getEpisodes(authorization: String, seriesId: String, userId: String, seasonId: String, fields: String): EmbyItemsResponse = error("Not used")
    override suspend fun getPlaybackInfo(authorization: String, itemId: String, userId: String): EmbyPlaybackInfoResponse = error("Not used")
    override suspend fun reportPlaybackStarted(authorization: String, request: EmbyPlaybackStartRequest) = error("Not used")
    override suspend fun reportPlaybackProgress(authorization: String, request: EmbyPlaybackProgressRequest) = error("Not used")
    override suspend fun reportPlaybackStopped(authorization: String, request: EmbyPlaybackStoppedRequest) = error("Not used")
    override suspend fun markFavorite(authorization: String, userId: String, itemId: String) = error("Not used")
    override suspend fun unmarkFavorite(authorization: String, userId: String, itemId: String) = error("Not used")
    override suspend fun markPlayed(authorization: String, userId: String, itemId: String) = error("Not used")
    override suspend fun unmarkPlayed(authorization: String, userId: String, itemId: String) = error("Not used")
    override suspend fun updateUserData(authorization: String, userId: String, itemId: String, request: EmbyUserDataUpdateRequest) = error("Not used")
}
