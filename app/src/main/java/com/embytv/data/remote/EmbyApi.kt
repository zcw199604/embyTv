package com.embytv.data.remote

import com.embytv.data.remote.dto.EmbyAuthRequest
import com.embytv.data.remote.dto.EmbyAuthResponse
import com.embytv.data.remote.dto.EmbyItemsResponse
import retrofit2.http.Body
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
    ): EmbyItemsResponse
}
