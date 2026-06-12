/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.sync

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.webkit.WebView
import androidx.core.app.NotificationCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.bernaferrari.changedetection.browser.AndroidWebwrightSession
import com.bernaferrari.changedetection.browser.destroySafely
import com.bernaferrari.changedetection.repo.Site
import com.bernaferrari.changedetection.repo.source.local.SitesDao
import com.bernaferrari.changedetection.ui.settings.SettingsViewModel
import io.github.aakira.napier.Napier
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Foreground Service to capture screenshots of websites.
 * This is required because WebViews can only be reliably created and used
 * in an active Activity or a Foreground Service (with some caveats),
 * and they require the Main Thread.
 */
class ScreenshotService :
    Service(),
    KoinComponent {

    private val syncEngine: SyncEngine by inject()
    private val sitesDao: SitesDao by inject()
    private val dataStore: DataStore<Preferences> by inject()

    // Queue of site IDs to process
    private val queue = ArrayDeque<String>()
    private var isProcessing = false
    private var webView: WebView? = null
    private var webwrightSession: AndroidWebwrightSession? = null

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Napier.d("ScreenshotService created")
        startForeground(NOTIFICATION_ID, createNotification("Initializing sync..."))

        // Initialize WebView on Main Thread
        try {
            // ContextThemeWrapper is often needed for WebView in Service
            // But we use application context wrapped effectively by Service context
            webView = WebView(this).apply {
                // Set a fixed width for consistent rendering (e.g. mobile view)
                // 1080px width, height will grow
                layout(0, 0, VIEWPORT_WIDTH, VIEWPORT_HEIGHT)
            }
            webwrightSession = AndroidWebwrightSession.attach(
                context = this,
                webView = requireNotNull(webView),
                overviewMode = true,
            )
        } catch (e: Exception) {
            Napier.e("Failed to initialize WebView", e)
            stopSelf()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val siteId = intent?.getStringExtra(EXTRA_SITE_ID)

        if (siteId != null) {
            Napier.d("Received request to sync site: $siteId")
            queue.add(siteId)
            processNext()
        } else {
            Napier.w("Service started without siteId")
        }

        return START_NOT_STICKY
    }

    private fun processNext() {
        if (isProcessing) return
        val siteId = queue.removeFirstOrNull()

        if (siteId == null) {
            Napier.d("Queue empty, stopping service")
            stopSelf()
            return
        }

        isProcessing = true
        updateNotification("Syncing site...")

        serviceScope.launch {
            val site = withContext(Dispatchers.IO) {
                sitesDao.getSiteById(siteId)
            }

            if (site == null) {
                Napier.e("Site not found: $siteId")
                isProcessing = false
                processNext()
                return@launch
            }

            val delayMs = withContext(Dispatchers.IO) { getBrowserDelay() }
            processSite(site, delayMs)
        }
    }

    private suspend fun processSite(site: Site, delayMs: Long) {
        // Must be on Main Thread for WebView
        Napier.i("Loading URL: ${site.url}")
        val session = webwrightSession
        if (session == null) {
            handleError(site, "Browser session is not initialized")
            return
        }

        try {
            val navigation = session.goto(site.url)
            if (!navigation.ok) {
                handleError(site, navigation.error ?: "HTTP ${navigation.status}")
                return
            }
            session.waitForPageToSettle(delayMs)
            captureSnapshot(site, session)
        } catch (e: Exception) {
            Napier.e("Webwright navigation failed", e)
            handleError(site, e.message ?: "Unknown browser navigation error")
        }
    }

    private fun captureSnapshot(site: Site, session: AndroidWebwrightSession) {
        serviceScope.launch {
            try {
                val screenshot = session.captureFullPagePng(
                    maxHeightPixels = MAX_HEIGHT_PIXELS,
                )
                val result = SyncEngine.FetchResult.Success(screenshot.contentType, screenshot.bytes)
                syncEngine.processFetchResult(site, result)
                isProcessing = false
                processNext()
            } catch (e: Exception) {
                Napier.e("Failed to capture screenshot", e)
                handleError(site, e.message ?: "Unknown capture error")
            } catch (e: OutOfMemoryError) {
                Napier.e("OOM while capturing screenshot", e)
                handleError(site, "Out of Memory while capturing")
            }
        }
    }

    private fun handleError(site: Site, message: String) {
        serviceScope.launch {
            syncEngine.processFetchResult(site, SyncEngine.FetchResult.Error(message))
            withContext(Dispatchers.Main) {
                isProcessing = false
                processNext()
            }
        }
    }

    private fun createNotification(text: String): Notification {
        val channelId = "sync_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Sync Service", NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Change Detection")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_popup_sync) // Replace with app icon if available
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification(text: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, createNotification(text))
    }

    private suspend fun getBrowserDelay(): Long {
        val preferences = dataStore.data.first()
        return preferences[SettingsViewModel.BROWSER_DELAY_KEY] ?: 5000L
    }

    override fun onDestroy() {
        super.onDestroy()
        webwrightSession?.close()
        webwrightSession = null
        webView?.destroySafely(clearCache = false)
        serviceScope.cancel()
    }

    companion object {
        const val EXTRA_SITE_ID = "site_id"
        private const val NOTIFICATION_ID = 1234
        private const val MAX_HEIGHT_PIXELS = 15000 // Cap at ~15k pixels height to prevent crashes
        private const val VIEWPORT_WIDTH = 1080
        private const val VIEWPORT_HEIGHT = 1920
    }
}
