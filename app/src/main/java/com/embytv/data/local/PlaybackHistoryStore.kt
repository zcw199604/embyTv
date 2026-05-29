package com.embytv.data.local

// 管理最近播放记录的本地持久化和纯规则，供后续继续观看入口复用。
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

private val Context.playbackHistoryDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "playback_history",
)

@Serializable
data class PlaybackHistoryItem(
    val mediaId: String,
    val mediaTitle: String,
    val positionMs: Long,
    val durationMs: Long,
    val timestamp: Long,
    val thumbnailUrl: String? = null,
)

class PlaybackHistoryStore(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true }
    private val listSerializer = ListSerializer(PlaybackHistoryItem.serializer())

    val historyFlow: Flow<List<PlaybackHistoryItem>> = context.playbackHistoryDataStore.data
        .map { preferences ->
            decodeHistory(preferences[HISTORY_KEY])
        }

    suspend fun addHistory(item: PlaybackHistoryItem) {
        if (item.mediaId.isBlank()) return
        context.playbackHistoryDataStore.edit { preferences ->
            preferences[HISTORY_KEY] = json.encodeToString(
                listSerializer,
                PlaybackHistoryRules.add(decodeHistory(preferences[HISTORY_KEY]), item),
            )
        }
    }

    suspend fun removeHistory(mediaId: String) {
        context.playbackHistoryDataStore.edit { preferences ->
            val updated = decodeHistory(preferences[HISTORY_KEY]).filterNot { it.mediaId == mediaId }
            preferences[HISTORY_KEY] = json.encodeToString(listSerializer, updated)
        }
    }

    suspend fun clearHistory() {
        context.playbackHistoryDataStore.edit { preferences ->
            preferences[HISTORY_KEY] = "[]"
        }
    }

    private fun decodeHistory(raw: String?): List<PlaybackHistoryItem> =
        raw?.let {
            runCatching { json.decodeFromString(listSerializer, it) }.getOrDefault(emptyList())
        } ?: emptyList()

    companion object {
        const val MAX_HISTORY_SIZE = 50
        private val HISTORY_KEY = stringPreferencesKey("playback_history_list")
    }
}

object PlaybackHistoryRules {
    fun add(
        history: List<PlaybackHistoryItem>,
        item: PlaybackHistoryItem,
        maxSize: Int = PlaybackHistoryStore.MAX_HISTORY_SIZE,
    ): List<PlaybackHistoryItem> {
        if (item.mediaId.isBlank()) return history
        return (listOf(item) + history.filterNot { it.mediaId == item.mediaId }).take(maxSize)
    }
}
