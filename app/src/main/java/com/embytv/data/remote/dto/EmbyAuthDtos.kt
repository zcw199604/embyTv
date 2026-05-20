package com.embytv.data.remote.dto

import com.google.gson.annotations.SerializedName

data class EmbyAuthRequest(
    @SerializedName("Username") val username: String,
    @SerializedName("Pw") val password: String,
)

data class EmbyAuthResponse(
    @SerializedName("AccessToken") val accessToken: String?,
    @SerializedName("ServerId") val serverId: String?,
    @SerializedName("User") val user: EmbyUserDto?,
)

data class EmbyUserDto(
    @SerializedName("Id") val id: String?,
    @SerializedName("Name") val name: String?,
)
