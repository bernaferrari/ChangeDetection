package com.bernaferrari.changedetection.mainnew

import android.content.Context
import android.view.View
import com.bernaferrari.changedetection.extensions.convertTimestampToDate
import com.bernaferrari.changedetection.extensions.readableFileSize
import com.bernaferrari.changedetection.repo.Site
import com.bernaferrari.changedetection.repo.SiteAndLastSnap
import com.bernaferrari.changedetection.repo.Snap
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit


// creates a completable animation for fading the reload button when pressed
fun View.fadeOut(duration: Long = 30): Completable {
    return Completable.create {
        animate().setDuration(duration)
            .alpha(0f)
            .withEndAction {
                visibility = View.GONE
                it.onComplete()
            }
    }
}

// creates a completable animation for shrinking the reload button when pressed
fun View.shrinkIn(duration: Long = 30): Completable {
    return Completable.create {
        animate().setDuration(duration)
            .scaleX(0.25f)
            .scaleY(0.25f)
            .withEndAction {
                it.onComplete()
            }
    }
}

/**
 * This calculates how long it will take until updating the "last sync" text again.
 *
 * @param timestamp in milliseconds
 */
fun Long.generateTimerDisposable(): Completable {
    // Get the abs value just in case our user is the terminator and came from the future.
    val delay = Math.abs(System.currentTimeMillis() - this)

    val seconds = delay / 1000
    val min = seconds / 60
    val hours = min / 60

    val timeToWait = when {
        hours / 24 > 0 -> 3600 * 24
        hours > 0 -> 3600
        min > 0 -> 60
        else -> 30
    }

    val ttl = (timeToWait * 1000).toFloat() // in Milli
    val finalTimeToWait = ((1 - ((delay / ttl) % 1)) * ttl).toLong()

    // If timeAgo is 3.9 days ago, we want to refresh when it is 4 days ago, so we wait 0.1 day.
    // If timeAgo is 7 years ago, we want to refresh when it is 8 years ago, so we wait 1 year.
    // If timeAgo is 1 min ago, we want to refresh when it is 2 min ago.

    return Completable.timer(
        finalTimeToWait,
        TimeUnit.MILLISECONDS,
        AndroidSchedulers.mainThread()
    )
}


/**
 * This calculates how long it will take until updating the "last sync" text again.
 *
 * @param timestamp in milliseconds
 */
fun Long.generateRemainingTimer(): Long {
    // Get the abs value just in case our user is the terminator and came from the future.
    val delay = Math.abs(System.currentTimeMillis() - this)

    val seconds = delay / 1000
    val min = seconds / 60
    val hours = min / 60

    val timeToWait = when {
        hours / 24 > 0 -> 3600 * 24
        hours > 0 -> 3600
        min > 0 -> 60
        else -> 30
    }

    val ttl = (timeToWait * 1000).toFloat() // in Milli
    val finalTimeToWait = ((1 - ((delay / ttl) % 1)) * ttl).toLong()

    // If timeAgo is 3.9 days ago, we want to refresh when it is 4 days ago, so we wait 0.1 day.
    // If timeAgo is 7 years ago, we want to refresh when it is 8 years ago, so we wait 1 year.
    // If timeAgo is 1 min ago, we want to refresh when it is 2 min ago.

    return finalTimeToWait
}

internal fun getTitle(site: Site?): String {
    if (site == null) return ""

    return if (site.title.isNullOrBlank()) {
        site.url.replaceFirst("^(http[s]?://www\\.|http[s]?://|www\\.)".toRegex(), "")
    } else {
        site.title ?: ""
    }
}

internal fun getLastSync(
    context: Context,
    siteLstSnp: SiteAndLastSnap,
    isSyncing: Boolean
): String {
    val siteTs = siteLstSnp.site.timestamp
    val ts = siteTs.convertTimestampToDate()

    return when {
        isSyncing -> context.getString(com.bernaferrari.changedetection.R.string.syncing)
        else -> when {
            siteLstSnp.site.isSuccessful -> "$ts – ${siteLstSnp.snap?.contentSize?.readableFileSize()}"
            else -> "$ts – ${context.getString(com.bernaferrari.changedetection.R.string.error)}"
        }
    }
}

internal fun getLastDiff(context: Context, lastSnap: Snap?): String =
    lastSnap?.timestamp?.convertTimestampToDate()
            ?: context.getString(com.bernaferrari.changedetection.R.string.nothing_yet)

internal fun getLogsSubtitle(lastSnap: Snap): String =
    "${lastSnap.contentSize.readableFileSize()} • ${lastSnap.timestamp.convertTimestampToDate()}"

internal fun Long.formatDayMonthYear(): String? {
    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    return sdf.format(Date(this))
}

internal fun Long.getDayOfMonth(): String {
    val sdf = SimpleDateFormat("dd", Locale.getDefault())
    return sdf.format(Date(this))
}