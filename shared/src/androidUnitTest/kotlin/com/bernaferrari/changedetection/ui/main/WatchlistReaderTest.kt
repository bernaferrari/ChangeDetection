package com.bernaferrari.changedetection.ui.main

import com.bernaferrari.changedetection.repo.Site
import com.bernaferrari.changedetection.repo.SiteAndLastSnap
import com.bernaferrari.changedetection.repo.Snap
import com.bernaferrari.changedetection.repo.source.SitesRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test
import org.mockito.kotlin.*

@OptIn(ExperimentalCoroutinesApi::class)
class WatchlistReaderTest {
    private val sites: SitesRepository = mock()
    private val page = Site("Tickets", "https://example.test", 3L, "", "page", 0 to 0)
    private val snap = Snap("latest", "page", 3L, "text/html", "UTF-8", 20)
    private val rows = listOf(SiteAndLastSnap(page, snap, 2))
    private val failures = mutableListOf<Throwable>()
    private var successfulLoads = 0

    private fun reader() = watchlistReader(sites, { failures += it }, { successfulLoads++ })

    @Test fun `history read failure retains previous complete result and recovers`() = runTest {
        val revision = MutableStateFlow(0)
        var fail = false
        whenever(sites.observeWatchlist()).thenReturn(
            flow {
                revision.collect {
                    if (fail) error("Database temporarily unavailable")
                    emit(rows.map { row -> row.copy(snapshotCount = 2 + it) })
                }
            },
        )
        val emissions = mutableListOf<List<SiteAndLastSnap>>()
        val observer = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { reader().collect { emissions += it } }
        runCurrent()
        assertEquals(2, emissions.last().single().snapshotCount)
        fail = true
        revision.value = 1
        runCurrent()
        assertEquals(1, emissions.size)
        assertEquals(snap, emissions.last().single().snap)
        assertEquals(1, failures.size)
        advanceTimeBy(1_000)
        runCurrent()
        assertEquals(1, failures.size)
        fail = false
        advanceTimeBy(2_000)
        runCurrent()
        assertEquals(3, emissions.last().single().snapshotCount)
        assertEquals(2, successfulLoads)
        observer.cancel()
    }

    @Test fun `initial query failure never emits a fabricated empty history`() = runTest {
        whenever(sites.observeWatchlist()).thenReturn(flow { error("Cannot read history") })
        val emissions = mutableListOf<List<SiteAndLastSnap>>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { reader().collect { emissions += it } }
        runCurrent()
        assertTrue(emissions.isEmpty())
        assertEquals(1, failures.size)
    }

    @Test fun `failed site observation resubscribes and recovers without navigation`() = runTest {
        var attempts = 0
        whenever(sites.observeWatchlist()).thenReturn(
            flow {
                if (attempts++ == 0) error("Read connection closed")
                emit(rows)
            },
        )
        val emissions = mutableListOf<List<SiteAndLastSnap>>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { reader().collect { emissions += it } }
        runCurrent()
        assertTrue(emissions.isEmpty())
        advanceTimeBy(1_000)
        runCurrent()
        assertEquals(snap, emissions.single().single().snap)
        assertEquals(1, successfulLoads)
    }

    @Test fun `cancellation is not a load error or retry`() = runTest {
        var attempts = 0
        whenever(sites.observeWatchlist()).thenReturn(
            flow {
                attempts++
                throw CancellationException("Screen closed")
            },
        )
        val observer = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { reader().collect() }
        runCurrent()
        assertTrue(observer.isCancelled)
        assertTrue(failures.isEmpty())
        assertEquals(1, attempts)
    }
}
