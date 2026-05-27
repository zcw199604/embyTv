package com.embytv.data.repository

import com.embytv.data.remote.EmbyApiProvider
import com.embytv.data.remote.dto.EmbyAuthRequest
import com.embytv.data.remote.dto.EmbyItemDto
import com.embytv.data.remote.dto.EmbyMediaSourceDto
import com.embytv.data.remote.dto.EmbyMediaStreamDto
import com.embytv.domain.model.EmbyCredentialStore
import com.embytv.domain.model.EmbyHomeDashboard
import com.embytv.domain.model.EmbyLibrarySummary
import com.embytv.domain.model.EmbyLibraryLatestSection
import com.embytv.domain.model.EmbySession
import com.embytv.domain.model.MediaItemSummary
import com.embytv.domain.model.NoOpEmbyCredentialStore
import com.embytv.domain.model.PlaybackDetails
import com.embytv.domain.model.PlaybackSource
import com.embytv.domain.model.PlaybackTrack
import com.embytv.domain.model.PlaybackVideoStream
import com.embytv.domain.model.SavedEmbyCredential
import com.embytv.domain.model.ServerConfig
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class EmbyRepository(
    private val apiFactory: EmbyApiProvider,
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
                ).items.mapNotNull { it.toMediaItemSummary(session.serverUrl) }
            }
        }

    suspend fun loadHomeDashboard(session: EmbySession, deviceId: String): Result<EmbyHomeDashboard> =
        withContext(ioDispatcher) {
            runCatching {
                val api = apiFactory.create(session.serverUrl, session.accessToken)
                val authorization = buildAuthorizationHeader(deviceId, session.accessToken)
                val views = api.getViews(
                    authorization = authorization,
                    userId = session.userId,
                ).items
                val libraries = views.mapNotNull { view ->
                    val id = view.id ?: return@mapNotNull null
                    val count = api.getItemsByParent(
                        authorization = authorization,
                        userId = session.userId,
                        parentId = id,
                    ).totalRecordCount
                    EmbyLibrarySummary(
                        id = id,
                        name = view.name.orEmpty(),
                        type = view.type.orEmpty(),
                        collectionType = view.collectionType,
                        itemCount = count,
                        imageUrl = streamUrlBuilder.buildPrimaryImageUrl(
                            serverUrl = session.serverUrl,
                            itemId = id,
                            tag = view.imageTags?.get("Primary"),
                        ),
                    )
                }
                val resume = api.getResumeItems(
                    authorization = authorization,
                    userId = session.userId,
                ).items.mapNotNull { it.toMediaItemSummary(session.serverUrl) }
                val latest = api.getLatestItems(
                    authorization = authorization,
                    userId = session.userId,
                ).mapNotNull { it.toMediaItemSummary(session.serverUrl) }
                val libraryLatestSections = libraries.mapNotNull { library ->
                    val items = api.getItemsByParent(
                        authorization = authorization,
                        userId = session.userId,
                        parentId = library.id,
                        limit = LIBRARY_LATEST_LIMIT,
                        sortBy = "DateCreated",
                        sortOrder = "Descending",
                    ).items.mapNotNull { it.toMediaItemSummary(session.serverUrl) }
                    if (items.isEmpty()) {
                        null
                    } else {
                        EmbyLibraryLatestSection(library = library, items = items)
                    }
                }

                EmbyHomeDashboard(
                    libraries = libraries,
                    resumeItems = resume,
                    latestItems = latest,
                    libraryLatestSections = libraryLatestSections,
                )
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

    suspend fun createPlaybackSourceWithDetails(
        session: EmbySession,
        deviceId: String,
        item: MediaItemSummary,
    ): Result<PlaybackSource> = withContext(ioDispatcher) {
        runCatching {
            val api = apiFactory.create(session.serverUrl, session.accessToken)
            val playbackInfo = api.getPlaybackInfo(
                authorization = buildAuthorizationHeader(deviceId, session.accessToken),
                itemId = item.id,
                userId = session.userId,
            )
            val source = playbackInfo.mediaSources.firstOrNull()
            createPlaybackSource(session, item).copy(
                details = PlaybackDetails(
                    playSessionId = playbackInfo.playSessionId,
                    mediaSourceId = source?.id,
                    container = source?.container,
                    bitrate = source?.bitrate,
                    video = source?.videoStream()?.toPlaybackVideoStream(),
                    audioTracks = source?.audioStreams().orEmpty().map { it.toPlaybackTrack() },
                    subtitleTracks = source?.subtitleStreams().orEmpty().map { it.toPlaybackTrack() },
                ),
            )
        }
    }

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

    private fun EmbyItemDto.toMediaItemSummary(serverUrl: String): MediaItemSummary? {
        val id = id ?: return null
        return MediaItemSummary(
            id = id,
            name = name.orEmpty(),
            type = type.orEmpty(),
            overview = overview,
            imageUrl = streamUrlBuilder.buildPrimaryImageUrl(
                serverUrl = serverUrl,
                itemId = id,
                tag = imageTags?.get("Primary"),
            ),
            thumbImageUrl = streamUrlBuilder.buildThumbImageUrl(
                serverUrl = serverUrl,
                itemId = id,
                tag = imageTags?.get("Thumb"),
            ),
            backdropImageUrl = streamUrlBuilder.buildBackdropImageUrl(
                serverUrl = serverUrl,
                itemId = id,
                tag = backdropImageTags.firstOrNull(),
            ),
            seriesName = seriesName,
            seasonName = seasonName,
            parentIndexNumber = parentIndexNumber,
            indexNumber = indexNumber,
            parentId = parentId,
            runTimeTicks = runTimeTicks,
            playbackPositionTicks = userData?.playbackPositionTicks ?: 0L,
            playedPercentage = userData?.playedPercentage,
            productionYear = productionYear,
        )
    }

    private fun EmbyMediaSourceDto.videoStream(): EmbyMediaStreamDto? =
        mediaStreams.firstOrNull { it.type.equals("Video", ignoreCase = true) }

    private fun EmbyMediaSourceDto.audioStreams(): List<EmbyMediaStreamDto> =
        mediaStreams.filter { it.type.equals("Audio", ignoreCase = true) }

    private fun EmbyMediaSourceDto.subtitleStreams(): List<EmbyMediaStreamDto> =
        mediaStreams.filter { it.type.equals("Subtitle", ignoreCase = true) }

    private fun EmbyMediaStreamDto.toPlaybackVideoStream(): PlaybackVideoStream =
        PlaybackVideoStream(
            codec = codec,
            width = width,
            height = height,
            videoRange = videoRange,
        )

    private fun EmbyMediaStreamDto.toPlaybackTrack(): PlaybackTrack =
        PlaybackTrack(
            index = index ?: -1,
            codec = codec,
            displayTitle = displayTitle,
            channels = channels,
            language = language,
            isDefault = isDefault,
            isForced = isForced,
        )

    private companion object {
        const val LIBRARY_LATEST_LIMIT = 8
    }
}
