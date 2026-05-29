package com.embytv.data.local

import android.content.Context
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.embytv.domain.model.EmbyCredentialStore
import com.embytv.domain.model.SavedEmbyCredential
import com.embytv.domain.model.SavedEmbyCredentialList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

@Suppress("DEPRECATION")
class EncryptedEmbyCredentialStore(
    context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : EmbyCredentialStore {
    private val appContext = context.applicationContext

    override suspend fun save(credential: SavedEmbyCredential) = withContext(ioDispatcher) {
        val current = loadAllInternal().upsert(credential)
        saveAllInternal(current)
    }

    override suspend fun load(): SavedEmbyCredential? = withContext(ioDispatcher) {
        loadAllInternal().credentials.firstOrNull()
    }

    override suspend fun loadAll(): SavedEmbyCredentialList = withContext(ioDispatcher) {
        loadAllInternal()
    }

    override suspend fun saveAll(credentials: SavedEmbyCredentialList) = withContext(ioDispatcher) {
        saveAllInternal(credentials)
    }

    override suspend fun delete(uniqueKey: String) = withContext(ioDispatcher) {
        saveAllInternal(loadAllInternal().remove(uniqueKey))
    }

    override suspend fun clear() = withContext(ioDispatcher) {
        preferences().edit { clear() }
    }

    private fun loadAllInternal(): SavedEmbyCredentialList {
        val prefs = preferences()
        prefs.getString(KEY_CREDENTIALS_JSON, null)?.let { raw ->
            runCatching { return parseCredentialList(raw) }
        }
        val legacy = loadLegacyCredential() ?: return SavedEmbyCredentialList()
        val migrated = SavedEmbyCredentialList(listOf(legacy))
        saveAllInternal(migrated)
        return migrated
    }

    private fun saveAllInternal(credentials: SavedEmbyCredentialList) {
        val normalized = SavedEmbyCredentialList(
            credentials.credentials.fold(SavedEmbyCredentialList()) { acc, credential ->
                acc.upsert(credential)
            }.credentials,
        )
        preferences().edit {
            putString(KEY_CREDENTIALS_JSON, normalized.toJson())
            if (normalized.credentials.isEmpty()) {
                remove(KEY_SERVER_URL)
                remove(KEY_USER_ID)
                remove(KEY_USERNAME)
                remove(KEY_ACCESS_TOKEN)
                remove(KEY_SERVER_ID)
                remove(KEY_DEVICE_ID)
                remove(KEY_SAVED_AT)
            } else {
                val latest = normalized.credentials.first()
                putString(KEY_SERVER_URL, latest.serverUrl)
                putString(KEY_USER_ID, latest.userId)
                putString(KEY_USERNAME, latest.username)
                putString(KEY_ACCESS_TOKEN, latest.accessToken)
                putString(KEY_SERVER_ID, latest.serverId)
                putString(KEY_DEVICE_ID, latest.deviceId)
                putLong(KEY_SAVED_AT, latest.savedAtEpochMillis)
            }
        }
    }

    private fun loadLegacyCredential(): SavedEmbyCredential? {
        val prefs = preferences()
        val serverUrl = prefs.getString(KEY_SERVER_URL, null) ?: return null
        val userId = prefs.getString(KEY_USER_ID, null) ?: return null
        val username = prefs.getString(KEY_USERNAME, null) ?: return null
        val accessToken = prefs.getString(KEY_ACCESS_TOKEN, null) ?: return null
        val deviceId = prefs.getString(KEY_DEVICE_ID, null) ?: return null
        return SavedEmbyCredential(
            serverUrl = serverUrl,
            userId = userId,
            username = username,
            accessToken = accessToken,
            serverId = prefs.getString(KEY_SERVER_ID, null),
            deviceId = deviceId,
            savedAtEpochMillis = prefs.getLong(KEY_SAVED_AT, 0L),
        )
    }

    private fun parseCredentialList(raw: String): SavedEmbyCredentialList {
        val array = JSONArray(raw)
        return SavedEmbyCredentialList(
            credentials = List(array.length()) { index ->
                val json = array.getJSONObject(index)
                SavedEmbyCredential(
                    serverUrl = json.getString("serverUrl"),
                    userId = json.getString("userId"),
                    username = json.getString("username"),
                    accessToken = json.getString("accessToken"),
                    serverId = json.optString("serverId").takeIf { it.isNotBlank() },
                    deviceId = json.getString("deviceId"),
                    savedAtEpochMillis = json.optLong("savedAtEpochMillis", 0L),
                )
            },
        )
    }

    private fun SavedEmbyCredentialList.toJson(): String {
        val array = JSONArray()
        credentials.forEach { credential ->
            array.put(
                JSONObject()
                    .put("serverUrl", credential.serverUrl)
                    .put("userId", credential.userId)
                    .put("username", credential.username)
                    .put("accessToken", credential.accessToken)
                    .put("serverId", credential.serverId)
                    .put("deviceId", credential.deviceId)
                    .put("savedAtEpochMillis", credential.savedAtEpochMillis),
            )
        }
        return array.toString()
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
        const val KEY_CREDENTIALS_JSON = "credentials_json"
        const val KEY_SERVER_URL = "server_url"
        const val KEY_USER_ID = "user_id"
        const val KEY_USERNAME = "username"
        const val KEY_ACCESS_TOKEN = "access_token"
        const val KEY_SERVER_ID = "server_id"
        const val KEY_DEVICE_ID = "device_id"
        const val KEY_SAVED_AT = "saved_at"
    }
}
