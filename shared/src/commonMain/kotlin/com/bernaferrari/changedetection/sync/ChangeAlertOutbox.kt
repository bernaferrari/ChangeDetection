package com.bernaferrari.changedetection.sync

import com.bernaferrari.changedetection.repo.Site
import com.bernaferrari.changedetection.repo.Snap
import com.bernaferrari.changedetection.repo.capturePolicy
import com.bernaferrari.changedetection.repo.source.local.PendingChangeAlert
import com.bernaferrari.changedetection.repo.source.local.PendingChangeAlertDao
import com.bernaferrari.changedetection.repo.source.local.SitesDao
import com.bernaferrari.changedetection.repo.source.local.SnapsDao
import com.bernaferrari.changedetection.util.ContentHasher
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.sync.Mutex
import kotlin.time.Clock

/**
 * At-least-once submission with stable platform replacement identities, not an exactly-once OS receipt.
 * A crash after OS submission but before acknowledgement can resubmit the same saved change. Both native
 * notifiers reuse the same site identity; Android suppresses sound when that identity already shows this snapshot.
 */
class ChangeAlertOutbox(
    private val pending: PendingChangeAlertDao,
    private val sites: SitesDao,
    private val snaps: SnapsDao,
    private val notifier: ChangeNotifier,
    private val now: () -> Long = { Clock.System.now().toEpochMilliseconds() },
) {
    private val delivery = Mutex()

    /** The caller must hold the same database write transaction that inserts [snap]. */
    suspend fun enqueue(site: Site, snap: Snap) {
        if (notifier.supportsDelivery && site.isNotificationEnabled) {
            pending.enqueue(PendingChangeAlert(snap.snapId, site.id, snap.timestamp, identity(site)))
        }
    }

    /** Fixed-size work and failure rotation keep a broken notifier from starving other pages. */
    suspend fun drain(onlySiteId: String? = null) {
        // A startup retry must never make a check wait behind unrelated delivery failures.
        if (!delivery.tryLock()) return
        try {
            kotlinx.coroutines.withTimeoutOrNull(20_000) {
                for (alert in pending.firstBatch(50, onlySiteId)) {
                    currentCoroutineContext().ensureActive()
                    val site = sites.getSiteById(alert.siteId)
                    if (site == null || !eligible(alert, site)) {
                        pending.complete(alert.snapId)
                        continue
                    }
                    pending.attempted(alert.snapId, maxOf(now().coerceAtLeast(1), if (alert.lastAttemptAt < Long.MAX_VALUE) alert.lastAttemptAt + 1 else Long.MAX_VALUE))
                    try {
                        kotlinx.coroutines.withTimeout(15_000) {
                            notifier.notifyChangeIfCurrent(site, alert.snapId) { eligible(alert, sites.getSiteById(alert.siteId)) }
                        }
                        pending.complete(alert.snapId)
                    } catch (timeout: kotlinx.coroutines.TimeoutCancellationException) {
                        currentCoroutineContext().ensureActive()
                        appLog.w("Saved change alert delivery timed out; retrying later")
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (error: Exception) {
                        // Preserve the intent across process/worker failure. History has already committed.
                        appLog.w("Saved change alert remains queued for retry", error)
                    }
                }
            }
        } finally {
            delivery.unlock()
        }
    }

    private suspend fun eligible(alert: PendingChangeAlert, site: Site?): Boolean = notifier.supportsDelivery && site != null && site.isNotificationEnabled && site.isSyncEnabled && !site.isRead &&
        identity(site) == alert.policyIdentity && snaps.getLastSnapForSiteId(alert.siteId)?.snapId == alert.snapId

    /** Retry on process startup and while it remains alive, even if monitoring has no due checks. */
    suspend fun run() {
        while (currentCoroutineContext().isActive) {
            try {
                drain()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                appLog.w("Pending alerts will retry", error)
            }
            delay(60_000)
        }
    }

    companion object {
        internal fun identity(site: Site): String {
            val policy = site.capturePolicy()
            return ContentHasher.hashContent("${site.url.length}:${site.url}:${policy.mode.name}:${policy.regionIdentity}".encodeToByteArray())
        }
    }
}

private val appLog = com.bernaferrari.changedetection.logging.AppLog.tagged("ChangeAlertOutbox")
