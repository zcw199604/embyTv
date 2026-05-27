package com.embytv.data.local

import android.content.Context
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.embytv.domain.model.EmbyCredentialStore
import com.embytv.domain.model.SavedEmbyCredential
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Suppress("DEPRECATION")
class EncryptedEmbyCredentialStore(
    context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : EmbyCredentialStore {
    private val appContext = context.applicationContext

    override suspend fun save(credential: SavedEmbyCredential) = withContext(ioDispatcher) {
        preferences().edit {
            putString(KEY_SERVER_URL, credential.serverUrl)
            putString(KEY_USER_ID, credential.userId)
            putString(KEY_USERNAME, credential.username)
            putString(KEY_ACCESS_TOKEN, credential.accessToken)
            putString(KEY_SERVER_ID, credential.serverId)
            putString(KEY_DEVICE_ID, credential.deviceId)
            putLong(KEY_SAVED_AT, credential.savedAtEpochMillis)
        }
    }

    override suspend fun load(): SavedEmbyCredential? = withContext(ioDispatcher) {
        val prefs = preferences()
        val serverUrl = prefs.getString(KEY_SERVER_URL, null) ?: return@withContext null
        val userId = prefs.getString(KEY_USER_ID, null) ?: return@withContext null
        val username = prefs.getString(KEY_USERNAME, null) ?: return@withContext null
        val accessToken = prefs.getString(KEY_ACCESS_TOKEN, null) ?: return@withContext null
        val deviceId = prefs.getString(KEY_DEVICE_ID, null) ?: return@withContext null
        SavedEmbyCredential(
            serverUrl = serverUrl,
            userId = userId,
            username = username,
            accessToken = accessToken,
            serverId = prefs.getString(KEY_SERVER_ID, null),
            deviceId = deviceId,
            savedAtEpochMillis = prefs.getLong(KEY_SAVED_AT, 0L),
        )
    }

    override suspend fun clear() = withContext(ioDispatcher) {
        preferences().edit { clear() }
    }

    private fun preferences() = EncryptedSharedPreferences.create(
        appContext,
        FILE_NAME,
        MasterKey.Builder(appContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    private companion object {
        const val FILE_NAME = "emby_credentials"
        const val KEY_SERVER_URL = "server_url"
        const val KEY_USER_ID = "user_id"
        const val KEY_USERNAME = "username"
        const val KEY_ACCESS_TOKEN = "access_token"
        const val KEY_SERVER_ID = "server_id"
        const val KEY_DEVICE_ID = "device_id"
        const val KEY_SAVED_AT = "saved_at"
    }
}
