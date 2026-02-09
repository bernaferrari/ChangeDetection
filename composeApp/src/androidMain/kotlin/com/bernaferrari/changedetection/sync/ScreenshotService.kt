package com.bernaferrari.changedetection.sync

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.app.NotificationCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.bernaferrari.changedetection.repo.Site
import com.bernaferrari.changedetection.repo.source.local.SitesDao
import com.bernaferrari.changedetection.ui.settings.SettingsViewModel
import io.github.aakira.napier.Napier
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.ByteArrayOutputStream

/**
 * Foreground Service to capture screenshots of websites.
 * This is required because WebViews can only be reliably created and used
 * in an active Activity or a Foreground Service (with some caveats),
 * and they require the Main Thread.
 */
class ScreenshotService : Service(), KoinComponent {

    private val syncEngine: SyncEngine by inject()
    private val sitesDao: SitesDao by inject()
    private val dataStore: DataStore<Preferences> by inject()

    // Queue of site IDs to process
    private val queue = ArrayDeque<String>()
    private var isProcessing = false
    private var webView: WebView? = null

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val handler = Handler(Looper.getMainLooper())

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
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true

                // Set a fixed width for consistent rendering (e.g. mobile view)
                // 1080px width, height will grow
                layout(0, 0, 1080, 1920)
            }
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

    private fun processSite(site: Site, delayMs: Long) {
        // Must be on Main Thread for WebView
        Napier.i("Loading URL: ${site.url}")

        webView?.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)

                // Wait for dynamic content to load (user configured delay)
                handler.postDelayed({
                    captureSnapshot(site)
                }, delayMs)
            }

            override fun onReceivedError(
                view: WebView?,
                request: android.webkit.WebResourceRequest?,
                error: android.webkit.WebResourceError?
            ) {
                val description = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    error?.description?.toString() ?: "Unknown error"
                } else {
                    "Unknown error"
                }
                Napier.e("WebView error: $description")
                handleError(site, "WebView error: $description")
            }
        }

        webView?.loadUrl(site.url)
    }

    private fun captureSnapshot(site: Site) {
        val wv = webView ?: return

        try {
            // Measure the full height of the content
            // Note: wv.scale is deprecated, use displayMetrics.density instead
            val density = resources.displayMetrics.density
            val contentHeight = (wv.contentHeight * density).toInt()
            val safeHeight = contentHeight.coerceAtMost(MAX_HEIGHT_PIXELS)
            val width = wv.width

            if (width <= 0 || safeHeight <= 0) {
                handleError(site, "Invalid WebView dimensions")
                return
            }

            // Create bitmap
            val bitmap = Bitmap.createBitmap(width, safeHeight, Bitmap.Config.RGB_565) // 565 saves memory
            val canvas = Canvas(bitmap)

            // Layout again to ensure it renders full height?
            // wv.layout(0, 0, width, safeHeight) // This might trigger relayout/reload

            wv.draw(canvas)

            // Compress to Byte Array (WebP or PNG)
            serviceScope.launch(Dispatchers.Default) {
                val stream = ByteArrayOutputStream()
                // Use WebP for efficiency
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, 80, stream)
                } else {
                    bitmap.compress(Bitmap.CompressFormat.WEBP, 80, stream)
                }
                val byteArray = stream.toByteArray()

                bitmap.recycle() // Free memory immediately

                // Pass to SyncEngine
                val result = SyncEngine.FetchResult.Success("image/webp", byteArray)
                syncEngine.processFetchResult(site, result)

                // Done
                withContext(Dispatchers.Main) {
                    isProcessing = false
                    processNext()
                }
            }

        } catch (e: Exception) {
            Napier.e("Failed to capture screenshot", e)
            handleError(site, e.message ?: "Unknown capture error")
        } catch (e: OutOfMemoryError) {
            Napier.e("OOM while capturing screenshot", e)
            handleError(site, "Out of Memory while capturing")
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
        webView?.destroy()
        serviceScope.cancel()
    }

    companion object {
        const val EXTRA_SITE_ID = "site_id"
        private const val NOTIFICATION_ID = 1234
        private const val MAX_HEIGHT_PIXELS = 15000 // Cap at ~15k pixels height to prevent crashes
    }
}
