package com.embytv.data.repository

import com.embytv.BuildConfig
import com.embytv.data.remote.EmbyApiProvider
import com.embytv.data.remote.dto.EmbyAuthRequest
import com.embytv.data.remote.dto.EmbyChapterInfoDto
import com.embytv.data.remote.dto.EmbyItemDto
import com.embytv.data.remote.dto.EmbyMediaSourceDto
import com.embytv.data.remote.dto.EmbyMediaStreamDto
import com.embytv.data.remote.dto.EmbyPlaybackProgressRequest
import com.embytv.data.remote.dto.EmbyPlaybackStartRequest
import com.embytv.data.remote.dto.EmbyPlaybackStoppedRequest
import com.embytv.data.remote.dto.EmbyUserDataUpdateRequest
import com.embytv.domain.model.DiscoveryEntryItems
import com.embytv.domain.model.DiscoveryEntrySummary
import com.embytv.domain.model.DiscoveryKind
import com.embytv.domain.model.EmbyDiscoveryContent
import com.embytv.domain.model.EmbyCredentialStore
import com.embytv.domain.model.EmbyFavoriteDashboard
import com.embytv.domain.model.EmbyHomeDashboard
import com.embytv.domain.model.EmbyLibraryContent
import com.embytv.domain.model.EmbyLibrarySummary
import com.embytv.domain.model.EmbyLibraryLatestSection
import com.embytv.domain.model.EmbyMediaDetail
import com.embytv.domain.model.EmbyPersonSummary
import com.embytv.domain.model.EmbySeasonEpisodes
import com.embytv.domain.model.EmbySeasonSummary
import com.embytv.domain.model.EmbySession
import com.embytv.domain.model.MediaItemSummary
import com.embytv.domain.model.NoOpEmbyCredentialStore
import com.embytv.domain.model.PlaybackDetails
import com.embytv.domain.model.PlaybackOverlayDetails
import com.embytv.domain.model.PlaybackQueue
import com.embytv.domain.model.PlaybackSource
import com.embytv.domain.model.PlaybackTrack
import com.embytv.domain.model.PlaybackVideoStream
import com.embytv.domain.model.SavedEmbyCredential
import com.embytv.domain.model.SeekThumbnail
import com.embytv.domain.model.ServerConfig
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class EmbyRepository(
    private val apiFactory: EmbyApiProvider,
    private val streamUrlBuilder: EmbyStreamUrlBuilder,
    private val credentialStore: EmbyCredentialStore = NoOpEmbyCredentialStore,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val clientVersion: String = BuildConfig.VERSION_NAME,
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

    suspend fun loadSavedCredentials(): Result<List<SavedEmbyCredential>> = withContext(ioDispatcher) {
        runCatching { credentialStore.loadAll().credentials }
    }

    suspend fun deleteSavedCredential(uniqueKey: String): Result<Unit> = withContext(ioDispatcher) {
        runCatching { credentialStore.delete(uniqueKey) }
    }

    suspend fun clearSavedCredential(): Result<Unit> = withContext(ioDispatcher) {
        runCatching { credentialStore.clear() }
    }

    fun buildImageAuthorizationHeader(session: EmbySession, deviceId: String): String =
        buildAuthorizationHeader(deviceId, session.accessToken)

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
                val dashboardParts = coroutineScope {
                    val countSemaphore = Semaphore(DASHBOARD_REQUEST_PARALLELISM)
                    val latestSemaphore = Semaphore(DASHBOARD_REQUEST_PARALLELISM)
                    val librariesDeferred = async {
                        views.mapNotNull { view ->
                            val id = view.id ?: return@mapNotNull null
                            async {
                                countSemaphore.withPermit {
                                    val count = runCatching {
                                        api.getItemsByParent(
                                            authorization = authorization,
                                            userId = session.userId,
                                            parentId = id,
                                        ).totalRecordCount
                                    }.getOrDefault(0)
                                    EmbyLibrarySummary(
                                        id = id,
                                        name = view.name.orEmpty(),
                                        type = view.type.orEmpty(),
                                        collectionType = view.collectionType,
                                        itemCount = count,
                                        imageUrl = streamUrlBuilder.buildPrimaryImageUrl(
                                            serverUrl = session.serverUrl,
                                            itemId = id,
                                            tag = view.primaryTag(),
                                            profile = EmbyImageProfile.Backdrop,
                                        ) ?: streamUrlBuilder.buildPrimaryImageUrl(
                                            serverUrl = session.serverUrl,
                                            itemId = id,
                                            tag = null,
                                            allowUntagged = true,
                                            profile = EmbyImageProfile.Backdrop,
                                        ),
                                    )
                                }
                            }
                        }.awaitAll()
                    }
                    val resumeDeferred = async {
                        runCatching {
                            api.getResumeItems(
                                authorization = authorization,
                                userId = session.userId,
                            ).items.mapNotNull { it.toMediaItemSummary(session.serverUrl) }
                        }.getOrDefault(emptyList())
                    }
                    val latestDeferred = async {
                        runCatching {
                            api.getLatestItems(
                                authorization = authorization,
                                userId = session.userId,
                            ).mapNotNull { it.toMediaItemSummary(session.serverUrl) }
                        }.getOrDefault(emptyList())
                    }
                    val nextUpDeferred = async {
                        runCatching {
                            api.getNextUp(
                                authorization = authorization,
                                userId = session.userId,
                                limit = NEXT_UP_LIMIT,
                            ).items.mapNotNull { it.toMediaItemSummary(session.serverUrl) }
                        }.getOrDefault(emptyList())
                    }
                    val libraries = librariesDeferred.await()
                    val libraryLatestSectionsDeferred = libraries.map { library ->
                        async {
                            latestSemaphore.withPermit {
                                val items = runCatching {
                                    loadLatestItemsForLibrary(
                                        api = api,
                                        authorization = authorization,
                                        userId = session.userId,
                                        serverUrl = session.serverUrl,
                                        library = library,
                                    )
                                }.getOrDefault(emptyList())
                                if (items.isEmpty()) {
                                    null
                                } else {
                                    EmbyLibraryLatestSection(library = library, items = items)
                                }
                            }
                        }
                    }
                    DashboardParts(
                        libraries = libraries,
                        resumeItems = resumeDeferred.await(),
                        latestItems = latestDeferred.await(),
                        nextUpItems = nextUpDeferred.await(),
                        libraryLatestSections = libraryLatestSectionsDeferred.awaitAll().filterNotNull(),
                    )
                }

                EmbyHomeDashboard(
                    libraries = dashboardParts.libraries,
                    resumeItems = dashboardParts.resumeItems,
                    latestItems = dashboardParts.latestItems,
                    nextUpItems = dashboardParts.nextUpItems,
                    libraryLatestSections = dashboardParts.libraryLatestSections,
                )
            }
        }

    suspend fun loadLibraryContent(
        session: EmbySession,
        deviceId: String,
        library: EmbyLibrarySummary,
        limit: Int = LIBRARY_CONTENT_LIMIT,
    ): Result<EmbyLibraryContent> = withContext(ioDispatcher) {
        runCatching {
            val api = apiFactory.create(session.serverUrl, session.accessToken)
            val items = api.getItemsByParent(
                authorization = buildAuthorizationHeader(deviceId, session.accessToken),
                userId = session.userId,
                parentId = library.id,
                includeItemTypes = library.libraryContentTypes(),
                startIndex = 0,
                limit = limit,
                sortBy = "SortName",
                sortOrder = "Ascending",
            ).items.mapNotNull { it.toMediaItemSummary(session.serverUrl) }
            EmbyLibraryContent(library = library, items = items)
        }
    }

    suspend fun loadFavoriteDashboard(session: EmbySession, deviceId: String): Result<EmbyFavoriteDashboard> =
        withContext(ioDispatcher) {
            runCatching {
                val api = apiFactory.create(session.serverUrl, session.accessToken)
                val items = api.getItems(
                    authorization = buildAuthorizationHeader(deviceId, session.accessToken),
                    userId = session.userId,
                    recursive = true,
                    includeItemTypes = "Movie,Series,Episode",
                    fields = com.embytv.data.remote.EmbyApi.MEDIA_ITEM_FIELDS,
                    filters = "IsFavorite",
                    startIndex = 0,
                    limit = FAVORITE_CONTENT_LIMIT,
                    sortBy = "DateCreated",
                    sortOrder = "Descending",
                    enableUserData = true,
                ).items
                EmbyFavoriteDashboard(
                    movies = items
                        .filter { it.type.equals("Movie", ignoreCase = true) }
                        .mapNotNull { it.toMediaItemSummary(session.serverUrl) },
                    series = items.toFavoriteSeriesSummaries(session.serverUrl),
                    totalCount = items.size,
                )
            }
        }

    suspend fun searchItems(
        session: EmbySession,
        deviceId: String,
        query: String,
        limit: Int = SEARCH_LIMIT,
    ): Result<com.embytv.domain.model.EmbySearchResults> = withContext(ioDispatcher) {
        runCatching {
            val normalizedQuery = query.trim()
            if (normalizedQuery.isBlank()) {
                return@runCatching com.embytv.domain.model.EmbySearchResults(query = normalizedQuery)
            }
            val api = apiFactory.create(session.serverUrl, session.accessToken)
            val items = api.getItems(
                authorization = buildAuthorizationHeader(deviceId, session.accessToken),
                userId = session.userId,
                recursive = true,
                includeItemTypes = "Movie,Series,Episode,BoxSet,Playlist",
                fields = com.embytv.data.remote.EmbyApi.MEDIA_ITEM_FIELDS,
                startIndex = 0,
                limit = limit,
                sortBy = "SortName",
                sortOrder = "Ascending",
                enableUserData = true,
                searchTerm = normalizedQuery,
            ).items.mapNotNull { it.toMediaItemSummary(session.serverUrl) }
            com.embytv.domain.model.EmbySearchResults(query = normalizedQuery, items = items)
        }
    }

    suspend fun loadDiscoveryContent(
        session: EmbySession,
        deviceId: String,
        kind: DiscoveryKind,
        limit: Int = DISCOVERY_LIMIT,
    ): Result<EmbyDiscoveryContent> = withContext(ioDispatcher) {
        runCatching {
            val api = apiFactory.create(session.serverUrl, session.accessToken)
            val authorization = buildAuthorizationHeader(deviceId, session.accessToken)
            val entries = when (kind) {
                DiscoveryKind.Collections -> api.getItems(
                    authorization = authorization,
                    userId = session.userId,
                    recursive = true,
                    includeItemTypes = "BoxSet",
                    fields = com.embytv.data.remote.EmbyApi.MEDIA_ITEM_FIELDS,
                    startIndex = 0,
                    limit = limit,
                    sortBy = "SortName",
                    sortOrder = "Ascending",
                    enableUserData = true,
                ).items.mapNotNull { it.toDiscoveryEntry(session.serverUrl, kind) }
                DiscoveryKind.Playlists -> api.getItems(
                    authorization = authorization,
                    userId = session.userId,
                    recursive = true,
                    includeItemTypes = "Playlist",
                    fields = com.embytv.data.remote.EmbyApi.MEDIA_ITEM_FIELDS,
                    startIndex = 0,
                    limit = limit,
                    sortBy = "SortName",
                    sortOrder = "Ascending",
                    enableUserData = true,
                ).items.mapNotNull { it.toDiscoveryEntry(session.serverUrl, kind) }
                DiscoveryKind.Genres -> api.getGenres(
                    authorization = authorization,
                    userId = session.userId,
                    limit = limit,
                ).items.mapNotNull { it.toDiscoveryEntry(session.serverUrl, kind) }
                DiscoveryKind.Persons -> api.getPersons(
                    authorization = authorization,
                    userId = session.userId,
                    limit = limit,
                ).items.mapNotNull { it.toDiscoveryEntry(session.serverUrl, kind) }
            }
            EmbyDiscoveryContent(kind = kind, entries = entries)
        }
    }

    suspend fun loadDiscoveryEntryItems(
        session: EmbySession,
        deviceId: String,
        entry: DiscoveryEntrySummary,
        limit: Int = LIBRARY_CONTENT_LIMIT,
    ): Result<DiscoveryEntryItems> = withContext(ioDispatcher) {
        runCatching {
            val api = apiFactory.create(session.serverUrl, session.accessToken)
            val authorization = buildAuthorizationHeader(deviceId, session.accessToken)
            val items = when (entry.kind) {
                DiscoveryKind.Collections -> api.getItemsByParent(
                    authorization = authorization,
                    userId = session.userId,
                    parentId = entry.id,
                    recursive = true,
                    includeItemTypes = "Movie,Series",
                    startIndex = 0,
                    limit = limit,
                    sortBy = "SortName",
                    sortOrder = "Ascending",
                    fields = com.embytv.data.remote.EmbyApi.MEDIA_ITEM_FIELDS,
                ).items
                DiscoveryKind.Playlists -> api.getPlaylistItems(
                    authorization = authorization,
                    playlistId = entry.id,
                    userId = session.userId,
                    limit = limit,
                ).items
                DiscoveryKind.Genres -> api.getItems(
                    authorization = authorization,
                    userId = session.userId,
                    recursive = true,
                    includeItemTypes = "Movie,Series",
                    fields = com.embytv.data.remote.EmbyApi.MEDIA_ITEM_FIELDS,
                    startIndex = 0,
                    limit = limit,
                    sortBy = "SortName",
                    sortOrder = "Ascending",
                    enableUserData = true,
                    genreIds = entry.id,
                ).items
                DiscoveryKind.Persons -> api.getItems(
                    authorization = authorization,
                    userId = session.userId,
                    recursive = true,
                    includeItemTypes = "Movie,Series",
                    fields = com.embytv.data.remote.EmbyApi.MEDIA_ITEM_FIELDS,
                    startIndex = 0,
                    limit = limit,
                    sortBy = "SortName",
                    sortOrder = "Ascending",
                    enableUserData = true,
                    personIds = entry.id,
                ).items
            }
            DiscoveryEntryItems(
                entry = entry,
                items = items.mapNotNull { it.toMediaItemSummary(session.serverUrl) },
            )
        }
    }

    suspend fun loadNextUp(
        session: EmbySession,
        deviceId: String,
        seriesId: String? = null,
    ): Result<List<MediaItemSummary>> = withContext(ioDispatcher) {
        runCatching {
            val api = apiFactory.create(session.serverUrl, session.accessToken)
            api.getNextUp(
                authorization = buildAuthorizationHeader(deviceId, session.accessToken),
                userId = session.userId,
                seriesId = seriesId,
                limit = NEXT_UP_LIMIT,
            ).items.mapNotNull { it.toMediaItemSummary(session.serverUrl) }
        }
    }

    suspend fun loadMediaDetail(
        session: EmbySession,
        deviceId: String,
        itemId: String,
    ): Result<EmbyMediaDetail> = withContext(ioDispatcher) {
        runCatching {
            val api = apiFactory.create(session.serverUrl, session.accessToken)
            val authorization = buildAuthorizationHeader(deviceId, session.accessToken)
            val item = api.getItem(
                authorization = authorization,
                userId = session.userId,
                itemId = itemId,
                fields = com.embytv.data.remote.EmbyApi.MEDIA_DETAIL_FIELDS,
            )
            val summary = requireNotNull(item.toMediaItemSummary(session.serverUrl)) {
                "Emby 未返回媒体详情"
            }
            val seasons = if (summary.type.equals("Series", ignoreCase = true)) {
                api.getSeasons(
                    authorization = authorization,
                    seriesId = summary.id,
                    userId = session.userId,
                    fields = com.embytv.data.remote.EmbyApi.SEASON_FIELDS,
                ).items.mapNotNull { it.toSeasonSummary(session.serverUrl) }
            } else {
                emptyList()
            }
            EmbyMediaDetail(
                item = summary,
                people = item.people.mapNotNull { person ->
                    val name = person.name?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                    EmbyPersonSummary(
                        id = person.id,
                        name = name,
                        role = person.role,
                        type = person.type,
                    )
                },
                genres = item.genres.filter { it.isNotBlank() },
                studios = item.studios.mapNotNull { it.name?.takeIf { name -> name.isNotBlank() } },
                communityRating = item.communityRating,
                officialRating = item.officialRating,
                premiereDate = item.premiereDate,
                criticRating = item.criticRating,
                providerIds = item.providerIds.filterProviderIds(),
                seasons = seasons,
            )
        }
    }

    suspend fun loadPlaybackOverlayDetails(
        session: EmbySession,
        deviceId: String,
        itemId: String,
    ): Result<PlaybackOverlayDetails> = withContext(ioDispatcher) {
        runCatching {
            val api = apiFactory.create(session.serverUrl, session.accessToken)
            val authorization = buildAuthorizationHeader(deviceId, session.accessToken)
            coroutineScope {
                val detailDeferred = async {
                    loadMediaDetail(session, deviceId, itemId).getOrThrow()
                }
                val playbackDeferred = async {
                    api.getPlaybackInfo(
                        authorization = authorization,
                        itemId = itemId,
                        userId = session.userId,
                    ).toPlaybackDetails(session)
                }
                PlaybackOverlayDetails(
                    mediaDetail = detailDeferred.await(),
                    playbackDetails = playbackDeferred.await(),
                )
            }
        }
    }

    suspend fun loadSeasonEpisodes(
        session: EmbySession,
        deviceId: String,
        seriesId: String,
        season: EmbySeasonSummary,
    ): Result<EmbySeasonEpisodes> = withContext(ioDispatcher) {
        runCatching {
            val api = apiFactory.create(session.serverUrl, session.accessToken)
            val episodes = api.getEpisodes(
                authorization = buildAuthorizationHeader(deviceId, session.accessToken),
                seriesId = seriesId,
                userId = session.userId,
                seasonId = season.id,
                fields = com.embytv.data.remote.EmbyApi.SEASON_EPISODE_FIELDS,
            ).items.mapNotNull { it.toMediaItemSummary(session.serverUrl) }
            EmbySeasonEpisodes(season = season, episodes = episodes)
        }
    }

    fun createPlaybackSource(
        session: EmbySession,
        item: MediaItemSummary,
        queue: PlaybackQueue? = null,
    ): PlaybackSource =
        PlaybackSource(
            itemId = item.id,
            title = item.name,
            streamUrl = streamUrlBuilder.buildVideoStreamUrl(
                serverUrl = session.serverUrl,
                itemId = item.id,
                accessToken = session.accessToken,
            ),
            playlistItemId = item.playlistItemId,
            session = session,
            queue = queue,
            previewThumbnailUrl = item.thumbImageUrl ?: item.backdropImageUrl ?: item.imageUrl,
            seekThumbnails = item.seekThumbnails,
            startPositionMs = item.playbackPositionTicks.toMilliseconds(),
            contextLabel = item.playbackContextLabel(),
        )

    suspend fun createPlaybackSourceWithDetails(
        session: EmbySession,
        deviceId: String,
        item: MediaItemSummary,
        queueItems: List<MediaItemSummary> = emptyList(),
    ): Result<PlaybackSource> = withContext(ioDispatcher) {
        runCatching {
            val api = apiFactory.create(session.serverUrl, session.accessToken)
            val authorization = buildAuthorizationHeader(deviceId, session.accessToken)
            val playbackInfo = api.getPlaybackInfo(
                authorization = authorization,
                itemId = item.id,
                userId = session.userId,
            )
            val queue = buildPlaybackQueue(
                api = api,
                authorization = authorization,
                session = session,
                item = item,
                queueItems = queueItems,
            )
            createPlaybackSource(session, item, queue).copy(
                deviceId = deviceId,
                details = playbackInfo.toPlaybackDetails(session),
            )
        }
    }

    suspend fun toggleFavorite(
        session: EmbySession,
        deviceId: String,
        itemId: String,
        favorite: Boolean,
    ): Result<Unit> = withContext(ioDispatcher) {
        runCatching {
            val api = apiFactory.create(session.serverUrl, session.accessToken)
            val authorization = buildAuthorizationHeader(deviceId, session.accessToken)
            if (favorite) {
                api.markFavorite(authorization = authorization, userId = session.userId, itemId = itemId)
            } else {
                api.unmarkFavorite(authorization = authorization, userId = session.userId, itemId = itemId)
            }
        }
    }

    suspend fun markPlayed(
        session: EmbySession,
        deviceId: String,
        itemId: String,
        played: Boolean,
    ): Result<Unit> = withContext(ioDispatcher) {
        runCatching {
            val api = apiFactory.create(session.serverUrl, session.accessToken)
            val authorization = buildAuthorizationHeader(deviceId, session.accessToken)
            if (played) {
                api.markPlayed(authorization = authorization, userId = session.userId, itemId = itemId)
            } else {
                api.unmarkPlayed(authorization = authorization, userId = session.userId, itemId = itemId)
            }
        }
    }

    suspend fun clearResumeProgress(
        session: EmbySession,
        deviceId: String,
        itemId: String,
    ): Result<Unit> = withContext(ioDispatcher) {
        runCatching {
            val api = apiFactory.create(session.serverUrl, session.accessToken)
            api.updateUserData(
                authorization = buildAuthorizationHeader(deviceId, session.accessToken),
                userId = session.userId,
                itemId = itemId,
                request = EmbyUserDataUpdateRequest(playbackPositionTicks = 0L),
            )
        }
    }

    suspend fun reportPlaybackStarted(
        session: EmbySession,
        deviceId: String,
        source: PlaybackSource,
        positionMs: Long,
    ): Result<Unit> = withContext(ioDispatcher) {
        runCatching {
            val api = apiFactory.create(session.serverUrl, session.accessToken)
            api.reportPlaybackStarted(
                authorization = buildAuthorizationHeader(deviceId, session.accessToken),
                request = EmbyPlaybackStartRequest(
                    itemId = source.itemId,
                    mediaSourceId = source.details.mediaSourceId,
                    playSessionId = source.details.playSessionId,
                    playlistItemId = source.playlistItemId,
                    positionTicks = positionMs.toEmbyTicks(),
                ),
            )
        }
    }

    suspend fun reportPlaybackProgress(
        session: EmbySession,
        deviceId: String,
        source: PlaybackSource,
        positionMs: Long,
        isPaused: Boolean,
    ): Result<Unit> = withContext(ioDispatcher) {
        runCatching {
            val api = apiFactory.create(session.serverUrl, session.accessToken)
            api.reportPlaybackProgress(
                authorization = buildAuthorizationHeader(deviceId, session.accessToken),
                request = EmbyPlaybackProgressRequest(
                    itemId = source.itemId,
                    mediaSourceId = source.details.mediaSourceId,
                    playSessionId = source.details.playSessionId,
                    playlistItemId = source.playlistItemId,
                    positionTicks = positionMs.toEmbyTicks(),
                    isPaused = isPaused,
                ),
            )
        }
    }

    suspend fun reportPlaybackStopped(
        session: EmbySession,
        deviceId: String,
        source: PlaybackSource,
        positionMs: Long,
    ): Result<Unit> = withContext(ioDispatcher) {
        runCatching {
            val api = apiFactory.create(session.serverUrl, session.accessToken)
            api.reportPlaybackStopped(
                authorization = buildAuthorizationHeader(deviceId, session.accessToken),
                request = EmbyPlaybackStoppedRequest(
                    itemId = source.itemId,
                    mediaSourceId = source.details.mediaSourceId,
                    playSessionId = source.details.playSessionId,
                    playlistItemId = source.playlistItemId,
                    positionTicks = positionMs.toEmbyTicks(),
                ),
            )
        }
    }

    private fun buildAuthorizationHeader(deviceId: String, accessToken: String? = null): String {
        return buildString {
            append("MediaBrowser Client=\"EmbyTv\", Device=\"Android TV\", DeviceId=\"$deviceId\", Version=\"$clientVersion\"")
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
            imageUrl = primaryImageUrl(serverUrl),
            thumbImageUrl = thumbImageUrl(serverUrl),
            backdropImageUrl = backdropImageUrl(serverUrl),
            seriesName = seriesName,
            seasonName = seasonName,
            parentIndexNumber = parentIndexNumber,
            indexNumber = indexNumber,
            parentId = parentId,
            runTimeTicks = runTimeTicks,
            playbackPositionTicks = userData?.playbackPositionTicks ?: 0L,
            playedPercentage = userData?.playedPercentage,
            productionYear = productionYear,
            seriesId = seriesId,
            unplayedItemCount = userData?.unplayedItemCount,
            childCount = childCount,
            recursiveItemCount = recursiveItemCount,
            dateCreated = dateCreated,
            isFavorite = userData?.isFavorite ?: false,
            played = userData?.played ?: false,
            playCount = userData?.playCount,
            playlistItemId = playlistItemId,
            seekThumbnails = chapters.toSeekThumbnails(serverUrl, id),
        )
    }

    private fun EmbyItemDto.toDiscoveryEntry(serverUrl: String, kind: DiscoveryKind): DiscoveryEntrySummary? {
        val id = id ?: return null
        return DiscoveryEntrySummary(
            id = id,
            name = name.orEmpty().ifBlank { id },
            type = type.orEmpty(),
            kind = kind,
            imageUrl = primaryImageUrl(serverUrl) ?: thumbImageUrl(serverUrl) ?: backdropImageUrl(serverUrl),
            itemCount = childCount ?: recursiveItemCount,
        )
    }

    private fun EmbyItemDto.toSeasonSummary(serverUrl: String): EmbySeasonSummary? {
        val id = id ?: return null
        val count = userData?.unplayedItemCount
        return EmbySeasonSummary(
            id = id,
            name = name.orEmpty().ifBlank {
                indexNumber?.let { "第 $it 季" } ?: "Season"
            },
            indexNumber = indexNumber,
                imageUrl = streamUrlBuilder.buildPrimaryImageUrl(
                    serverUrl = serverUrl,
                    itemId = id,
                    tag = primaryTag(),
                    profile = EmbyImageProfile.Poster,
                ) ?: streamUrlBuilder.buildPrimaryImageUrl(
                    serverUrl = serverUrl,
                    itemId = id,
                    tag = null,
                    allowUntagged = true,
                    profile = EmbyImageProfile.Poster,
                ),
            episodeCount = childCount,
            unplayedItemCount = count?.takeIf { it > 0 },
        )
    }

    private suspend fun loadLatestItemsForLibrary(
        api: com.embytv.data.remote.EmbyApi,
        authorization: String,
        userId: String,
        serverUrl: String,
        library: EmbyLibrarySummary,
    ): List<MediaItemSummary> {
        val raw = api.getLatestItems(
            authorization = authorization,
            userId = userId,
            parentId = library.id,
            includeItemTypes = library.latestItemTypes(),
            groupItems = library.latestGroupItems(),
            limit = LIBRARY_LATEST_LIMIT,
        )
        return if (library.isTvShows()) {
            raw.toSeriesSummaries(serverUrl).ifEmpty {
                raw.mapNotNull { it.toMediaItemSummary(serverUrl) }
            }
        } else {
            raw.mapNotNull { it.toMediaItemSummary(serverUrl) }
        }
    }

    private fun List<EmbyItemDto>.toSeriesSummaries(serverUrl: String): List<MediaItemSummary> {
        val episodes = filter { item ->
            item.type.equals("Episode", ignoreCase = true) &&
                (!item.seriesId.isNullOrBlank() || !item.seriesName.isNullOrBlank())
        }
        if (episodes.isEmpty()) return emptyList()
        return episodes
            .groupBy { item -> item.seriesId?.takeIf { it.isNotBlank() } ?: item.seriesName.orEmpty() }
            .values
            .mapNotNull { group ->
                val seed = group.firstOrNull() ?: return@mapNotNull null
                val seriesId = seed.seriesId?.takeIf { it.isNotBlank() } ?: seed.id ?: return@mapNotNull null
                val seriesName = seed.seriesName?.takeIf { it.isNotBlank() } ?: seed.name.orEmpty()
                MediaItemSummary(
                    id = seriesId,
                    name = seriesName,
                    type = "Series",
                    overview = seed.overview,
                    imageUrl = streamUrlBuilder.buildPrimaryImageUrl(
                        serverUrl = serverUrl,
                        itemId = seriesId,
                        tag = seed.seriesPrimaryImageTag,
                        allowUntagged = true,
                        profile = EmbyImageProfile.Poster,
                    ),
                    thumbImageUrl = seed.thumbImageUrl(serverUrl),
                    backdropImageUrl = seed.backdropImageUrl(serverUrl),
                    seriesId = seriesId,
                    seriesName = seriesName,
                    runTimeTicks = null,
                    playbackPositionTicks = 0L,
                    playedPercentage = null,
                    productionYear = seed.productionYear,
                    unplayedItemCount = group.mapNotNull { it.userData?.unplayedItemCount }.maxOrNull(),
                    childCount = group.mapNotNull { it.childCount }.maxOrNull(),
                    recursiveItemCount = group.mapNotNull { it.recursiveItemCount }.maxOrNull(),
                    dateCreated = group.mapNotNull { it.dateCreated }.maxOrNull(),
                )
            }
    }

    private fun List<EmbyItemDto>.toFavoriteSeriesSummaries(serverUrl: String): List<MediaItemSummary> {
        val directSeries = filter { item -> item.type.equals("Series", ignoreCase = true) }
            .mapNotNull { item -> item.toMediaItemSummary(serverUrl)?.withNonBlankTitle() }
        val directSeriesIds = directSeries.map { it.id }.toSet()
        val episodeSeries = filter { item ->
            item.type.equals("Episode", ignoreCase = true) &&
                (!item.seriesId.isNullOrBlank() || !item.seriesName.isNullOrBlank())
        }
            .groupBy { item -> item.seriesId?.takeIf { it.isNotBlank() } ?: item.seriesName.orEmpty() }
            .values
            .mapNotNull { group ->
                val seed = group.firstOrNull() ?: return@mapNotNull null
                val seriesItemId = seed.seriesId?.takeIf { it.isNotBlank() }
                val seriesId = seriesItemId ?: seed.seriesName?.takeIf { it.isNotBlank() }
                    ?: return@mapNotNull null
                if (seriesId in directSeriesIds) return@mapNotNull null
                val seriesName = seed.seriesName?.takeIf { it.isNotBlank() } ?: seriesId
                MediaItemSummary(
                    id = seriesId,
                    name = seriesName,
                    type = "Series",
                    overview = seed.overview,
                    imageUrl = seriesItemId?.let { itemId ->
                        streamUrlBuilder.buildPrimaryImageUrl(
                            serverUrl = serverUrl,
                            itemId = itemId,
                            tag = seed.seriesPrimaryImageTag,
                            allowUntagged = true,
                            profile = EmbyImageProfile.Poster,
                        )
                    } ?: seed.primaryImageUrl(serverUrl),
                    thumbImageUrl = seed.thumbImageUrl(serverUrl),
                    backdropImageUrl = seed.backdropImageUrl(serverUrl),
                    seriesId = seriesId,
                    seriesName = seriesName,
                    runTimeTicks = null,
                    playbackPositionTicks = 0L,
                    playedPercentage = null,
                    productionYear = seed.productionYear,
                    unplayedItemCount = group.mapNotNull { it.userData?.unplayedItemCount }.maxOrNull(),
                    childCount = group.mapNotNull { it.childCount }.maxOrNull(),
                    recursiveItemCount = group.mapNotNull { it.recursiveItemCount }.maxOrNull(),
                    dateCreated = group.mapNotNull { it.dateCreated }.maxOrNull(),
                )
            }
        return directSeries + episodeSeries
    }

    private fun MediaItemSummary.withNonBlankTitle(): MediaItemSummary =
        if (name.isNotBlank()) {
            this
        } else {
            copy(name = seriesName?.takeIf { it.isNotBlank() } ?: id)
        }

    private fun EmbyItemDto.primaryImageUrl(serverUrl: String): String? {
        val id = id ?: return null
        return streamUrlBuilder.buildPrimaryImageUrl(
            serverUrl = serverUrl,
            itemId = id,
            tag = primaryTag(),
            profile = EmbyImageProfile.Poster,
        ) ?: seriesPrimaryImageUrl(serverUrl)
            ?: streamUrlBuilder.buildPrimaryImageUrl(
                serverUrl = serverUrl,
                itemId = id,
                tag = null,
                allowUntagged = true,
                profile = EmbyImageProfile.Poster,
            )
    }

    private fun EmbyItemDto.seriesPrimaryImageUrl(serverUrl: String): String? {
        val itemId = seriesId?.takeIf { it.isNotBlank() } ?: return null
        return streamUrlBuilder.buildPrimaryImageUrl(
            serverUrl = serverUrl,
            itemId = itemId,
            tag = seriesPrimaryImageTag,
            allowUntagged = !seriesPrimaryImageTag.isNullOrBlank(),
            profile = EmbyImageProfile.Poster,
        )
    }

    private fun EmbyItemDto.thumbImageUrl(serverUrl: String): String? {
        val id = id ?: return null
        return streamUrlBuilder.buildThumbImageUrl(
            serverUrl = serverUrl,
            itemId = id,
            tag = imageTags?.get("Thumb"),
            profile = EmbyImageProfile.Thumb,
        ) ?: parentThumbItemId?.takeIf { it.isNotBlank() }?.let { parentId ->
            streamUrlBuilder.buildThumbImageUrl(
                serverUrl = serverUrl,
                itemId = parentId,
                tag = parentThumbImageTag,
                allowUntagged = !parentThumbImageTag.isNullOrBlank(),
                profile = EmbyImageProfile.Thumb,
            )
        } ?: streamUrlBuilder.buildThumbImageUrl(
            serverUrl = serverUrl,
            itemId = id,
            tag = null,
            allowUntagged = true,
            profile = EmbyImageProfile.Thumb,
        )
    }

    private fun EmbyItemDto.backdropImageUrl(serverUrl: String): String? {
        val id = id ?: return null
        return streamUrlBuilder.buildBackdropImageUrl(
            serverUrl = serverUrl,
            itemId = id,
            tag = backdropImageTags.firstOrNull(),
            profile = EmbyImageProfile.Backdrop,
        ) ?: parentBackdropItemId?.takeIf { it.isNotBlank() }?.let { parentId ->
            streamUrlBuilder.buildBackdropImageUrl(
                serverUrl = serverUrl,
                itemId = parentId,
                tag = parentBackdropImageTags.firstOrNull(),
                allowUntagged = parentBackdropImageTags.isNotEmpty(),
                profile = EmbyImageProfile.Backdrop,
            )
        } ?: streamUrlBuilder.buildBackdropImageUrl(
            serverUrl = serverUrl,
            itemId = id,
            tag = null,
            allowUntagged = true,
            profile = EmbyImageProfile.Backdrop,
        )
    }

    private fun EmbyItemDto.primaryTag(): String? =
        imageTags?.get("Primary") ?: primaryImageTag

    private fun List<EmbyChapterInfoDto>.toSeekThumbnails(serverUrl: String, itemId: String): List<SeekThumbnail> =
        mapIndexedNotNull { index, chapter ->
            val imageUrl = streamUrlBuilder.buildChapterImageUrl(
                serverUrl = serverUrl,
                itemId = itemId,
                chapterIndex = chapter.chapterIndex ?: index,
                tag = chapter.imageTag,
                profile = EmbyImageProfile.Thumb,
            ) ?: return@mapIndexedNotNull null
            SeekThumbnail(
                positionMs = chapter.startPositionTicks.toMilliseconds(),
                imageUrl = imageUrl,
            )
        }.sortedBy { it.positionMs }

    private fun Map<String, String>.filterProviderIds(): Map<String, String> =
        filter { (key, value) -> key.isNotBlank() && value.isNotBlank() }

    private fun Long?.toMilliseconds(): Long =
        this?.takeIf { it > 0L }?.div(10_000L) ?: 0L

    private fun EmbyLibrarySummary.isTvShows(): Boolean =
        collectionType.equals("tvshows", ignoreCase = true)

    private fun EmbyLibrarySummary.isMovies(): Boolean =
        collectionType.equals("movies", ignoreCase = true)

    private fun EmbyLibrarySummary.latestItemTypes(): String = when {
        isTvShows() -> "Episode"
        isMovies() -> "Movie"
        else -> "Movie,Series"
    }

    private fun EmbyLibrarySummary.latestGroupItems(): Boolean? =
        if (isTvShows()) true else null

    private fun EmbyLibrarySummary.libraryContentTypes(): String = when {
        isTvShows() -> "Series"
        isMovies() -> "Movie"
        else -> "Movie,Series"
    }

    private fun MediaItemSummary.playbackContextLabel(): String? {
        val episodeLabel = if (parentIndexNumber != null && indexNumber != null) {
            "S%02dE%02d".format(parentIndexNumber, indexNumber)
        } else {
            null
        }
        return when {
            !seriesName.isNullOrBlank() && episodeLabel != null -> "$seriesName · $episodeLabel"
            !seasonName.isNullOrBlank() && episodeLabel != null -> "$seasonName · $episodeLabel"
            productionYear != null -> productionYear.toString()
            else -> null
        }
    }

    private fun EmbyMediaSourceDto.videoStream(): EmbyMediaStreamDto? =
        mediaStreams.firstOrNull { it.type.equals("Video", ignoreCase = true) }

    private fun EmbyMediaSourceDto.audioStreams(): List<EmbyMediaStreamDto> =
        mediaStreams.filter { it.type.equals("Audio", ignoreCase = true) }

    private fun EmbyMediaSourceDto.subtitleStreams(): List<EmbyMediaStreamDto> =
        mediaStreams.filter { it.type.equals("Subtitle", ignoreCase = true) }

    private fun com.embytv.data.remote.dto.EmbyPlaybackInfoResponse.toPlaybackDetails(session: EmbySession): PlaybackDetails {
        val source = mediaSources.firstOrNull()
        return PlaybackDetails(
            playSessionId = playSessionId,
            mediaSourceId = source?.id,
            container = source?.container,
            bitrate = source?.bitrate,
            video = source?.videoStream()?.toPlaybackVideoStream(),
            audioTracks = source?.audioStreams().orEmpty().map { it.toPlaybackTrack(session) },
            subtitleTracks = source?.subtitleStreams().orEmpty().map { it.toPlaybackTrack(session) },
        )
    }

    private fun EmbyMediaStreamDto.toPlaybackVideoStream(): PlaybackVideoStream =
        PlaybackVideoStream(
            codec = codec,
            width = width,
            height = height,
            videoRange = videoRange,
        )

    private fun EmbyMediaStreamDto.toPlaybackTrack(session: EmbySession): PlaybackTrack =
        PlaybackTrack(
            index = index ?: -1,
            codec = codec,
            displayTitle = displayTitle,
            channels = channels,
            language = language,
            isDefault = isDefault,
            isForced = isForced,
            isExternal = isExternal,
            deliveryMethod = deliveryMethod,
            externalUrl = if (isExternal) {
                streamUrlBuilder.buildSubtitleDeliveryUrl(
                    serverUrl = session.serverUrl,
                    deliveryUrl = deliveryUrl,
                    accessToken = session.accessToken,
                )
            } else {
                null
            },
        )

    private suspend fun loadPlaybackQueueItemsForItem(
        api: com.embytv.data.remote.EmbyApi,
        authorization: String,
        session: EmbySession,
        item: MediaItemSummary,
    ): List<MediaItemSummary>? {
        if (!item.type.equals("Episode", ignoreCase = true)) return null
        val seriesId = item.seriesId?.takeIf { it.isNotBlank() } ?: return null
        val seasonId = item.parentId?.takeIf { it.isNotBlank() } ?: return null
        return runCatching {
            api.getEpisodes(
                authorization = authorization,
                seriesId = seriesId,
                userId = session.userId,
                seasonId = seasonId,
                fields = com.embytv.data.remote.EmbyApi.SEASON_EPISODE_FIELDS,
            ).items.mapNotNull { it.toMediaItemSummary(session.serverUrl) }
        }.getOrNull()
    }

    private suspend fun buildPlaybackQueue(
        api: com.embytv.data.remote.EmbyApi,
        authorization: String,
        session: EmbySession,
        item: MediaItemSummary,
        queueItems: List<MediaItemSummary>,
    ): PlaybackQueue? {
        val baseQueue = PlaybackQueue.from(queueItems, item.id)
            ?: loadPlaybackQueueItemsForItem(
                api = api,
                authorization = authorization,
                session = session,
                item = item,
            )?.sortedByPlaybackOrder()
                ?.let { PlaybackQueue.from(it, item.id) }
        if (baseQueue?.next != null || !item.type.equals("Episode", ignoreCase = true)) {
            return baseQueue
        }
        val nextUp = loadNextUpFallbackForItem(
            api = api,
            authorization = authorization,
            session = session,
            item = item,
        ) ?: return baseQueue
        return baseQueue?.copy(next = nextUp) ?: PlaybackQueue(current = item, next = nextUp)
    }

    private suspend fun loadNextUpFallbackForItem(
        api: com.embytv.data.remote.EmbyApi,
        authorization: String,
        session: EmbySession,
        item: MediaItemSummary,
    ): MediaItemSummary? {
        val seriesId = item.seriesId?.takeIf { it.isNotBlank() } ?: return null
        return runCatching {
            api.getNextUp(
                authorization = authorization,
                userId = session.userId,
                seriesId = seriesId,
                limit = NEXT_UP_LIMIT,
            ).items.mapNotNull { it.toMediaItemSummary(session.serverUrl) }
                .firstOrNull { it.id != item.id }
        }.getOrNull()
    }

    private fun List<MediaItemSummary>.sortedByPlaybackOrder(): List<MediaItemSummary> =
        sortedWith(
            compareBy<MediaItemSummary>(
                { it.parentIndexNumber ?: Int.MAX_VALUE },
                { it.indexNumber ?: Int.MAX_VALUE },
                { it.name },
                { it.id },
            ),
        )

    private companion object {
        const val SEARCH_LIMIT = 60
        const val DISCOVERY_LIMIT = 60
        const val NEXT_UP_LIMIT = 12
        const val LIBRARY_LATEST_LIMIT = 8
        const val LIBRARY_CONTENT_LIMIT = 60
        const val FAVORITE_CONTENT_LIMIT = 60
        const val DASHBOARD_REQUEST_PARALLELISM = 4
    }
}

private data class DashboardParts(
    val libraries: List<EmbyLibrarySummary>,
    val resumeItems: List<MediaItemSummary>,
    val latestItems: List<MediaItemSummary>,
    val nextUpItems: List<MediaItemSummary>,
    val libraryLatestSections: List<EmbyLibraryLatestSection>,
)

internal fun Long.toEmbyTicks(): Long {
    val positionMs = coerceAtLeast(0L)
    val maxSafePositionMs = Long.MAX_VALUE / EMBY_TICKS_PER_MILLISECOND
    return if (positionMs > maxSafePositionMs) {
        Long.MAX_VALUE
    } else {
        positionMs * EMBY_TICKS_PER_MILLISECOND
    }
}

private const val EMBY_TICKS_PER_MILLISECOND = 10_000L
