package com.bernaferrari.changedetection.sync

import com.bernaferrari.changedetection.diagnostics.*
import android.content.Context
import android.view.ContextThemeWrapper
import android.webkit.WebView
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.bernaferrari.changedetection.browser.AndroidPageCapture
import com.bernaferrari.changedetection.browser.AndroidWebViewStartup
import com.bernaferrari.changedetection.browser.configureChangeDetectionWebView
import com.bernaferrari.changedetection.browser.destroySafely
import com.bernaferrari.changedetection.repo.Site
import com.bernaferrari.changedetection.ui.settings.SettingsViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

sealed interface ScreenshotCaptureOutcome {
    data class Success(
        val contentType: String,
        val bytes: ByteArray,
        val title: String? = null,
        val icon: String? = null,
        val validateCapture: suspend () -> Unit = {},
    ) : ScreenshotCaptureOutcome

    data class Failure(
        val message: String,
        val error: Throwable? = null,
    ) : ScreenshotCaptureOutcome
}

/** Test seam around the real WebView browser session. */
interface ScreenshotBrowser : AutoCloseable {
    suspend fun capture(
        site: Site,
        delayMs: Long,
    ): ScreenshotCaptureOutcome
}

fun interface ScreenshotBrowserFactory {
    fun create(): ScreenshotBrowser
}

/**
 * Runs screenshot checks inside the WorkManager job that requested them.
 *
 * Capture, comparison, and persistence are one awaited suspend call, so
 * WorkManager cannot report success while detached work is still running.
 */
class AndroidScreenshotSyncer(
    private val syncEngine: SyncEngine,
    private val dataStore: DataStore<Preferences>,
    private val progressTracker: SyncProgressTracker,
    /** Remembers the page's icon found during a capture. */
    private val onIcon: suspend (siteId: String, iconUrl: String) -> Unit = { _, _ -> },
    private val browserFactory: ScreenshotBrowserFactory,
) {
    suspend fun syncSites(sites: List<Site>): List<SyncResult> = syncSites(sites, save = { site, result -> syncEngine.processFetchResult(site, result, site.threshold) })

    internal suspend fun syncSites(
        sites: List<Site>,
        save: suspend (Site, SyncEngine.FetchResult) -> SyncResult,
    ): List<SyncResult> {
        if (sites.isEmpty()) return emptyList()

        return withContext(Dispatchers.Main.immediate) {
            val delayMs = try {
                withContext(Dispatchers.IO) {
                    dataStore.data.first()[SettingsViewModel.BROWSER_DELAY_KEY] ?: DEFAULT_BROWSER_DELAY_MS
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                return@withContext sites.map { site ->
                    if (!site.isSyncEnabled) SyncResult.Skipped(site.id, "Sync disabled") else save(site, captureError(error))
                }
            }
            sites.map { site ->
                progressTracker.track(site.id) { captureSite(site, delayMs, save) }
            }
        }
    }

    /** Each page owns its browser, so a dead renderer cannot poison the rest of the batch. */
    private suspend fun captureSite(site: Site, delayMs: Long, save: suspend (Site, SyncEngine.FetchResult) -> SyncResult): SyncResult {
        if (!site.isSyncEnabled) return SyncResult.Skipped(site.id, "Sync disabled")
        var browser: ScreenshotBrowser? = null
        return try {
            browser = diagnosticStage(DiagnosticStage.CREATE_BROWSER) { browserFactory.create() }
            when (val capture = browser.capture(site, delayMs)) {
                is ScreenshotCaptureOutcome.Success -> {
                    capture.icon?.let { icon ->
                        try {
                            onIcon(site.id, icon)
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (error: Exception) {
                            appLog.w("Could not save the page icon", error)
                        }
                    }
                    save(site, SyncEngine.FetchResult.Success(capture.contentType, capture.bytes, capturedWithBrowser = true, validateCapture = capture.validateCapture))
                }

                is ScreenshotCaptureOutcome.Failure -> save(site, SyncEngine.FetchResult.Error(capture.message, capture.error))
            }
        } catch (timeout: kotlinx.coroutines.TimeoutCancellationException) {
            kotlinx.coroutines.currentCoroutineContext().ensureActive()
            save(site, captureError(timeout))
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            appLog.e("Screenshot check failed for ${site.id}", error)
            save(site, captureError(error))
        } catch (error: OutOfMemoryError) {
            appLog.e("Screenshot check ran out of memory for ${site.id}", error)
            save(site, captureError(error))
        } finally {
            runCatching { diagnosticStage(DiagnosticStage.CLOSE_BROWSER) { browser?.close() } }
                .onFailure { appLog.w("Failed to close screenshot browser", it) }
        }
    }

    private fun captureError(error: Throwable) = SyncEngine.FetchResult.Error(error.message ?: "Screenshot check failed", error)

    private companion object {
        const val DEFAULT_BROWSER_DELAY_MS = 5_000L
    }
}

class AndroidScreenshotBrowserFactory(context: Context) : ScreenshotBrowserFactory {
    private val appContext = context.applicationContext.also { AndroidDiagnostics.install(it) }

    override fun create(): ScreenshotBrowser {
        val themedContext = ContextThemeWrapper(appContext, android.R.style.Theme_DeviceDefault_Light)
        val webView = AndroidWebViewStartup.create(themedContext)
        try {
            webView.layout(0, 0, VIEWPORT_WIDTH, VIEWPORT_HEIGHT)
            webView.configureChangeDetectionWebView()
            return AndroidScreenshotBrowser(webView, AndroidPageCapture(webView))
        } catch (error: Throwable) {
            webView.destroySafely(clearCache = false)
            throw error
        }
    }

    private class AndroidScreenshotBrowser(
        private val webView: WebView,
        private val session: AndroidPageCapture,
    ) : ScreenshotBrowser {
        override suspend fun capture(
            site: Site,
            delayMs: Long,
        ): ScreenshotCaptureOutcome {
            val screenshot = if (site.captureMode == com.bernaferrari.changedetection.repo.CaptureMode.TEXT) {
                session.captureText(site.url, delayMs, only = site.textIncludeSelector)
            } else {
                session.capture(site.url, delayMs, MAX_HEIGHT_PIXELS, hide = site.textExcludeSelector, only = site.textIncludeSelector)
            }
            return ScreenshotCaptureOutcome.Success(screenshot.contentType, screenshot.bytes, screenshot.title, screenshot.icon, session.captureValidator())
        }

        override fun close() {
            webView.destroySafely(clearCache = false)
        }
    }

    private companion object {
        const val MAX_HEIGHT_PIXELS = 10_000
        const val VIEWPORT_WIDTH = com.bernaferrari.changedetection.browser.WatchCaptureViewport.WIDTH
        const val VIEWPORT_HEIGHT = com.bernaferrari.changedetection.browser.WatchCaptureViewport.HEIGHT
    }
}

private val appLog = com.bernaferrari.changedetection.logging.AppLog.tagged("AndroidScreenshotSyncer")
