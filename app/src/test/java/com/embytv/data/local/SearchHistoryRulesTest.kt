package com.embytv.data.local

// 覆盖搜索历史纯规则，避免 DataStore 环境差异影响去重和数量上限验证。
import org.junit.Assert.assertEquals
import org.junit.Test

class SearchHistoryRulesTest {
    @Test
    fun addMovesDuplicateQueryToFront() {
        val history = listOf(
            SearchHistoryItem(query = "matrix", timestamp = 1L, resultCount = 2),
            SearchHistoryItem(query = "alien", timestamp = 2L, resultCount = 3),
        )

        val updated = SearchHistoryRules.add(history, "Matrix", 8, timestamp = 3L)

        assertEquals(2, updated.size)
        assertEquals("Matrix", updated.first().query)
        assertEquals(8, updated.first().resultCount)
        assertEquals("alien", updated.last().query)
    }

    @Test
    fun addLimitsHistorySize() {
        val history = (0 until 25).map { index ->
            SearchHistoryItem(query = "query$index", timestamp = index.toLong(), resultCount = index)
        }

        val updated = SearchHistoryRules.add(history, "latest", 1, timestamp = 30L)

        assertEquals(SearchHistoryStore.MAX_HISTORY_SIZE, updated.size)
        assertEquals("latest", updated.first().query)
    }

    @Test
    fun blankQueryDoesNotMutateHistory() {
        val history = listOf(SearchHistoryItem(query = "avatar", timestamp = 1L, resultCount = 2))

        val updated = SearchHistoryRules.add(history, "   ", 4, timestamp = 2L)

        assertEquals(history, updated)
    }
}
