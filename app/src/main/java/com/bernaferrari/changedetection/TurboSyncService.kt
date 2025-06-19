package com.bernaferrari.changedetection

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.bernaferrari.changedetection.repo.source.SitesRepository
import com.bernaferrari.changedetection.repo.source.SnapsRepository
import com.bernaferrari.changedetection.settings.SettingsViewModel
import com.orhanobut.logger.Logger
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import kotlin.math.roundToInt

@AndroidEntryPoint
class TurboSyncService : Service() {

    @Inject
    lateinit var okHttpClient: OkHttpClient
    @Inject
    lateinit var sitesRepository: SitesRepository
    @Inject
    lateinit var snapsRepository: SnapsRepository
    @Inject
    lateinit var dataStore: DataStore<Preferences>

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val handler = Handler(Looper.getMainLooper())
    private var syncRunnable: Runnable? = null

    private var siteIds: List<String> = emptyList()
    private var startTime: Long = 0L
    private var syncCount = 0
    private val turboDuration = 3 * 60 * 60 * 1000L // 3 hours in milliseconds
    private val syncInterval = 30 * 1000L // 30 seconds

    companion object {
        private const val NOTIFICATION_ID = 100
        private const val CHANNEL_ID = "turbo_sync_channel"
        private const val CHANNEL_NAME = "Turbo Sync"
        private const val COMPLETION_NOTIFICATION_ID = 101
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        Logger.d("🚀 TurboSyncService created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        siteIds = intent?.getStringArrayListExtra("siteIds") ?: emptyList()
        startTime = intent?.getLongExtra("startTime", System.currentTimeMillis())
            ?: System.currentTimeMillis()

        if (siteIds.isEmpty()) {
            Logger.e("TurboSyncService started without siteIds")
            stopSelf()
            return START_NOT_STICKY
        }

        Logger.d("🚀 TurboSyncService starting for ${siteIds.size} sites: $siteIds")

        startForeground(NOTIFICATION_ID, createProgressNotification())
        startTurboSync()

        return START_STICKY
    }

    private fun startTurboSync() {
        syncRunnable = object : Runnable {
            override fun run() {
                val elapsed = System.currentTimeMillis() - startTime

                if (elapsed >= turboDuration) {
                    // 3 hours completed
                    completeTurboSync()
                    return
                }

                // Perform sync for all sites
                serviceScope.launch {
                    performSyncForAllSites()
                    syncCount++
                    updateNotification()
                }

                // Schedule next sync
                handler.postDelayed(this, syncInterval)
            }
        }

        // Start first sync immediately
        handler.post(syncRunnable!!)
    }

    private suspend fun performSyncForAllSites() {
        siteIds.forEach { siteId ->
            try {
                val site = sitesRepository.getSiteById(siteId) ?: return@forEach
                Logger.d("🔄 Turbo sync #${syncCount + 1} for ${site.title} (${site.id})")

                // Fetch content
                val request = Request.Builder()
                    .url(site.url)
                    .addHeader("User-Agent", "ChangeDetection-Turbo/1.0")
                    .build()

                okHttpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val contentType =
                            response.header("content-type", "text/html") ?: "text/html"
                        val bytes = response.body?.bytes() ?: return@forEach

                        // Save snap
                        val snap = com.bernaferrari.changedetection.repo.Snap(
                            siteId = siteId,
                            timestamp = System.currentTimeMillis(),
                            contentType = contentType,
                            contentSize = bytes.size,
                            contentCharset = "UTF-8"
                        )

                        snapsRepository.saveSnap(snap, bytes, 0)
                        sitesRepository.updateSite(
                            site.copy(
                                isSuccessful = true,
                                timestamp = System.currentTimeMillis()
                            )
                        )

                        Logger.d("✅ Turbo sync completed for ${site.title}")
                    } else {
                        Logger.e("❌ Turbo sync failed for ${site.title}: HTTP ${response.code}")
                        sitesRepository.updateSite(site.copy(isSuccessful = false))
                    }
                }
            } catch (e: Exception) {
                Logger.e("❌ Turbo sync error for site $siteId: ${e.message}")
            }
        }
    }

    private fun completeTurboSync() {
        Logger.d("🎉 Turbo mode completed after 3 hours with $syncCount sync rounds for ${siteIds.size} sites")

        // Disable turbo mode in settings
        serviceScope.launch {
            dataStore.edit { preferences ->
                preferences[SettingsViewModel.TURBO_MODE_KEY] = false
                preferences[SettingsViewModel.TURBO_SITE_IDS_KEY] = ""
                preferences[SettingsViewModel.TURBO_START_TIME_KEY] = 0L
            }
        }

        // Show completion notification
        showCompletionNotification()

        stopSelf()
    }

    private fun createProgressNotification() = NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle("🚀 Turbo Sync Active")
        .setContentText("Monitoring ${siteIds.size} sites every 30 seconds...")
        .setSmallIcon(R.drawable.ic_sync)
        .setColor(0xFF2196F3.toInt())
        .setContentIntent(createContentIntent())
        .setOngoing(true)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .build()

    private fun updateNotification() {
        val elapsed = System.currentTimeMillis() - startTime
        val remainingMs = turboDuration - elapsed
        val remainingMinutes = (remainingMs / (60 * 1000)).toInt()
        val progressPercent = ((elapsed.toDouble() / turboDuration) * 100).roundToInt()

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("🚀 Turbo Sync Active")
            .setContentText("${siteIds.size} sites • $syncCount rounds • ${remainingMinutes}min left")
            .setSmallIcon(R.drawable.ic_sync)
            .setColor(0xFF2196F3.toInt())
            .setContentIntent(createContentIntent())
            .setProgress(100, progressPercent, false)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun showCompletionNotification() {
        val totalSyncs = syncCount * siteIds.size
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("✅ Turbo Sync Complete")
            .setContentText("Completed $totalSyncs syncs across ${siteIds.size} sites over 3 hours")
            .setSmallIcon(R.drawable.ic_sync)
            .setColor(0xFF4CAF50.toInt())
            .setContentIntent(createContentIntent())
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(COMPLETION_NOTIFICATION_ID, notification)
    }

    private fun createContentIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java)
        return PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Turbo sync monitoring notifications"
                setShowBadge(false)
            }

            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        Logger.d("🛑 TurboSyncService destroying")

        syncRunnable?.let { handler.removeCallbacks(it) }
        serviceScope.cancel()

        stopForeground(true)
    }
}
