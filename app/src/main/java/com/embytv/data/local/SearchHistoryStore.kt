package com.embytv.data.local

// 管理搜索历史的本地持久化和纯规则，供搜索页展示最近查询并快速复搜。
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

private val Context.searchHistoryDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "search_history",
)

@Serializable
data class SearchHistoryItem(
    val query: String,
    val timestamp: Long,
    val resultCount: Int = 0,
)

class SearchHistoryStore(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true }
    private val listSerializer = ListSerializer(SearchHistoryItem.serializer())

    val historyFlow: Flow<List<SearchHistoryItem>> = context.searchHistoryDataStore.data
        .map { preferences ->
            decodeHistory(preferences[HISTORY_KEY])
        }

    suspend fun addHistory(query: String, resultCount: Int, timestamp: Long = System.currentTimeMillis()) {
        val normalizedQuery = query.trim()
        if (normalizedQuery.isBlank()) return
        context.searchHistoryDataStore.edit { preferences ->
            val currentList = decodeHistory(preferences[HISTORY_KEY])
            val updatedList = SearchHistoryRules.add(
                history = currentList,
                query = normalizedQuery,
                resultCount = resultCount,
                timestamp = timestamp,
            )
            preferences[HISTORY_KEY] = json.encodeToString(listSerializer, updatedList)
        }
    }

    suspend fun removeHistory(query: String) {
        val normalizedQuery = query.trim()
        if (normalizedQuery.isBlank()) return
        context.searchHistoryDataStore.edit { preferences ->
            val updatedList = decodeHistory(preferences[HISTORY_KEY])
                .filterNot { it.query.equals(normalizedQuery, ignoreCase = true) }
            preferences[HISTORY_KEY] = json.encodeToString(listSerializer, updatedList)
        }
    }

    suspend fun clearHistory() {
        context.searchHistoryDataStore.edit { preferences ->
            preferences[HISTORY_KEY] = "[]"
        }
    }

    private fun decodeHistory(value: String?): List<SearchHistoryItem> =
        if (value.isNullOrBlank()) {
            emptyList()
        } else {
            runCatching {
                json.decodeFromString(listSerializer, value)
                    .filter { it.query.isNotBlank() }
                    .sortedByDescending { it.timestamp }
                    .take(MAX_HISTORY_SIZE)
            }.getOrDefault(emptyList())
        }

    companion object {
        private val HISTORY_KEY = stringPreferencesKey("search_history_list")
        const val MAX_HISTORY_SIZE = 20
    }
}

internal object SearchHistoryRules {
    fun add(
        history: List<SearchHistoryItem>,
        query: String,
        resultCount: Int,
        timestamp: Long,
    ): List<SearchHistoryItem> {
        val normalizedQuery = query.trim()
        if (normalizedQuery.isBlank()) return history.take(SearchHistoryStore.MAX_HISTORY_SIZE)
        val item = SearchHistoryItem(
            query = normalizedQuery,
            timestamp = timestamp,
            resultCount = resultCount.coerceAtLeast(0),
        )
        return (listOf(item) + history.filterNot { it.query.equals(normalizedQuery, ignoreCase = true) })
            .take(SearchHistoryStore.MAX_HISTORY_SIZE)
    }
}
