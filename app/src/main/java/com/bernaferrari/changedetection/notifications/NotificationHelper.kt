package com.bernaferrari.changedetection.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.bernaferrari.changedetection.MainActivity
import com.bernaferrari.changedetection.R
import com.bernaferrari.changedetection.repo.Site
import com.orhanobut.logger.Logger
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
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
            
            Logger.d("📱 Notification channels created")
        }
    }
    
    fun sendChangeDetectedNotification(site: Site, changeDescription: String = "Content has changed") {
        try {
            val intent = Intent(context, MainActivity::class.java).apply {
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
                .setContentTitle("🔔 Change Detected!")
                .setContentText("${site.title ?: site.url} - $changeDescription")
                .setStyle(NotificationCompat.BigTextStyle().bigText(
                    "Site: ${site.title ?: "Website"}\n" +
                    "URL: ${site.url}\n" +
                    "Change: $changeDescription\n\n" +
                    "Tap to view differences"
                ))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setCategory(NotificationCompat.CATEGORY_STATUS)
                .build()
            
            val notificationId = NOTIFICATION_ID_CHANGE_BASE + site.id.hashCode()
            
            val notificationManager = NotificationManagerCompat.from(context)
            if (notificationManager.areNotificationsEnabled()) {
                notificationManager.notify(notificationId, notification)
                Logger.d("🔔 Change notification sent for ${site.title}")
            } else {
                Logger.w("Notifications are disabled for the app")
            }
            
        } catch (e: Exception) {
            Logger.e("Failed to send change notification: ${e.message}")
        }
    }
    
    fun sendSyncErrorNotification(site: Site, errorMessage: String) {
        try {
            val intent = Intent(context, MainActivity::class.java).apply {
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
                .setContentTitle("⚠️ Sync Error")
                .setContentText("${site.title ?: site.url} - $errorMessage")
                .setStyle(NotificationCompat.BigTextStyle().bigText(
                    "Site: ${site.title ?: "Website"}\n" +
                    "URL: ${site.url}\n" +
                    "Error: $errorMessage"
                ))
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()
            
            val notificationId = NOTIFICATION_ID_CHANGE_BASE + site.id.hashCode() + 5000
            
            val notificationManager = NotificationManagerCompat.from(context)
            if (notificationManager.areNotificationsEnabled()) {
                notificationManager.notify(notificationId, notification)
                Logger.d("⚠️ Sync error notification sent for ${site.title}")
            }
            
        } catch (e: Exception) {
            Logger.e("Failed to send sync error notification: ${e.message}")
        }
    }
    
    fun cancelNotificationsForSite(siteId: String) {
        try {
            val notificationId = NOTIFICATION_ID_CHANGE_BASE + siteId.hashCode()
            NotificationManagerCompat.from(context).cancel(notificationId)
            Logger.d("Cancelled notifications for site: $siteId")
        } catch (e: Exception) {
            Logger.e("Failed to cancel notifications: ${e.message}")
        }
    }
}