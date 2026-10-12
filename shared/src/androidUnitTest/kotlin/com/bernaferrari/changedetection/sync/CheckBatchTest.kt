package com.bernaferrari.changedetection.sync

import com.bernaferrari.changedetection.error.ErrorResult
import com.bernaferrari.changedetection.error.ErrorType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class CheckBatchTest {
    @Test fun failedResultsAndExceptionsAreAggregatedAfterCheckingLaterPages() = runTest {
        val visited = mutableListOf<String>()
        try {
            checkBatch(listOf("failure", "throws", "healthy")) { id ->
                visited.add(id)
                when (id) {
                    "failure" -> SyncResult.Failed(id, ErrorResult(ErrorType.NETWORK, "Offline"))
                    "throws" -> error("Unavailable")
                    else -> SyncResult.Success(id, false)
                }
            }
            fail("A failed batch must not complete as success")
        } catch (failed: CheckBatchFailure) {
            assertEquals(2, failed.failed)
            assertEquals(3, failed.checked)
        }
        assertEquals(listOf("failure", "throws", "healthy"), visited)
    }

    @Test fun cancellationStopsTheBatchBeforeAnotherPageCanStart() = runTest {
        val visited = mutableListOf<String>()
        try {
            checkBatch(listOf("first", "second")) { id ->
                visited.add(id)
                throw CancellationException("Expired")
            }
            fail("Cancellation must propagate")
        } catch (_: CancellationException) { }
        assertEquals(listOf("first"), visited)
    }

    @Test fun successfulAndSkippedChecksCompleteNormally() = runTest {
        assertEquals(2, checkBatch(listOf("healthy", "deleted")) { id -> if (id == "healthy") SyncResult.Success(id, false) else SyncResult.Skipped(id, "Deleted") })
    }
}
