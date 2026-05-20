package com.embytv.domain.model

data class EmbySession(
    val serverUrl: String,
    val userId: String,
    val accessToken: String,
    val serverId: String?,
)
