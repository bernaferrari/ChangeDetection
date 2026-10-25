package com.bernaferrari.changedetection.background

import com.bernaferrari.changedetection.repo.ColorGroup
import com.bernaferrari.changedetection.repo.Site
import com.bernaferrari.changedetection.sync.SyncResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class MonitoringBatchTest {
    private fun site(id: String) = Site(id, "https://example.test/$id", 0L, id, ColorGroup(0, 0), true, false)

    @Test fun `bounded batch resumes remaining pages without repeating its completed prefix`() = runTest {
        val pages = listOf("a", "b", "c", "d").map(::site)
        var state = BatchCheckpoint()
        var time = 0L
        val checked = mutableListOf<String>()
        suspend fun batch() = runMonitoringBatch(pages, state, { state = it }, { time }, budgetMs = 180_000, pageReserveMs = 90_000) { page, _, _ ->
            checked += page.id
            time += 90_000
            SyncResult.Success(page.id, false)
        }
        assertTrue(batch())
        assertEquals(listOf("a", "b"), checked)
        assertEquals(setOf("c", "d"), state.remaining)
        time = 0
        assertFalse(batch())
        assertEquals(listOf("a", "b", "c", "d"), checked)
        assertEquals(emptySet<String>(), state.remaining)
    }

    @Test fun `interrupted in flight page remains pending without starving other pages`() = runTest {
        val pages = listOf("a", "b", "c").map(::site)
        var state = BatchCheckpoint()
        val error = runCatching {
            runMonitoringBatch(pages, state, { state = it }, { 0L }) { _, _, _ -> throw CancellationException("worker stopped") }
        }.exceptionOrNull()
        assertTrue(error is CancellationException)
        assertEquals(setOf("a", "b", "c"), state.remaining)
        val order = mutableListOf<String>()
        assertFalse(runMonitoringBatch(pages, state, { state = it }, { 0L }) { page, _, _ -> order += page.id; SyncResult.Success(page.id, false) })
        assertEquals(listOf("b", "c", "a"), order)
    }

    @Test fun `paused or deleted remainder does not block a new round`() = runTest {
        val pages = listOf(site("new"))
        var state = BatchCheckpoint(setOf("deleted"))
        val seen = mutableListOf<String>()
        assertFalse(runMonitoringBatch(pages, state, { state = it }, { 0L }) { page, _, _ -> seen += page.id; SyncResult.Success(page.id, false) })
        assertEquals(listOf("new"), seen)
        assertEquals(emptySet<String>(), state.remaining)
    }
}
