package com.embytv.domain.model

interface EmbyCredentialStore {
    suspend fun save(credential: SavedEmbyCredential)
    suspend fun load(): SavedEmbyCredential?
    suspend fun clear()
}

object NoOpEmbyCredentialStore : EmbyCredentialStore {
    override suspend fun save(credential: SavedEmbyCredential) = Unit
    override suspend fun load(): SavedEmbyCredential? = null
    override suspend fun clear() = Unit
}
