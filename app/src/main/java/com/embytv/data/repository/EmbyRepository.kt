package com.embytv.data.repository

import com.embytv.data.remote.EmbyApiFactory
import com.embytv.data.remote.dto.EmbyAuthRequest
import com.embytv.domain.model.EmbyCredentialStore
import com.embytv.domain.model.EmbySession
import com.embytv.domain.model.MediaItemSummary
import com.embytv.domain.model.NoOpEmbyCredentialStore
import com.embytv.domain.model.PlaybackSource
import com.embytv.domain.model.SavedEmbyCredential
import com.embytv.domain.model.ServerConfig
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class EmbyRepository(
    private val apiFactory: EmbyApiFactory,
    private val streamUrlBuilder: EmbyStreamUrlBuilder,
    private val credentialStore: EmbyCredentialStore = NoOpEmbyCredentialStore,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    @OptIn(ExperimentalTime::class)
    suspend fun authenticate(config: ServerConfig): Result<EmbySession> = withContext(ioDispatcher) {
        runCatching {
            val api = apiFactory.create(config.baseUrl)
            val response = api.authenticateByName(
                authorization = buildAuthorizationHeader(config.deviceId),
                request = EmbyAuthRequest(
                    username = config.username,
                    password = config.password,
                ),
            )
            val userId = requireNotNull(response.user?.id) { "Emby 未返回用户 ID" }
            val token = requireNotNull(response.accessToken) { "Emby 未返回访问令牌" }
            val session = EmbySession(
                serverUrl = config.baseUrl,
                userId = userId,
                accessToken = token,
                serverId = response.serverId,
            )
            credentialStore.save(
                SavedEmbyCredential(
                    serverUrl = session.serverUrl,
                    userId = session.userId,
                    username = config.username,
                    accessToken = session.accessToken,
                    serverId = session.serverId,
                    deviceId = config.deviceId,
                    savedAtEpochMillis = Clock.System.now().toEpochMilliseconds(),
                ),
            )
            session
        }
    }

    suspend fun loadSavedCredential(): Result<SavedEmbyCredential?> = withContext(ioDispatcher) {
        runCatching { credentialStore.load() }
    }

    suspend fun clearSavedCredential(): Result<Unit> = withContext(ioDispatcher) {
        runCatching { credentialStore.clear() }
    }

    suspend fun loadMediaItems(session: EmbySession, deviceId: String): Result<List<MediaItemSummary>> =
        withContext(ioDispatcher) {
            runCatching {
                val api = apiFactory.create(session.serverUrl, session.accessToken)
                api.getItems(
                    authorization = buildAuthorizationHeader(deviceId, session.accessToken),
                    userId = session.userId,
                ).items.mapNotNull { item ->
                    val id = item.id ?: return@mapNotNull null
                    MediaItemSummary(
                        id = id,
                        name = item.name.orEmpty(),
                        type = item.type.orEmpty(),
                        overview = item.overview,
                        imageUrl = streamUrlBuilder.buildPrimaryImageUrl(
                            serverUrl = session.serverUrl,
                            itemId = id,
                            tag = item.imageTags?.get("Primary"),
                        ),
                    )
                }
            }
        }

    fun createPlaybackSource(session: EmbySession, item: MediaItemSummary): PlaybackSource =
        PlaybackSource(
            itemId = item.id,
            title = item.name,
            streamUrl = streamUrlBuilder.buildVideoStreamUrl(
                serverUrl = session.serverUrl,
                itemId = item.id,
                accessToken = session.accessToken,
            ),
        )

    private fun buildAuthorizationHeader(deviceId: String, accessToken: String? = null): String {
        return buildString {
            append("MediaBrowser Client=\"EmbyTv\", Device=\"Android TV\", DeviceId=\"$deviceId\", Version=\"0.1.0\"")
            if (!accessToken.isNullOrBlank()) {
                append(", Token=\"")
                append(accessToken)
                append("\"")
            }
        }
    }
}
