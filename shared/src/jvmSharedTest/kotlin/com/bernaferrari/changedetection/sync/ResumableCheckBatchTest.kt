package com.bernaferrari.changedetection.sync

import com.bernaferrari.changedetection.repo.Site
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class ResumableCheckBatchTest {
    private fun site(id: String, checkedAt: Long = 0) = Site(id, "https://example.test/$id", checkedAt, "", id, 0 to 0)

    @Test fun interruptedCaptureDoesNotStarveLaterPagesAfterRestart() = runTest {
        var savedCursor: String? = null
        val visited = mutableListOf<String>()
        val sites = listOf(site("a-slow"), site("b-later"), site("recent", 100))
        try {
            checkResumableBatch(sites, savedCursor, { savedCursor = it }) { id ->
                visited += id
                assertEquals(id, savedCursor)
                throw CancellationException("Background time expired")
            }
            fail("Expiration must propagate")
        } catch (_: CancellationException) { }
        assertEquals("a-slow", visited.single())
        visited.clear()
        // A fresh caller has only the persisted cursor, not the previous loop's memory.
        checkResumableBatch(sites, savedCursor, { savedCursor = it }) { id ->
            visited += id
            SyncResult.Success(id, false)
        }
        assertEquals(listOf("b-later", "recent", "a-slow"), visited)
        assertNull(savedCursor)
    }

    @Test fun deletedCursorFallsBackToOldestFirstAndPausedPagesAreIgnored() = runTest {
        val visited = mutableListOf<String>()
        checkResumableBatch(listOf(site("new", 100), site("paused").copy(isSyncEnabled = false), site("old")), "deleted", {}) { id ->
            visited += id
            SyncResult.Success(id, false)
        }
        assertEquals(listOf("old", "new"), visited)
    }

    @Test fun ordinaryFailuresContinueAndClearTheCompletedBatchCursor() = runTest {
        var savedCursor: String? = "previous"
        val visited = mutableListOf<String>()
        try {
            checkResumableBatch(listOf(site("a"), site("b")), savedCursor, { savedCursor = it }) { id ->
                visited += id
                if (id == "a") error("Offline")
                SyncResult.Success(id, false)
            }
            fail("Batch failure must propagate")
        } catch (failed: CheckBatchFailure) {
            assertEquals(1, failed.failed)
        }
        assertEquals(listOf("a", "b"), visited)
        assertNull(savedCursor)
    }
}
