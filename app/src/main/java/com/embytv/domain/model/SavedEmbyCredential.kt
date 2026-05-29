package com.embytv.domain.model

data class SavedEmbyCredential(
    val serverUrl: String,
    val userId: String,
    val username: String,
    val accessToken: String,
    val serverId: String?,
    val deviceId: String,
    val savedAtEpochMillis: Long,
) {
    val uniqueKey: String
        get() = "${serverUrl.trimEnd('/')}|$userId"

    val displayLabel: String
        get() = "$username · ${serverUrl.trimEnd('/')}"
}

data class SavedEmbyCredentialList(
    val credentials: List<SavedEmbyCredential> = emptyList(),
) {
    fun upsert(credential: SavedEmbyCredential): SavedEmbyCredentialList =
        copy(
            credentials = (credentials.filterNot { it.uniqueKey == credential.uniqueKey } + credential)
                .sortedByDescending { it.savedAtEpochMillis },
        )

    fun remove(uniqueKey: String): SavedEmbyCredentialList =
        copy(credentials = credentials.filterNot { it.uniqueKey == uniqueKey })
}
