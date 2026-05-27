package com.embytv.domain.model

data class SavedEmbyCredential(
    val serverUrl: String,
    val userId: String,
    val username: String,
    val accessToken: String,
    val serverId: String?,
    val deviceId: String,
    val savedAtEpochMillis: Long,
)
