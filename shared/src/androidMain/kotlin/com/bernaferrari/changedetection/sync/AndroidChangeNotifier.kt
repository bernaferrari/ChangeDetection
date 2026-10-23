package com.bernaferrari.changedetection.sync

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.bernaferrari.changedetection.MainActivityProvider
import com.bernaferrari.changedetection.library.R
import com.bernaferrari.changedetection.repo.Site

/** Intent contract shared by change alerts and the Android app entry point. */
object ChangeNotificationContract {
    const val EXTRA_OPEN_COMPARISON = "openComparison"
    const val EXTRA_SITE_ID = "siteId"
    const val EXTRA_SNAP_ID = "snapId"
}

/** Posts one actionable notification for each persisted, non-baseline website change. */
class AndroidChangeNotifier internal constructor(
    private val context: Context,
    prepare: suspend (Site, String) -> PreparedAndroidAlert? = { site, id -> prepareAndroidAlert(context, site, id) },
) : ChangeNotifier {
    private val delivery = ChangeAlertDelivery(prepare = prepare, publish = ::post)

    override suspend fun notifyChange(site: Site, snapId: String) {
        delivery.deliver(site, snapId)
    }

    @SuppressLint("MissingPermission")
    private fun post(site: Site, snapId: String, prepared: PreparedAndroidAlert?) {
        val notificationManager = NotificationManagerCompat.from(context)
        if (!notificationManager.areNotificationsEnabled() || !hasNotificationPermission()) {
            appLog.d("Change alert skipped because notifications are unavailable")
            return
        }

        createChannel()

        // Full site tags keep alerts independent even when IDs have identical hashes.
        // Each immutable tap token identifies the snapshot displayed by that alert.
        val contentIntent = runCatching {
            PendingIntent.getActivity(
                context,
                NOTIFICATION_ID,
                Intent(context, MainActivityProvider.getMainActivityClass()).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    data = Uri.Builder().scheme("changedetection").authority("change")
                        .appendPath(site.id).appendPath(snapId).build()
                    putExtra(ChangeNotificationContract.EXTRA_OPEN_COMPARISON, true)
                    putExtra(ChangeNotificationContract.EXTRA_SITE_ID, site.id)
                    putExtra(ChangeNotificationContract.EXTRA_SNAP_ID, snapId)
                },
                PendingIntent.FLAG_IMMUTABLE,
            )
        }.getOrElse { error ->
            appLog.w("Unable to create the change-notification deep link", error)
            return
        }

        val siteLabel = site.title?.trim().takeUnless { it.isNullOrEmpty() } ?: Uri.parse(site.url).host?.removePrefix("www.") ?: site.url
        val message = prepared?.summary ?: context.getString(R.string.change_notification_generic)

        val publicVersion = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_change)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(context.getString(R.string.change_notification_generic))
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_change)
            .setContentTitle(context.getString(R.string.change_notification_named, siteLabel))
            .setOnlyAlertOnce(true)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(publicVersion)
            .setGroup(GROUP_KEY)
            .setWhen(System.currentTimeMillis())
        browserPendingIntent(site, snapId)?.let { browser ->
            builder.addAction(0, context.getString(R.string.change_notification_open_browser), browser)
        }
        prepared?.image?.let { bitmap ->
            builder.setStyle(NotificationCompat.BigPictureStyle().bigPicture(bitmap).setSummaryText(message))
        }
        val notification = builder.build()

        notificationManager.notify(site.id, NOTIFICATION_ID, notification)
    }

    private fun browserPendingIntent(site: Site, snapId: String): PendingIntent? {
        val uri = runCatching { Uri.parse(site.url) }.getOrNull() ?: return null
        if (uri.scheme?.lowercase() !in setOf("https", "http") || uri.host.isNullOrBlank()) return null
        val intent = Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        // Identifier participates in PendingIntent identity without changing the watched URL.
        if (Build.VERSION.SDK_INT >= 29) intent.identifier = "changedetection/browser/${site.id}/$snapId"
        return runCatching {
            PendingIntent.getActivity(context, browserRequests.incrementAndGet(), intent, PendingIntent.FLAG_IMMUTABLE)
        }.getOrNull()
    }

    private fun hasNotificationPermission(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.change_notification_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(R.string.change_notification_channel_description)
            setShowBadge(true)
            lockscreenVisibility = NotificationCompat.VISIBILITY_PRIVATE
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    internal companion object {
        val browserRequests = java.util.concurrent.atomic.AtomicInteger(0)
        const val CHANNEL_ID = "website_changes_v1"
        const val GROUP_KEY = "website_changes"
        const val NOTIFICATION_ID = 0x40000000
    }
}

private val appLog = com.bernaferrari.changedetection.logging.AppLog.tagged("AndroidChangeNotifier")
