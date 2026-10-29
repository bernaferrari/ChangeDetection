package com.bernaferrari.changedetection.sync

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.bernaferrari.changedetection.browser.IosPageCapture
import com.bernaferrari.changedetection.repo.ComparisonMethod
import com.bernaferrari.changedetection.repo.source.SitesRepository
import com.bernaferrari.changedetection.ui.settings.SettingsViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** One check on iOS: screenshot pages are captured with WebKit, text pages are fetched over HTTP. */
class IosCheckRunner(
    private val sites: SitesRepository,
    private val engine: SyncEngine,
    private val preferences: DataStore<Preferences>,
) : CheckRunner {
    private val locks = PageCheckLocks()
    private val batchLock = Mutex()
    override suspend fun check(siteId: String): SyncResult = locks.withLock(siteId) {
        val site = sites.getSiteById(siteId) ?: return@withLock SyncResult.Skipped(siteId, "Page deleted")
        if (!site.isSyncEnabled) return@withLock SyncResult.Skipped(siteId, "Page paused")
        val settings = preferences.data.first()
        val defaultMethod = ComparisonMethod.entries.firstOrNull { it.name == settings[SettingsViewModel.COMPARISON_METHOD_KEY] } ?: ComparisonMethod.SCREENSHOT
        if (site.captureMode != com.bernaferrari.changedetection.repo.CaptureMode.TEXT && site.captureMode.resolve(defaultMethod) == ComparisonMethod.HTML) return@withLock engine.syncSite(siteId)
        try {
            val page = IosPageCapture.capturePage(site.url, settings[SettingsViewModel.BROWSER_DELAY_KEY] ?: DEFAULT_SETTLE_MS, hide = site.textExcludeSelector, only = site.textIncludeSelector, text = site.captureMode == com.bernaferrari.changedetection.repo.CaptureMode.TEXT)
            page.icon?.let { sites.updateIcon(site.id, it) }
            engine.processFetchResult(site, SyncEngine.FetchResult.Success(page.contentType, page.bytes, capturedWithBrowser = true, validateCapture = page.validateCapture), site.threshold)
        } catch (timeout: kotlinx.coroutines.TimeoutCancellationException) {
            kotlinx.coroutines.currentCoroutineContext().ensureActive()
            engine.processFetchResult(site, SyncEngine.FetchResult.Error("Page capture timed out", timeout), site.threshold)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            engine.processFetchResult(site, SyncEngine.FetchResult.Error(error.message ?: "Could not capture the page", error), site.threshold)
        }
    }

    /** Every active page, one at a time (WebKit captures are heavy). */
    suspend fun checkAll(onlyDueAfterMinutes: Long? = null): Int = batchLock.withLock {
        val now = com.bernaferrari.changedetection.util.getCurrentTimeMillis()
        val due = sites.getSites().filter { site ->
            site.isSyncEnabled && (onlyDueAfterMinutes == null || now < site.timestamp || now - site.timestamp >= onlyDueAfterMinutes * 60_000L)
        }
        // Foreground catch-up and background refresh share durable progress. A slow first page
        // must not monopolize every short background window after process recreation.
        val cursor = preferences.data.first()[BATCH_CURSOR]
        checkResumableBatch(due, cursor, saveCursor = { next ->
            preferences.edit { saved ->
                if (next == null) saved.remove(BATCH_CURSOR) else saved[BATCH_CURSOR] = next
            }
        }, check = ::check)
    }

    private companion object {
        val BATCH_CURSOR = stringPreferencesKey("ios_monitoring_batch_cursor")
        const val DEFAULT_SETTLE_MS = 2_500L
    }
}
