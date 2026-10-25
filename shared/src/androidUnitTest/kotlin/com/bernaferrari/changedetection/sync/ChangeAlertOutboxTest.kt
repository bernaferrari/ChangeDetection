package com.bernaferrari.changedetection.sync

import com.bernaferrari.changedetection.repo.Site
import com.bernaferrari.changedetection.repo.Snap
import com.bernaferrari.changedetection.repo.source.local.PendingChangeAlert
import com.bernaferrari.changedetection.repo.source.local.PendingChangeAlertDao
import com.bernaferrari.changedetection.repo.source.local.SitesDao
import com.bernaferrari.changedetection.repo.source.local.SnapsDao
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import org.mockito.kotlin.*

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ChangeAlertOutboxTest {
    private val site = Site("Page", "https://example.test", 0L, "", "page", 0 to 0).copy(isRead = false, isSyncEnabled = true, isNotificationEnabled = true)
    private val snap = Snap("changed-version", site.id, 2L, "text/plain", "UTF-8", 5)

    @Test fun failedDeliverySurvivesRecreationAndRetriesTheSameSavedIdentity() = runTest {
        val queue = Queue()
        val sites = mock<SitesDao>()
        val snaps = mock<SnapsDao>()
        whenever(sites.getSiteById(site.id)).thenReturn(site)
        whenever(snaps.getLastSnapForSiteId(site.id)).thenReturn(snap)
        val submissions = mutableListOf<String>()
        val failing = ChangeAlertOutbox(queue, sites, snaps, ChangeNotifier { _, _ -> error("Transient notifier failure") })
        failing.enqueue(site, snap)
        failing.drain()
        assertEquals(listOf(snap.snapId), queue.rows.keys.toList())
        val recreated = ChangeAlertOutbox(queue, sites, snaps, ChangeNotifier { _, id -> submissions += id })
        recreated.drain()
        recreated.drain()
        assertEquals(listOf(snap.snapId), submissions)
        assertTrue(queue.rows.isEmpty())
    }

    @Test fun cancelledDeliveryKeepsItsIntentAndNeverDeletesHistory() = runTest {
        val queue = Queue()
        val sites = mock<SitesDao>()
        val snaps = mock<SnapsDao>()
        whenever(sites.getSiteById(site.id)).thenReturn(site)
        whenever(snaps.getLastSnapForSiteId(site.id)).thenReturn(snap)
        val outbox = ChangeAlertOutbox(queue, sites, snaps, ChangeNotifier { _, _ -> throw CancellationException("Worker stopped") })
        outbox.enqueue(site, snap)
        assertTrue(runCatching { outbox.drain() }.exceptionOrNull() is CancellationException)
        assertTrue(queue.rows.containsKey(snap.snapId))
        verify(snaps, never()).deleteSnapById(any())
    }

    @Test fun staleReadPausedDisabledAndEditedWatchAlertsAreDiscarded() = runTest {
        for (current in listOf(site.copy(isRead = true), site.copy(isSyncEnabled = false), site.copy(isNotificationEnabled = false), site.copy(url = "https://different.test"))) {
            val queue = Queue()
            val sites = mock<SitesDao>()
            val snaps = mock<SnapsDao>()
            val notifier = mock<ChangeNotifier>()
            whenever(notifier.supportsDelivery).thenReturn(true)
            whenever(sites.getSiteById(site.id)).thenReturn(current)
            whenever(snaps.getLastSnapForSiteId(site.id)).thenReturn(snap)
            val outbox = ChangeAlertOutbox(queue, sites, snaps, notifier)
            outbox.enqueue(site, snap)
            outbox.drain()
            assertTrue(queue.rows.isEmpty())
            verify(notifier, never()).notifyChange(any(), any())
        }
    }

    @Test fun supersededOrDeletedVersionsCannotReviveAnOlderNotification() = runTest {
        for (latest in listOf(snap.copy(snapId = "newer"), null)) {
            val queue = Queue()
            val sites = mock<SitesDao>()
            val snaps = mock<SnapsDao>()
            val posted = mutableListOf<String>()
            whenever(sites.getSiteById(site.id)).thenReturn(site)
            whenever(snaps.getLastSnapForSiteId(site.id)).thenReturn(latest)
            val outbox = ChangeAlertOutbox(queue, sites, snaps, ChangeNotifier { _, id -> posted += id })
            outbox.enqueue(site, snap)
            outbox.drain()
            assertTrue(queue.rows.isEmpty())
            assertTrue(posted.isEmpty())
        }
    }

    @Test fun crashAfterSubmissionMayRetryButKeepsStableReplacementIdentity() = runTest {
        val queue = Queue()
        val sites = mock<SitesDao>()
        val snaps = mock<SnapsDao>()
        val submitted = mutableListOf<String>()
        whenever(sites.getSiteById(site.id)).thenReturn(site)
        whenever(snaps.getLastSnapForSiteId(site.id)).thenReturn(snap)
        val outbox = ChangeAlertOutbox(queue, sites, snaps, ChangeNotifier { _, id -> submitted += id })
        outbox.enqueue(site, snap)
        queue.failNextComplete = true
        outbox.drain()
        assertTrue(queue.rows.containsKey(snap.snapId))
        outbox.drain()
        assertEquals(listOf(snap.snapId, snap.snapId), submitted)
        assertTrue(queue.rows.isEmpty()) // Native notifiers replace the same site ID; no claim of exactly-once OS receipt.
    }

    @Test fun unsupportedPlatformsDoNotAccumulateUndeliverableRows() = runTest {
        val queue = Queue()
        val outbox = ChangeAlertOutbox(queue, mock(), mock(), NoOpChangeNotifier)
        outbox.enqueue(site, snap)
        assertTrue(queue.rows.isEmpty())
    }

    @Test fun hangingNotifierIsBoundedAndItsIntentRemainsRetryable() = runTest {
        val queue = Queue()
        val sites = mock<SitesDao>()
        val snaps = mock<SnapsDao>()
        whenever(sites.getSiteById(site.id)).thenReturn(site)
        whenever(snaps.getLastSnapForSiteId(site.id)).thenReturn(snap)
        val outbox = ChangeAlertOutbox(queue, sites, snaps, ChangeNotifier { _, _ -> kotlinx.coroutines.awaitCancellation() })
        outbox.enqueue(site, snap)
        outbox.drain()
        assertEquals(15_000L, testScheduler.currentTime)
        assertTrue(queue.rows.containsKey(snap.snapId))
    }

    @Test fun busyStartupDeliveryDoesNotBlockAnotherCheckOrLoseIntent() = runTest {
        val queue = Queue()
        val sites = mock<SitesDao>()
        val snaps = mock<SnapsDao>()
        whenever(sites.getSiteById(site.id)).thenReturn(site)
        whenever(snaps.getLastSnapForSiteId(site.id)).thenReturn(snap)
        val outbox = ChangeAlertOutbox(queue, sites, snaps, ChangeNotifier { _, _ -> kotlinx.coroutines.awaitCancellation() })
        outbox.enqueue(site, snap)
        val active = async { outbox.drain() }
        testScheduler.runCurrent()
        outbox.drain()
        assertEquals(0L, testScheduler.currentTime)
        active.cancel()
        active.join()
        assertTrue(queue.rows.containsKey(snap.snapId))
    }

    @Test fun batchDeadlineBoundsMultipleBrokenNotifiersWithoutDroppingEitherRow() = runTest {
        val queue = Queue()
        val sites = mock<SitesDao>()
        val snaps = mock<SnapsDao>()
        val secondSite = site.copy(id = "second-page")
        val second = snap.copy(snapId = "second-change", siteId = secondSite.id)
        val pages = listOf(site, secondSite).associateBy { it.id }
        val versions = listOf(snap, second).associateBy { it.siteId }
        whenever(sites.getSiteById(any())).thenAnswer { pages[it.arguments[0] as String] }
        whenever(snaps.getLastSnapForSiteId(any())).thenAnswer { versions[it.arguments[0] as String] }
        val outbox = ChangeAlertOutbox(queue, sites, snaps, ChangeNotifier { _, _ -> kotlinx.coroutines.awaitCancellation() })
        outbox.enqueue(site, snap)
        outbox.enqueue(secondSite, second)
        outbox.drain()
        assertEquals(20_000L, testScheduler.currentTime)
        assertEquals(2, queue.rows.size)
    }

    @Test fun newerVersionCommittedDuringPreparationCannotPublishTheOlderAlert() = runTest {
        val queue = Queue()
        val sites = mock<SitesDao>()
        val snaps = mock<SnapsDao>()
        var latest = snap
        whenever(sites.getSiteById(site.id)).thenReturn(site)
        whenever(snaps.getLastSnapForSiteId(site.id)).thenAnswer { latest }
        val prepared = kotlinx.coroutines.CompletableDeferred<Unit>()
        val posted = mutableListOf<String>()
        val outbox = ChangeAlertOutbox(queue, sites, snaps, preparingNotifier(prepared, posted))
        outbox.enqueue(site, snap)
        val first = async { outbox.drain() }
        testScheduler.runCurrent()

        latest = snap.copy(snapId = "newer-change", timestamp = 3)
        outbox.enqueue(site, latest)
        outbox.drain() // The startup drain still owns delivery; this must not make the old alert eligible.
        prepared.complete(Unit)
        first.await()
        assertTrue(posted.isEmpty())
        assertEquals(setOf(latest.snapId), queue.rows.keys)

        outbox.drain()
        assertEquals(listOf(latest.snapId), posted)
        assertTrue(queue.rows.isEmpty())
    }

    @Test fun readPausedDisabledEditedAndDeletedPagesInvalidateAnAlertDuringPreparation() = runTest {
        for (updated in listOf(site.copy(isRead = true), site.copy(isSyncEnabled = false), site.copy(isNotificationEnabled = false), site.copy(url = "https://different.test"), null)) {
            val queue = Queue()
            val sites = mock<SitesDao>()
            val snaps = mock<SnapsDao>()
            var current: Site? = site
            whenever(sites.getSiteById(site.id)).thenAnswer { current }
            whenever(snaps.getLastSnapForSiteId(site.id)).thenReturn(snap)
            val prepared = kotlinx.coroutines.CompletableDeferred<Unit>()
            val posted = mutableListOf<String>()
            val outbox = ChangeAlertOutbox(queue, sites, snaps, preparingNotifier(prepared, posted))
            outbox.enqueue(site, snap)
            val owner = async { outbox.drain() }
            testScheduler.runCurrent()

            current = updated
            prepared.complete(Unit)
            owner.await()
            assertTrue("An invalidated alert must not post: $updated", posted.isEmpty())
            assertTrue(queue.rows.isEmpty())
        }
    }

    /** Exercises the same preparation/publication helper used by both native notifiers. */
    private fun preparingNotifier(prepared: kotlinx.coroutines.CompletableDeferred<Unit>, posted: MutableList<String>): ChangeNotifier = object : ChangeNotifier {
        private val delivery = ChangeAlertDelivery(prepare = { _, id ->
            prepared.await()
            id
        }, publish = { _, id, _, _ -> posted += id })
        override suspend fun notifyChange(site: Site, snapId: String) = delivery.deliver(site, snapId)
        override suspend fun notifyChangeIfCurrent(site: Site, snapId: String, isCurrent: suspend () -> Boolean) = delivery.deliver(site, snapId, isCurrent)
    }

    private class Queue : PendingChangeAlertDao {
        val rows = linkedMapOf<String, PendingChangeAlert>()
        var failNextComplete = false
        override suspend fun enqueue(alert: PendingChangeAlert) {
            rows.putIfAbsent(alert.snapId, alert)
        }
        override suspend fun firstBatch(limit: Int, siteId: String?): List<PendingChangeAlert> = rows.values.filter { siteId == null || it.siteId == siteId }.sortedWith(compareBy<PendingChangeAlert> { it.lastAttemptAt }.thenBy { it.createdAt }).take(limit)
        override suspend fun attempted(id: String, timestamp: Long) {
            rows[id]?.let { rows[id] = it.copy(lastAttemptAt = timestamp) }
        }
        override suspend fun complete(id: String) {
            if (failNextComplete) {
                failNextComplete = false
                error("Process interrupted after OS submission")
            }
            rows.remove(id)
        }
    }
}
