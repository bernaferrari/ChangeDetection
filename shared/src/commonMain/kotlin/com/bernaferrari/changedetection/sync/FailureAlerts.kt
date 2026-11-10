package com.bernaferrari.changedetection.sync

import com.bernaferrari.changedetection.error.CheckFailure
import com.bernaferrari.changedetection.error.FailureUrgency
import com.bernaferrari.changedetection.repo.Site

/** Tells the person, once per run of failed checks, that a page is no longer being watched. */
interface FailureNotifier {
    suspend fun notifyStoppedWatching(site: Site, failure: CheckFailure)

    /** The page works again, so an alert still on screen would now be false. */
    suspend fun clear(siteId: String) = Unit
}

/** Platforms without notification delivery; the page's own screen still shows the failure. */
object NoOpFailureNotifier : FailureNotifier {
    override suspend fun notifyStoppedWatching(site: Site, failure: CheckFailure) = Unit
}

private const val HOUR = 60 * 60 * 1000L

/** A problem only the person can fix is confirmed by one more check, so a single odd answer never alerts. */
internal const val CONFIRMED_FAILURES = 2

/** Sites and networks have bad hours; a few failures spread over this long is no longer a blip. */
internal const val PERSISTENT_FAILURES = 3
internal const val PERSISTENT_AFTER_MS = 6 * HOUR

/** However few checks ran (a weekly schedule, say), a day without a successful one is worth knowing about. */
internal const val STALE_AFTER_MS = 24 * HOUR

/**
 * Whether the current run of failed checks is worth interrupting someone for. Quiet for blips, prompt when
 * waiting cannot help, silent when the device (not the page) is the cause, and never twice for one run.
 */
internal fun shouldAlertFailure(site: Site, now: Long): Boolean {
    if (site.isSuccessful || site.failureAlerted || !site.isSyncEnabled || !site.isNotificationEnabled) return false
    val failingFor = now - (site.failingSince ?: return false)
    return when (CheckFailure.fromCode(site.lastFailureCode).urgency) {
        FailureUrgency.NEEDS_YOU -> site.failureCount >= CONFIRMED_FAILURES
        FailureUrgency.MAY_PASS -> (site.failureCount >= PERSISTENT_FAILURES && failingFor >= PERSISTENT_AFTER_MS) || (site.failureCount >= CONFIRMED_FAILURES && failingFor >= STALE_AFTER_MS)
        FailureUrgency.DEVICE -> false
    }
}
