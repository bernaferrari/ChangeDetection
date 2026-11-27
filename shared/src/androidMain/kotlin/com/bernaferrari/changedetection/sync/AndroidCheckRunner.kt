package com.bernaferrari.changedetection.sync

import com.bernaferrari.changedetection.diagnostics.*
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.bernaferrari.changedetection.repo.ComparisonMethod
import com.bernaferrari.changedetection.repo.source.SitesRepository
import com.bernaferrari.changedetection.ui.settings.SettingsViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withTimeout

/** One awaited check shared by manual, scheduled and Turbo entry points. */
class AndroidCheckRunner(
    private val sites: SitesRepository,
    private val engine: SyncEngine,
    private val screenshots: AndroidScreenshotSyncer,
    private val preferences: DataStore<Preferences>,
) : CheckRunner {
    // At most two live WebViews across manual, scheduled and Quick Monitor checks.
    private val capacity = Semaphore(2)

    override suspend fun check(siteId: String): SyncResult = engine.checkPage(siteId) {
        val site = sites.getSiteById(siteId) ?: return@checkPage SyncResult.Skipped(siteId, "Page deleted")
        if (!site.isSyncEnabled) return@checkPage SyncResult.Skipped(siteId, "Page paused")
        recordDiagnostic(site.captureMode, site.textIncludeSelector.isNotBlank(), site.textExcludeSelector.isNotBlank(), DiagnosticTrigger.CHECK, site.id) {
            try {
                withTimeout(90_000) {
                    capacity.withPermit {
                        val stored = preferences.data.first()[SettingsViewModel.COMPARISON_METHOD_KEY]
                        val defaultMethod = ComparisonMethod.entries.firstOrNull { it.name == stored } ?: ComparisonMethod.SCREENSHOT
                        if (site.captureMode != com.bernaferrari.changedetection.repo.CaptureMode.TEXT && site.captureMode.resolve(defaultMethod) == ComparisonMethod.HTML) {
                            syncSource()
                        } else {
                            screenshots.syncSites(listOf(site), save = ::save).single()
                        }
                    }
                }
            } catch (error: TimeoutCancellationException) {
                currentCoroutineContext().ensureActive()
                timeout(site, error)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                save(site, SyncEngine.FetchResult.Error("Page check failed", error))
            }
        }
    }
}
