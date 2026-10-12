package com.bernaferrari.changedetection.background

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RequestAcknowledgementTest {
    @Test fun `queued retry releases tap feedback without waiting for completion`() = runTest {
        val jobs = MutableStateFlow(emptyList<SyncJobInfo>())
        val waiting = async { awaitSiteRequest(jobs, "page") }
        runCurrent()
        jobs.value = listOf(SyncJobInfo(SyncState.ENQUEUED, setOf("page")))
        runCurrent()
        assertTrue(waiting.isCompleted)
    }

    @Test fun `other pages do not acknowledge the request`() = runTest {
        val jobs = MutableStateFlow(listOf(SyncJobInfo(SyncState.RUNNING, setOf("other"))))
        val waiting = async { awaitSiteRequest(jobs, "page") }
        runCurrent()
        assertFalse(waiting.isCompleted)
        jobs.value = listOf(SyncJobInfo(SyncState.RUNNING, setOf("page")))
        waiting.await()
    }

    @Test fun `missing acknowledgement cannot leave feedback stuck`() = runTest {
        val waiting = async { awaitSiteRequest(MutableStateFlow(emptyList()), "page") }
        runCurrent()
        advanceTimeBy(5_000)
        runCurrent()
        assertTrue(waiting.isCompleted)
    }
}
