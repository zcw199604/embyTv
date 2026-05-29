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
import com.embytv.domain.model.DiscoveryEntrySummary
import com.embytv.domain.model.DiscoveryKind
import com.embytv.domain.model.EmbySession
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class EmbyRepositoryDiscoveryTest {
    private val dispatcher = StandardTestDispatcher()
    private val api = DiscoveryFakeEmbyApi()
    private val repository = EmbyRepository(
        apiFactory = DiscoveryFakeEmbyApiProvider(api),
        streamUrlBuilder = EmbyStreamUrlBuilder(),
        ioDispatcher = dispatcher,
    )
    private val session = EmbySession("http://emby.test/", "user-1", "token-value", "server-1")

    @Test
    fun loadDiscoveryContentMapsCollectionsPlaylistsGenresAndPersons() = runTest(dispatcher) {
        api.collections = listOf(entry("collection-1", "合集", "BoxSet"))
        api.playlists = listOf(entry("playlist-1", "播放列表", "Playlist"))
        api.genres = listOf(entry("genre-1", "动作", "Genre"))
        api.persons = listOf(entry("person-1", "演员", "Person"))

        val collections = repository.loadDiscoveryContent(session, "device-1", DiscoveryKind.Collections).getOrThrow()
        val playlists = repository.loadDiscoveryContent(session, "device-1", DiscoveryKind.Playlists).getOrThrow()
        val genres = repository.loadDiscoveryContent(session, "device-1", DiscoveryKind.Genres).getOrThrow()
        val persons = repository.loadDiscoveryContent(session, "device-1", DiscoveryKind.Persons).getOrThrow()

        assertEquals("合集", collections.entries.single().name)
        assertEquals(DiscoveryKind.Collections, collections.entries.single().kind)
        assertEquals("播放列表", playlists.entries.single().name)
        assertEquals("动作", genres.entries.single().name)
        assertEquals("演员", persons.entries.single().name)
        assertEquals("http://emby.test/Items/person-1/Images/Primary?tag=person-1-primary&MaxWidth=500&MaxHeight=750&Quality=85", persons.entries.single().imageUrl)
    }

    @Test
    fun loadDiscoveryEntryItemsUsesKindSpecificQuery() = runTest(dispatcher) {
        val collection = discoveryEntry("collection-1", DiscoveryKind.Collections)
        val playlist = discoveryEntry("playlist-1", DiscoveryKind.Playlists)
        val genre = discoveryEntry("genre-1", DiscoveryKind.Genres)
        val person = discoveryEntry("person-1", DiscoveryKind.Persons)

        repository.loadDiscoveryEntryItems(session, "device-1", collection).getOrThrow()
        repository.loadDiscoveryEntryItems(session, "device-1", playlist).getOrThrow()
        repository.loadDiscoveryEntryItems(session, "device-1", genre).getOrThrow()
        repository.loadDiscoveryEntryItems(session, "device-1", person).getOrThrow()

        assertEquals("collection-1", api.parentRequests.single())
        assertEquals("playlist-1", api.playlistRequests.single())
        assertEquals("genre-1", api.itemRequests.single { it.genreIds != null }.genreIds)
        assertEquals("person-1", api.itemRequests.single { it.personIds != null }.personIds)
    }

    private fun entry(id: String, name: String, type: String): EmbyItemDto =
        EmbyItemDto(
            id = id,
            name = name,
            type = type,
            overview = null,
            imageTags = mapOf("Primary" to "$id-primary"),
            childCount = 3,
        )

    private fun discoveryEntry(id: String, kind: DiscoveryKind): DiscoveryEntrySummary =
        DiscoveryEntrySummary(
            id = id,
            name = id,
            type = kind.name,
            kind = kind,
            imageUrl = null,
        )
}

private class DiscoveryFakeEmbyApiProvider(private val api: EmbyApi) : EmbyApiProvider {
    override fun create(baseUrl: String, accessToken: String?): EmbyApi = api
}

private data class DiscoveryItemsRequest(
    val includeItemTypes: String,
    val genreIds: String?,
    val personIds: String?,
)

private class DiscoveryFakeEmbyApi : EmbyApi {
    var collections: List<EmbyItemDto> = emptyList()
    var playlists: List<EmbyItemDto> = emptyList()
    var genres: List<EmbyItemDto> = emptyList()
    var persons: List<EmbyItemDto> = emptyList()
    val itemRequests = mutableListOf<DiscoveryItemsRequest>()
    val parentRequests = mutableListOf<String>()
    val playlistRequests = mutableListOf<String>()

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
        itemRequests += DiscoveryItemsRequest(includeItemTypes, genreIds, personIds)
        val items = when (includeItemTypes) {
            "BoxSet" -> collections
            "Playlist" -> playlists
            else -> listOf(entryItem("item-${itemRequests.size}"))
        }
        return EmbyItemsResponse(items = items, totalRecordCount = items.size)
    }

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
        parentRequests += parentId
        return EmbyItemsResponse(items = listOf(entryItem("parent-item")), totalRecordCount = 1)
    }

    override suspend fun getPlaylistItems(
        authorization: String,
        playlistId: String,
        userId: String,
        fields: String,
        startIndex: Int,
        limit: Int,
    ): EmbyItemsResponse {
        playlistRequests += playlistId
        return EmbyItemsResponse(items = listOf(entryItem("playlist-item")), totalRecordCount = 1)
    }

    override suspend fun getGenres(
        authorization: String,
        userId: String,
        recursive: Boolean,
        fields: String,
        startIndex: Int,
        limit: Int,
        sortBy: String,
        sortOrder: String,
        enableUserData: Boolean,
    ): EmbyItemsResponse = EmbyItemsResponse(items = genres, totalRecordCount = genres.size)

    override suspend fun getPersons(
        authorization: String,
        userId: String,
        recursive: Boolean,
        fields: String,
        startIndex: Int,
        limit: Int,
        sortBy: String,
        sortOrder: String,
        enableUserData: Boolean,
    ): EmbyItemsResponse = EmbyItemsResponse(items = persons, totalRecordCount = persons.size)

    private fun entryItem(id: String): EmbyItemDto =
        EmbyItemDto(id = id, name = id, type = "Movie", overview = null, imageTags = null)

    override suspend fun authenticateByName(authorization: String, request: EmbyAuthRequest): EmbyAuthResponse = error("Not used")
    override suspend fun getItem(authorization: String, userId: String, itemId: String, fields: String): EmbyItemDto = error("Not used")
    override suspend fun getViews(authorization: String, userId: String): EmbyViewsResponse = error("Not used")
    override suspend fun getResumeItems(authorization: String, userId: String, recursive: Boolean, mediaTypes: String, fields: String, limit: Int): EmbyItemsResponse = error("Not used")
    override suspend fun getLatestItems(authorization: String, userId: String, parentId: String?, includeItemTypes: String, groupItems: Boolean?, fields: String, limit: Int): List<EmbyItemDto> = error("Not used")
    override suspend fun getNextUp(authorization: String, userId: String, fields: String, limit: Int, seriesId: String?): EmbyItemsResponse = error("Not used")
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
