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
import org.junit.Test

class EmbyUserActionsTest {
    private val dispatcher = StandardTestDispatcher()
    private val api = UserActionsFakeEmbyApi()
    private val repository = EmbyRepository(
        apiFactory = UserActionsFakeEmbyApiProvider(api),
        streamUrlBuilder = EmbyStreamUrlBuilder(),
        ioDispatcher = dispatcher,
    )
    private val session = EmbySession("http://emby.test/", "user-1", "token-value", "server-1")

    @Test
    fun togglesFavoriteAndPlayedStateThroughEmbyPlaystateEndpoints() = runTest(dispatcher) {
        repository.toggleFavorite(session, "device-1", "item-1", favorite = true).getOrThrow()
        repository.toggleFavorite(session, "device-1", "item-1", favorite = false).getOrThrow()
        repository.markPlayed(session, "device-1", "item-1", played = true).getOrThrow()
        repository.markPlayed(session, "device-1", "item-1", played = false).getOrThrow()

        assertEquals(listOf("favorite:item-1", "unfavorite:item-1", "played:item-1", "unplayed:item-1"), api.actions)
    }

    @Test
    fun clearResumeProgressUpdatesPlaybackPositionToZero() = runTest(dispatcher) {
        repository.clearResumeProgress(session, "device-1", "item-1").getOrThrow()

        assertEquals("item-1", api.userDataUpdates.single().first)
        assertEquals(0L, api.userDataUpdates.single().second.playbackPositionTicks)
    }
}

private class UserActionsFakeEmbyApiProvider(private val api: EmbyApi) : EmbyApiProvider {
    override fun create(baseUrl: String, accessToken: String?): EmbyApi = api
}

private class UserActionsFakeEmbyApi : EmbyApi {
    val actions = mutableListOf<String>()
    val userDataUpdates = mutableListOf<Pair<String, EmbyUserDataUpdateRequest>>()

    override suspend fun markFavorite(authorization: String, userId: String, itemId: String) {
        actions += "favorite:$itemId"
    }

    override suspend fun unmarkFavorite(authorization: String, userId: String, itemId: String) {
        actions += "unfavorite:$itemId"
    }

    override suspend fun markPlayed(authorization: String, userId: String, itemId: String) {
        actions += "played:$itemId"
    }

    override suspend fun unmarkPlayed(authorization: String, userId: String, itemId: String) {
        actions += "unplayed:$itemId"
    }

    override suspend fun updateUserData(authorization: String, userId: String, itemId: String, request: EmbyUserDataUpdateRequest) {
        userDataUpdates += itemId to request
    }

    override suspend fun authenticateByName(authorization: String, request: EmbyAuthRequest): EmbyAuthResponse = error("Not used")
    override suspend fun getItems(authorization: String, userId: String, recursive: Boolean, includeItemTypes: String, fields: String, filters: String?, startIndex: Int, limit: Int?, sortBy: String?, sortOrder: String?, enableUserData: Boolean, searchTerm: String?, genreIds: String?, personIds: String?): EmbyItemsResponse = error("Not used")
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
}
