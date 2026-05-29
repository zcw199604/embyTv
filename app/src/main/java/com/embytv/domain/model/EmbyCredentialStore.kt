package com.embytv.domain.model

interface EmbyCredentialStore {
    suspend fun save(credential: SavedEmbyCredential)
    suspend fun load(): SavedEmbyCredential?
    suspend fun clear()

    suspend fun saveAll(credentials: SavedEmbyCredentialList) {
        credentials.credentials.firstOrNull()?.let { save(it) } ?: clear()
    }

    suspend fun loadAll(): SavedEmbyCredentialList =
        SavedEmbyCredentialList(load()?.let { listOf(it) }.orEmpty())

    suspend fun delete(uniqueKey: String) {
        val remaining = loadAll().remove(uniqueKey)
        saveAll(remaining)
    }
}

object NoOpEmbyCredentialStore : EmbyCredentialStore {
    override suspend fun save(credential: SavedEmbyCredential) = Unit
    override suspend fun load(): SavedEmbyCredential? = null
    override suspend fun clear() = Unit
}
