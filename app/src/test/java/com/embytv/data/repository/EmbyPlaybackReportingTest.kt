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
import com.embytv.data.remote.dto.EmbyViewsResponse
import com.embytv.domain.model.EmbySession
import com.embytv.domain.model.PlaybackDetails
import com.embytv.domain.model.PlaybackSource
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EmbyPlaybackReportingTest {
    private val dispatcher = StandardTestDispatcher()
    private val api = ReportingFakeEmbyApi()
    private val repository = EmbyRepository(
        apiFactory = ReportingFakeEmbyApiProvider(api),
        streamUrlBuilder = EmbyStreamUrlBuilder(),
        ioDispatcher = dispatcher,
    )
    private val session = EmbySession(
        serverUrl = "http://emby.test/",
        userId = "user-1",
        accessToken = "token-value",
        serverId = "server-1",
    )
    private val source = PlaybackSource(
        itemId = "item-1",
        title = "测试媒体",
        streamUrl = "http://emby.test/Videos/item-1/stream",
        details = PlaybackDetails(
            playSessionId = "play-session",
            mediaSourceId = "media-source",
        ),
    )

    @Test
    fun reportsPlaybackStartProgressAndStopped() = runTest(dispatcher) {
        repository.reportPlaybackStarted(session, "device-1", source, positionMs = 1_234).getOrThrow()
        repository.reportPlaybackProgress(session, "device-1", source, positionMs = 2_000, isPaused = true).getOrThrow()
        repository.reportPlaybackStopped(session, "device-1", source, positionMs = 3_000).getOrThrow()

        assertEquals("item-1", api.started.single().itemId)
        assertEquals("media-source", api.started.single().mediaSourceId)
        assertEquals("play-session", api.started.single().playSessionId)
        assertEquals(12_340_000L, api.started.single().positionTicks)
        assertFalse(api.started.single().isPaused)
        assertTrue(api.started.single().canSeek)

        assertEquals("item-1", api.progress.single().itemId)
        assertEquals("media-source", api.progress.single().mediaSourceId)
        assertEquals("play-session", api.progress.single().playSessionId)
        assertEquals(20_000_000L, api.progress.single().positionTicks)
        assertTrue(api.progress.single().isPaused)
        assertFalse(api.progress.single().isMuted)
        assertEquals("DirectPlay", api.progress.single().playMethod)

        assertEquals("item-1", api.stopped.single().itemId)
        assertEquals("media-source", api.stopped.single().mediaSourceId)
        assertEquals("play-session", api.stopped.single().playSessionId)
        assertEquals(30_000_000L, api.stopped.single().positionTicks)
    }

    @Test
    fun convertsNegativePositionToZeroTicks() = runTest(dispatcher) {
        repository.reportPlaybackProgress(session, "device-1", source, positionMs = -10, isPaused = false).getOrThrow()

        assertEquals(0L, api.progress.single().positionTicks)
    }
}

private class ReportingFakeEmbyApiProvider(
    private val api: EmbyApi,
) : EmbyApiProvider {
    override fun create(baseUrl: String, accessToken: String?): EmbyApi = api
}

private class ReportingFakeEmbyApi : EmbyApi {
    val started = mutableListOf<EmbyPlaybackStartRequest>()
    val progress = mutableListOf<EmbyPlaybackProgressRequest>()
    val stopped = mutableListOf<EmbyPlaybackStoppedRequest>()

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
        started += request
    }

    override suspend fun reportPlaybackProgress(
        authorization: String,
        request: EmbyPlaybackProgressRequest,
    ) {
        progress += request
    }

    override suspend fun reportPlaybackStopped(
        authorization: String,
        request: EmbyPlaybackStoppedRequest,
    ) {
        stopped += request
    }
}
