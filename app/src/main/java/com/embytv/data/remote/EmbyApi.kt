package com.embytv.data.remote

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
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface EmbyApi {
    @POST("Users/AuthenticateByName")
    suspend fun authenticateByName(
        @Header("X-Emby-Authorization") authorization: String,
        @Body request: EmbyAuthRequest,
    ): EmbyAuthResponse

    @GET("Users/{userId}/Items")
    suspend fun getItems(
        @Header("X-Emby-Authorization") authorization: String,
        @Path("userId") userId: String,
        @Query("Recursive") recursive: Boolean = true,
        @Query("IncludeItemTypes") includeItemTypes: String = "Movie,Episode",
        @Query("Fields") fields: String = "Overview,PrimaryImageAspectRatio,ImageTags",
        @Query("Filters") filters: String? = null,
        @Query("StartIndex") startIndex: Int = 0,
        @Query("Limit") limit: Int? = null,
        @Query("SortBy") sortBy: String? = null,
        @Query("SortOrder") sortOrder: String? = null,
        @Query("EnableUserData") enableUserData: Boolean = true,
        @Query("SearchTerm") searchTerm: String? = null,
        @Query("GenreIds") genreIds: String? = null,
        @Query("PersonIds") personIds: String? = null,
    ): EmbyItemsResponse

    @GET("Users/{userId}/Items/{itemId}")
    suspend fun getItem(
        @Header("X-Emby-Authorization") authorization: String,
        @Path("userId") userId: String,
        @Path("itemId") itemId: String,
        @Query("Fields") fields: String = MEDIA_DETAIL_FIELDS,
    ): EmbyItemDto

    @GET("Users/{userId}/Views")
    suspend fun getViews(
        @Header("X-Emby-Authorization") authorization: String,
        @Path("userId") userId: String,
    ): EmbyViewsResponse

    @GET("Users/{userId}/Items")
    suspend fun getItemsByParent(
        @Header("X-Emby-Authorization") authorization: String,
        @Path("userId") userId: String,
        @Query("ParentId") parentId: String,
        @Query("Recursive") recursive: Boolean = true,
        @Query("IncludeItemTypes") includeItemTypes: String = "Movie,Episode",
        @Query("StartIndex") startIndex: Int = 0,
        @Query("Limit") limit: Int = 0,
        @Query("SortBy") sortBy: String? = null,
        @Query("SortOrder") sortOrder: String? = null,
        @Query("Fields") fields: String = MEDIA_ITEM_FIELDS,
    ): EmbyItemsResponse

    @GET("Users/{userId}/Items/Resume")
    suspend fun getResumeItems(
        @Header("X-Emby-Authorization") authorization: String,
        @Path("userId") userId: String,
        @Query("Recursive") recursive: Boolean = true,
        @Query("MediaTypes") mediaTypes: String = "Video",
        @Query("Fields") fields: String = MEDIA_ITEM_FIELDS,
        @Query("Limit") limit: Int = 24,
    ): EmbyItemsResponse

    @GET("Users/{userId}/Items/Latest")
    suspend fun getLatestItems(
        @Header("X-Emby-Authorization") authorization: String,
        @Path("userId") userId: String,
        @Query("ParentId") parentId: String? = null,
        @Query("IncludeItemTypes") includeItemTypes: String = "Movie,Episode",
        @Query("GroupItems") groupItems: Boolean? = null,
        @Query("Fields") fields: String = MEDIA_ITEM_FIELDS,
        @Query("Limit") limit: Int = 24,
    ): List<com.embytv.data.remote.dto.EmbyItemDto>

    @GET("Shows/NextUp")
    suspend fun getNextUp(
        @Header("X-Emby-Authorization") authorization: String,
        @Query("UserId") userId: String,
        @Query("Fields") fields: String = MEDIA_ITEM_FIELDS,
        @Query("Limit") limit: Int = 12,
        @Query("SeriesId") seriesId: String? = null,
    ): EmbyItemsResponse

    @GET("Genres")
    suspend fun getGenres(
        @Header("X-Emby-Authorization") authorization: String,
        @Query("UserId") userId: String,
        @Query("Recursive") recursive: Boolean = true,
        @Query("Fields") fields: String = MEDIA_ITEM_FIELDS,
        @Query("StartIndex") startIndex: Int = 0,
        @Query("Limit") limit: Int = 60,
        @Query("SortBy") sortBy: String = "SortName",
        @Query("SortOrder") sortOrder: String = "Ascending",
        @Query("EnableUserData") enableUserData: Boolean = true,
    ): EmbyItemsResponse

    @GET("Persons")
    suspend fun getPersons(
        @Header("X-Emby-Authorization") authorization: String,
        @Query("UserId") userId: String,
        @Query("Recursive") recursive: Boolean = true,
        @Query("Fields") fields: String = MEDIA_ITEM_FIELDS,
        @Query("StartIndex") startIndex: Int = 0,
        @Query("Limit") limit: Int = 60,
        @Query("SortBy") sortBy: String = "SortName",
        @Query("SortOrder") sortOrder: String = "Ascending",
        @Query("EnableUserData") enableUserData: Boolean = true,
    ): EmbyItemsResponse

    @GET("Playlists/{playlistId}/Items")
    suspend fun getPlaylistItems(
        @Header("X-Emby-Authorization") authorization: String,
        @Path("playlistId") playlistId: String,
        @Query("UserId") userId: String,
        @Query("Fields") fields: String = MEDIA_ITEM_FIELDS,
        @Query("StartIndex") startIndex: Int = 0,
        @Query("Limit") limit: Int = 60,
    ): EmbyItemsResponse

    @GET("Shows/{seriesId}/Seasons")
    suspend fun getSeasons(
        @Header("X-Emby-Authorization") authorization: String,
        @Path("seriesId") seriesId: String,
        @Query("UserId") userId: String,
        @Query("Fields") fields: String = SEASON_FIELDS,
    ): EmbyItemsResponse

    @GET("Shows/{seriesId}/Episodes")
    suspend fun getEpisodes(
        @Header("X-Emby-Authorization") authorization: String,
        @Path("seriesId") seriesId: String,
        @Query("UserId") userId: String,
        @Query("SeasonId") seasonId: String,
        @Query("Fields") fields: String = SEASON_EPISODE_FIELDS,
    ): EmbyItemsResponse

    @GET("Items/{itemId}/PlaybackInfo")
    suspend fun getPlaybackInfo(
        @Header("X-Emby-Authorization") authorization: String,
        @Path("itemId") itemId: String,
        @Query("UserId") userId: String,
    ): EmbyPlaybackInfoResponse

    @POST("Sessions/Playing")
    suspend fun reportPlaybackStarted(
        @Header("X-Emby-Authorization") authorization: String,
        @Body request: EmbyPlaybackStartRequest,
    )

    @POST("Sessions/Playing/Progress")
    suspend fun reportPlaybackProgress(
        @Header("X-Emby-Authorization") authorization: String,
        @Body request: EmbyPlaybackProgressRequest,
    )

    @POST("Sessions/Playing/Stopped")
    suspend fun reportPlaybackStopped(
        @Header("X-Emby-Authorization") authorization: String,
        @Body request: EmbyPlaybackStoppedRequest,
    )

    @POST("Users/{userId}/FavoriteItems/{itemId}")
    suspend fun markFavorite(
        @Header("X-Emby-Authorization") authorization: String,
        @Path("userId") userId: String,
        @Path("itemId") itemId: String,
    )

    @DELETE("Users/{userId}/FavoriteItems/{itemId}")
    suspend fun unmarkFavorite(
        @Header("X-Emby-Authorization") authorization: String,
        @Path("userId") userId: String,
        @Path("itemId") itemId: String,
    )

    @POST("Users/{userId}/PlayedItems/{itemId}")
    suspend fun markPlayed(
        @Header("X-Emby-Authorization") authorization: String,
        @Path("userId") userId: String,
        @Path("itemId") itemId: String,
    )

    @DELETE("Users/{userId}/PlayedItems/{itemId}")
    suspend fun unmarkPlayed(
        @Header("X-Emby-Authorization") authorization: String,
        @Path("userId") userId: String,
        @Path("itemId") itemId: String,
    )

    @POST("Users/{userId}/Items/{itemId}/UserData")
    suspend fun updateUserData(
        @Header("X-Emby-Authorization") authorization: String,
        @Path("userId") userId: String,
        @Path("itemId") itemId: String,
        @Body request: EmbyUserDataUpdateRequest,
    )

    companion object {
        const val MEDIA_ITEM_FIELDS =
            "Overview,PrimaryImageAspectRatio,PrimaryImageTag,ImageTags,BackdropImageTags,ParentThumbItemId,ParentThumbImageTag,ParentBackdropItemId,ParentBackdropImageTags,UserData,RunTimeTicks,MediaSources,Genres,ProductionYear,CommunityRating,CriticRating,OfficialRating,DateCreated,PremiereDate,ParentId,SeriesId,SeriesName,SeriesPrimaryImageTag,SeasonName,IndexNumber,ParentIndexNumber,RecursiveItemCount,ChildCount,PlaylistItemId"
        const val MEDIA_DETAIL_FIELDS =
            "Overview,People,Genres,Studios,PrimaryImageAspectRatio,PrimaryImageTag,ImageTags,BackdropImageTags,UserData,RunTimeTicks,ProductionYear,CommunityRating,CriticRating,OfficialRating,PremiereDate,RecursiveItemCount,ChildCount"
        const val SEASON_FIELDS =
            "Overview,PrimaryImageTag,ImageTags,BackdropImageTags,UserData,IndexNumber,ChildCount"
        const val SEASON_EPISODE_FIELDS =
            "Overview,PrimaryImageTag,ImageTags,BackdropImageTags,ParentThumbItemId,ParentThumbImageTag,ParentBackdropItemId,ParentBackdropImageTags,ParentIndexNumber,IndexNumber,UserData,RunTimeTicks,SeriesId,SeriesName,SeasonName"
    }
}
