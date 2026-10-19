package com.bernaferrari.changedetection.sync

import android.content.Context
import android.view.ContextThemeWrapper
import android.webkit.WebView
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.bernaferrari.changedetection.browser.AndroidPageCapture
import com.bernaferrari.changedetection.browser.configureChangeDetectionWebView
import com.bernaferrari.changedetection.browser.destroySafely
import com.bernaferrari.changedetection.repo.Site
import com.bernaferrari.changedetection.ui.settings.SettingsViewModel
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

sealed interface ScreenshotCaptureOutcome {
    data class Success(
        val contentType: String,
        val bytes: ByteArray,
        val title: String? = null,
        val icon: String? = null,
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
    suspend fun syncSites(sites: List<Site>): List<SyncResult> {
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
                    if (!site.isSyncEnabled) SyncResult.Skipped(site.id, "Sync disabled") else recordFailure(site, error)
                }
            }
            sites.map { site ->
                progressTracker.track(site.id) { captureSite(site, delayMs) }
            }
        }
    }

    /** Each page owns its browser, so a dead renderer cannot poison the rest of the batch. */
    private suspend fun captureSite(site: Site, delayMs: Long): SyncResult {
        if (!site.isSyncEnabled) return SyncResult.Skipped(site.id, "Sync disabled")
        var browser: ScreenshotBrowser? = null
        return try {
            browser = browserFactory.create()
            when (val capture = browser.capture(site, delayMs)) {
                is ScreenshotCaptureOutcome.Success -> {
                    capture.icon?.let { runCatching { onIcon(site.id, it) } }
                    syncEngine.processFetchResult(
                        site,
                        SyncEngine.FetchResult.Success(capture.contentType, capture.bytes, capturedWithBrowser = true),
                        site.threshold,
                    )
                }

                is ScreenshotCaptureOutcome.Failure -> recordFailure(site, capture.message, capture.error)
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            Napier.e("Screenshot check failed for ${site.id}", error)
            recordFailure(site, error)
        } catch (error: OutOfMemoryError) {
            Napier.e("Screenshot check ran out of memory for ${site.id}", error)
            recordFailure(site, error)
        } finally {
            runCatching { browser?.close() }
                .onFailure { Napier.w("Failed to close screenshot browser", it) }
        }
    }

    private suspend fun recordFailure(site: Site, error: Throwable): SyncResult = recordFailure(site, error.message ?: "Screenshot check failed", error)

    private suspend fun recordFailure(
        site: Site,
        message: String,
        error: Throwable? = null,
    ): SyncResult = syncEngine.processFetchResult(
        site,
        SyncEngine.FetchResult.Error(message = message, throwable = error),
        site.threshold,
    )

    private companion object {
        const val DEFAULT_BROWSER_DELAY_MS = 5_000L
    }
}

class AndroidScreenshotBrowserFactory(context: Context) : ScreenshotBrowserFactory {
    private val appContext = context.applicationContext

    override fun create(): ScreenshotBrowser {
        val themedContext = ContextThemeWrapper(appContext, android.R.style.Theme_DeviceDefault_Light)
        val webView = WebView(themedContext)
        try {
            webView.layout(0, 0, VIEWPORT_WIDTH, VIEWPORT_HEIGHT)
            webView.configureChangeDetectionWebView(overviewMode = true, acceptThirdPartyCookies = true)
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
            return ScreenshotCaptureOutcome.Success(screenshot.contentType, screenshot.bytes, screenshot.title, screenshot.icon)
        }

        override fun close() {
            webView.destroySafely(clearCache = false)
        }
    }

    private companion object {
        const val MAX_HEIGHT_PIXELS = 10_000
        const val VIEWPORT_WIDTH = 1_080
        const val VIEWPORT_HEIGHT = 1_920
    }
}
