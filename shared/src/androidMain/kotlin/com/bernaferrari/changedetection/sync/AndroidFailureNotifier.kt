package com.bernaferrari.changedetection.sync

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.bernaferrari.changedetection.MainActivityProvider
import com.bernaferrari.changedetection.error.CheckFailure
import com.bernaferrari.changedetection.error.FailureUrgency
import com.bernaferrari.changedetection.library.R
import com.bernaferrari.changedetection.repo.Site

internal fun Context.canPostNotifications(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
    ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

/** Says once that a page is no longer being checked, and takes it back when the page works again. */
class AndroidFailureNotifier(private val context: Context) : FailureNotifier {
    @SuppressLint("MissingPermission")
    override suspend fun notifyStoppedWatching(site: Site, failure: CheckFailure) {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled() || !context.canPostNotifications()) return
        createChannel()

        // No snapshot id: the tap opens the page itself, where the cause and the ways out are shown.
        val open = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID,
            Intent(context, MainActivityProvider.getMainActivityClass()).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                data = Uri.Builder().scheme("changedetection").authority("page").appendPath(site.id).build()
                putExtra(ChangeNotificationContract.EXTRA_OPEN_COMPARISON, true)
                putExtra(ChangeNotificationContract.EXTRA_SITE_ID, site.id)
            },
            PendingIntent.FLAG_IMMUTABLE,
        )
        val label = site.title?.trim().takeUnless { it.isNullOrEmpty() } ?: Uri.parse(site.url).host?.removePrefix("www.") ?: site.url
        val text = context.getString(if (failure.urgency == FailureUrgency.NEEDS_YOU) R.string.failure_notification_needs_you else R.string.failure_notification_keeps_failing)
        // The lock screen must not reveal which page is watched.
        val publicVersion = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_change)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(context.getString(R.string.failure_notification_generic))
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_change)
            .setContentTitle(context.getString(R.string.failure_notification_named, label))
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(open)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(publicVersion)
            .setGroup(GROUP_KEY)
            .build()
        manager.notify(site.id, NOTIFICATION_ID, notification)
    }

    override suspend fun clear(siteId: String) {
        NotificationManagerCompat.from(context).cancel(siteId, NOTIFICATION_ID)
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(CHANNEL_ID, context.getString(R.string.failure_notification_channel_name), NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = context.getString(R.string.failure_notification_channel_description)
            lockscreenVisibility = NotificationCompat.VISIBILITY_PRIVATE
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    internal companion object {
        const val CHANNEL_ID = "page_problems_v1"
        const val GROUP_KEY = "page_problems"

        /** Next to the change alert's id, so one page can show both under its own tag. */
        const val NOTIFICATION_ID = AndroidChangeNotifier.NOTIFICATION_ID + 1
    }
}
