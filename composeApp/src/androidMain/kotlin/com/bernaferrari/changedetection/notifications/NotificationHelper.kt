package com.bernaferrari.changedetection.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.bernaferrari.changedetection.MainActivityProvider
import com.bernaferrari.changedetection.library.R
import com.bernaferrari.changedetection.repo.Site
import io.github.aakira.napier.Napier

class NotificationHelper(private val context: Context) {

    companion object {
        private const val CHANNEL_ID_CHANGES = "change_detection_changes"
        private const val CHANNEL_ID_SYNC = "change_detection_sync"
        private const val NOTIFICATION_ID_CHANGE_BASE = 1000

        private const val CHANNEL_NAME_CHANGES = "Change Detection"
        private const val CHANNEL_NAME_SYNC = "Sync Status"

        private const val CHANNEL_DESC_CHANGES = "Notifications when website changes are detected"
        private const val CHANNEL_DESC_SYNC = "Background sync status and errors"
    }

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Channel for change detection notifications
            val changesChannel = NotificationChannel(
                CHANNEL_ID_CHANGES,
                CHANNEL_NAME_CHANGES,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESC_CHANGES
                enableVibration(true)
                setShowBadge(true)
            }

            // Channel for sync status notifications
            val syncChannel = NotificationChannel(
                CHANNEL_ID_SYNC,
                CHANNEL_NAME_SYNC,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = CHANNEL_DESC_SYNC
                enableVibration(false)
                setShowBadge(false)
            }

            notificationManager.createNotificationChannel(changesChannel)
            notificationManager.createNotificationChannel(syncChannel)

            Napier.d("Notification channels created")
        }
    }

    fun sendChangeDetectedNotification(site: Site, changeDescription: String = "Content has changed") {
        try {
            val intent = Intent(context, MainActivityProvider.getMainActivityClass()).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("siteId", site.id)
                putExtra("openComparison", true)
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                site.id.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, CHANNEL_ID_CHANGES)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(site.title ?: site.url)
                .setContentText("Change detected")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build()

            val notificationId = NOTIFICATION_ID_CHANGE_BASE + site.id.hashCode()

            val notificationManager = NotificationManagerCompat.from(context)
            if (notificationManager.areNotificationsEnabled()) {
                notificationManager.notify(notificationId, notification)
                Napier.d("Change notification sent for ${site.title}")
            } else {
                Napier.w("Notifications are disabled for the app")
            }

        } catch (e: Exception) {
            Napier.e("Failed to send change notification: ${e.message}")
        }
    }

    fun sendSyncErrorNotification(site: Site, errorMessage: String) {
        try {
            val intent = Intent(context, MainActivityProvider.getMainActivityClass()).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("siteId", site.id)
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                site.id.hashCode() + 10000,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, CHANNEL_ID_SYNC)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(site.title ?: site.url)
                .setContentText("Sync failed")
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

            val notificationId = NOTIFICATION_ID_CHANGE_BASE + site.id.hashCode() + 5000

            val notificationManager = NotificationManagerCompat.from(context)
            if (notificationManager.areNotificationsEnabled()) {
                notificationManager.notify(notificationId, notification)
                Napier.d("Sync error notification sent for ${site.title}")
            }

        } catch (e: Exception) {
            Napier.e("Failed to send sync error notification: ${e.message}")
        }
    }

    fun cancelNotificationsForSite(siteId: String) {
        try {
            val notificationId = NOTIFICATION_ID_CHANGE_BASE + siteId.hashCode()
            NotificationManagerCompat.from(context).cancel(notificationId)
            Napier.d("Cancelled notifications for site: $siteId")
        } catch (e: Exception) {
            Napier.e("Failed to cancel notifications: ${e.message}")
        }
    }
}
