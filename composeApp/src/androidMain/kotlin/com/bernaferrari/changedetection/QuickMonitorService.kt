/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection

import com.bernaferrari.changedetection.library.R

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class QuickMonitorService :
    Service(),
    KoinComponent {

    private val workerHelper: WorkerHelper by inject()
    private val dataStore: DataStore<Preferences> by inject()

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var monitoringJob: Job? = null
    private var autoStopJob: Job? = null
    private var autoStopDeadlineMs: Long = 0L
    private var siteIds: List<String> = emptyList()
    private var intervalSeconds: Int = 15

    companion object {
        const val CHANNEL_ID = "quick_monitor_channel"
        const val NOTIFICATION_ID = 2001
        const val ACTION_STOP = "com.bernaferrari.changedetection.STOP_QUICK_MONITOR"
        const val ACTION_EXTEND = "com.bernaferrari.changedetection.EXTEND_QUICK_MONITOR"
        const val EXTRA_SITE_IDS = "site_ids"
        const val EXTRA_INTERVAL = "interval_seconds"

        fun start(context: Context, siteIds: List<String>, intervalSeconds: Int = 15) {
            Napier.d("QuickMonitorService.start called with ${siteIds.size} sites: $siteIds")
            val intent = Intent(context, QuickMonitorService::class.java).apply {
                putStringArrayListExtra(EXTRA_SITE_IDS, ArrayList(siteIds))
                putExtra(EXTRA_INTERVAL, intervalSeconds)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, QuickMonitorService::class.java))
        }

        fun extend(context: Context) {
            val intent = Intent(context, QuickMonitorService::class.java).apply {
                action = ACTION_EXTEND
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        Napier.d("QuickMonitorService onCreate called")
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopQuickMonitor()
                return START_NOT_STICKY
            }

            ACTION_EXTEND -> {
                extendOneHour()
                return START_STICKY
            }
        }

        intent?.let {
            siteIds = it.getStringArrayListExtra(EXTRA_SITE_IDS) ?: emptyList()
            intervalSeconds = it.getIntExtra(EXTRA_INTERVAL, 15)
        }

        if (siteIds.isEmpty()) {
            Napier.e("No sites to monitor, stopping service")
            stopSelf()
            return START_NOT_STICKY
        }

        Napier.d("Starting monitoring for ${siteIds.size} sites")

        // Start foreground service immediately
        try {
            val notification = createNotification()
            Napier.d("Notification created successfully")

            // Start foreground with the notification
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
            Napier.d("Foreground service started successfully")
        } catch (e: Exception) {
            Napier.e("Failed to start foreground service: ${e.message}")
            stopSelf()
            return START_NOT_STICKY
        }

        startMonitoring()

        return START_STICKY
    }

    private fun startMonitoring() {
        monitoringJob?.cancel()
        Napier.d("Starting Quick Monitor for ${siteIds.size} sites")

        monitoringJob = serviceScope.launch {
            scheduleAutoStop(hoursFromNow = 3)
            persistBannerState(active = true)

            while (isActive) {
                if (siteIds.isEmpty()) {
                    Napier.d("No sites left to monitor; stopping service")
                    stopQuickMonitor()
                    break
                }

                siteIds.forEach { siteId ->
                    try {
                        Napier.d("Triggering monitoring work for site: $siteId")
                        workerHelper.reloadSiteById(siteId)
                    } catch (e: Exception) {
                        Napier.e("Failed to trigger quick monitor for site $siteId: ${e.message}")
                    }
                    delay(500L)
                }

                val delaySeconds = intervalSeconds.coerceAtLeast(5)
                delay(delaySeconds.toLong() * 1000L)
            }
        }
    }

    private fun scheduleAutoStop(hoursFromNow: Int) {
        autoStopDeadlineMs = System.currentTimeMillis() + hoursFromNow * 60L * 60L * 1000L
        try {
            autoStopJob?.cancel()
        } catch (_: Exception) {
        }
        autoStopJob = serviceScope.launch {
            val delayMs = (autoStopDeadlineMs - System.currentTimeMillis()).coerceAtLeast(0L)
            delay(delayMs)
            Napier.d("Auto-stopping Quick Monitor after scheduled delay")
            stopQuickMonitor()
        }
    }

    private fun extendOneHour() {
        Napier.d("Extending Quick Monitor by 1 hour")
        val now = System.currentTimeMillis()
        if (autoStopDeadlineMs <= now) {
            autoStopDeadlineMs = now + 60L * 60L * 1000L
        } else {
            autoStopDeadlineMs += 60L * 60L * 1000L
        }
        try {
            autoStopJob?.cancel()
        } catch (_: Exception) {
        }
        autoStopJob = serviceScope.launch {
            val delayMs = (autoStopDeadlineMs - System.currentTimeMillis()).coerceAtLeast(0L)
            delay(delayMs)
            Napier.d("Auto-stopping Quick Monitor after extension delay")
            stopQuickMonitor()
        }
        // Update persisted deadline for UI
        persistBannerState(active = true)
        updateNotification()
    }

    private fun createNotification(): Notification {
        Napier.d("Creating notification for ${siteIds.size} sites")

        val stopIntent = Intent(this, QuickMonitorService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            0,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val mainIntent = Intent(this, MainActivityProvider.getMainActivityClass())
        val mainPendingIntent = PendingIntent.getActivity(
            this,
            0,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        // Minimal notification
        val sitesText = if (siteIds.size == 1) "1 site" else "${siteIds.size} sites"
        val intervalText = if (intervalSeconds >= 60) "${intervalSeconds / 60}m" else "${intervalSeconds}s"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Turbo Mode")
            .setContentText("$sitesText • Every $intervalText")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(mainPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .addAction(0, "Stop", stopPendingIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Quick Monitor",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Real-time monitoring notifications"
                setShowBadge(true)
                enableVibration(false)
                enableLights(true)
                lightColor = getColor(R.color.primary)
                setSound(null, null)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            Napier.d("Notification channel created: $CHANNEL_ID")
        }
    }

    private fun stopQuickMonitor() {
        Napier.d("Stopping Quick Monitor Service")
        monitoringJob?.cancel()
        autoStopJob?.cancel()
        workerHelper.stopQuickMonitor()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        // Ensure UI banner state is cleared
        persistBannerState(active = false)
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun updateNotification() {
        try {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.notify(NOTIFICATION_ID, createNotification())
        } catch (_: Exception) {
        }
    }

    private fun persistBannerState(active: Boolean) {
        serviceScope.launch {
            try {
                val ACTIVE = booleanPreferencesKey("quick_monitor_active")
                val SITE_IDS = stringPreferencesKey("quick_monitor_site_ids")
                val START_TIME = longPreferencesKey("quick_monitor_start_time")
                val DEADLINE_TIME = longPreferencesKey("quick_monitor_deadline_time")
                val INTERVAL = intPreferencesKey("quick_monitor_interval_seconds")
                dataStore.edit { prefs ->
                    prefs[ACTIVE] = active
                    prefs[SITE_IDS] = if (active) siteIds.joinToString(",") else ""
                    if (active) {
                        if ((prefs[START_TIME] ?: 0L) == 0L) {
                            prefs[START_TIME] = System.currentTimeMillis()
                        }
                        prefs[DEADLINE_TIME] = autoStopDeadlineMs
                        prefs[INTERVAL] = intervalSeconds
                    } else {
                        prefs[START_TIME] = 0L
                        prefs[DEADLINE_TIME] = 0L
                        prefs[INTERVAL] = 15
                    }
                }
            } catch (_: Exception) {
            }
        }
    }
}
