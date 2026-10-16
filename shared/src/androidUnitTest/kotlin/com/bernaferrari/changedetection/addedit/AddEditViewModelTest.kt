package com.bernaferrari.changedetection.addedit

import com.bernaferrari.changedetection.repo.PageIconShape

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.bernaferrari.changedetection.background.MonitoringManager
import com.bernaferrari.changedetection.background.SyncJobInfo
import com.bernaferrari.changedetection.repo.Site
import com.bernaferrari.changedetection.repo.Snap
import com.bernaferrari.changedetection.repo.source.SitesDataSource
import com.bernaferrari.changedetection.repo.source.SitesRepository
import com.bernaferrari.changedetection.repo.ContentTypeInfo
import com.bernaferrari.changedetection.repo.source.SnapsDataSource
import com.bernaferrari.changedetection.repo.source.SnapsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class AddEditViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test fun `saving an editor after its page was deleted does not recreate it`() = runTest {
        val source = FakeSitesDataSource().apply { savedSite = existingSite("page", "https://example.com") }
        val editor = createViewModel(siteId = "page", dataSource = source)
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()
        editor.setTitle("Updated title")
        source.deleteSite("page")
        editor.saveSite()
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()
        assertEquals(null, source.savedSite)
        assertEquals(AddEditError.SiteNotFound, editor.uiState.value.saveError)
        assertFalse(editor.uiState.value.saveComplete)
    }

    @Test
    fun `icon shape stays a draft until save and restores when editing`() = runTest {
        val source = FakeSitesDataSource()
        val editor = createViewModel(dataSource = source)
        editor.setUrl("example.com")
        editor.setIconShape(com.bernaferrari.changedetection.repo.PageIconShape.CLOVER)
        editor.setIconColor(com.bernaferrari.changedetection.repo.PageIconColor.TEAL.colors)
        assertEquals(null, source.savedSite)
        editor.saveSite()
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()
        assertEquals(com.bernaferrari.changedetection.repo.PageIconShape.CLOVER, source.savedSite?.iconShape)
        val reopened = createViewModel(siteId = requireNotNull(source.savedSite).id, dataSource = source)
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()
        assertEquals(com.bernaferrari.changedetection.repo.PageIconShape.CLOVER, reopened.uiState.value.iconShape)
        assertEquals(com.bernaferrari.changedetection.repo.PageIconColor.TEAL.colors, reopened.uiState.value.selectedColors)
        reopened.setIconColor(com.bernaferrari.changedetection.repo.PageIconColor.BLUE.colors)
        assertEquals(com.bernaferrari.changedetection.repo.PageIconColor.TEAL.colors, source.savedSite?.colors)
        reopened.setIconShape(com.bernaferrari.changedetection.repo.PageIconShape.CIRCLE)
        assertEquals(com.bernaferrari.changedetection.repo.PageIconShape.CLOVER, source.savedSite?.iconShape)
        reopened.saveSite()
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()
        assertEquals(com.bernaferrari.changedetection.repo.PageIconShape.CIRCLE, source.savedSite?.iconShape)
        assertEquals(com.bernaferrari.changedetection.repo.PageIconColor.BLUE.colors, source.savedSite?.colors)
    }

    @Test
    fun `region edits persist only after save and restore on reopening`() = runTest {
        val source = FakeSitesDataSource()
        val editor = createViewModel(dataSource = source)
        editor.setUrl("example.com")
        val region = com.bernaferrari.changedetection.detection.TextRegion(" #price ", ".timer")
        editor.setTextRegion(region)
        assertEquals(null, source.savedSite)
        editor.saveSite()
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()
        assertEquals("#price", source.savedSite?.textIncludeSelector)
        assertEquals(".timer", source.savedSite?.textExcludeSelector)
        val reopened = createViewModel(siteId = requireNotNull(source.savedSite).id, dataSource = source)
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()
        assertEquals(region.copy(include = "#price"), reopened.uiState.value.textRegion)
    }

    @Test
    fun `invalid region prevents save and preview before doing work`() = runTest {
        val source = FakeSitesDataSource()
        var previews = 0
        val editor = createViewModel(
            dataSource = source,
            previewer = CapturePreviewer { _, _ ->
                previews++
                CapturePreview.Html("Unused", 0)
            },
        )
        editor.setUrl("example.com")
        editor.setTextRegion(com.bernaferrari.changedetection.detection.TextRegion("["))
        editor.saveSite()
        editor.requestPreview()
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()
        assertTrue(editor.uiState.value.regionError)
        assertEquals(null, source.savedSite)
        assertEquals(0, previews)
        editor.setTextRegion(com.bernaferrari.changedetection.detection.TextRegion("#price"))
        assertFalse(editor.uiState.value.regionError)
    }

    @Test
    fun `renaming an older monitor preserves its byte filter`() = runTest {
        val source = FakeSitesDataSource().apply {
            savedSite = existingSite("page", "https://example.com").copy(threshold = 512)
        }
        val editor = createViewModel(siteId = "page", dataSource = source)
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()
        assertEquals(512, editor.uiState.value.legacyThreshold)
        editor.setTitle("New name")
        editor.saveSite()
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()
        assertEquals(512, source.savedSite?.threshold)
        assertEquals("New name", source.savedSite?.title)
    }

    @Test
    fun `removing the old byte filter stays a draft until saved`() = runTest {
        val source = FakeSitesDataSource().apply {
            savedSite = existingSite("page", "https://example.com").copy(threshold = 512)
        }
        val editor = createViewModel(siteId = "page", dataSource = source)
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()
        editor.removeLegacyThreshold()
        assertEquals(0, editor.uiState.value.legacyThreshold)
        assertEquals(512, source.savedSite?.threshold)
        val cancelledDraft = createViewModel(siteId = "page", dataSource = source)
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()
        assertEquals(512, cancelledDraft.uiState.value.legacyThreshold)
        editor.saveSite()
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()
        assertEquals(0, source.savedSite?.threshold)
        assertEquals(com.bernaferrari.changedetection.repo.CaptureMode.DEFAULT, source.savedSite?.captureMode)
    }

    @Test
    fun `selecting an explicit rule clears legacy filtering only on save`() = runTest {
        val source = FakeSitesDataSource().apply {
            savedSite = existingSite("page", "https://example.com").copy(threshold = 512)
        }
        val editor = createViewModel(siteId = "page", dataSource = source)
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()
        editor.setCaptureMode(com.bernaferrari.changedetection.repo.CaptureMode.SOURCE)
        assertEquals(512, source.savedSite?.threshold)
        editor.saveSite()
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()
        assertEquals(0, source.savedSite?.threshold)
        assertEquals(com.bernaferrari.changedetection.repo.CaptureMode.SOURCE, source.savedSite?.captureMode)
    }

    @Test
    fun `new monitor defaults to screenshots where supported and stores that rule`() = runTest {
        val source = FakeSitesDataSource()
        val editor = createViewModel(dataSource = source)
        val expected = if (com.bernaferrari.changedetection.repo.supportsVisualMonitoring) com.bernaferrari.changedetection.repo.CaptureMode.SCREENSHOT else com.bernaferrari.changedetection.repo.CaptureMode.TEXT
        assertEquals(expected, editor.uiState.value.captureMode)
        editor.setUrl("example.com")
        editor.saveSite()
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()
        assertEquals(expected, source.savedSite?.captureMode)
        assertEquals(0, source.savedSite?.threshold)
    }

    @Test
    fun `preview timeout leaves loading state and offers another attempt`() = runTest {
        val editor = createViewModel(
            previewer = CapturePreviewer { _, _ ->
                kotlinx.coroutines.withTimeout(100) { kotlinx.coroutines.delay(200) }
                CapturePreview.Html("Too late", 8)
            },
        )
        editor.setUrl("example.com")
        editor.requestPreview()
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()
        assertTrue(editor.uiState.value.preview is CapturePreviewState.Failed)
    }

    @Test
    fun `preview uses the draft URL and mode without saving a monitor`() = runTest {
        val source = FakeSitesDataSource()
        var requested: Pair<String, com.bernaferrari.changedetection.repo.CaptureMode>? = null
        val editor = createViewModel(
            dataSource = source,
            previewer = CapturePreviewer { url, mode ->
                requested = url to mode
                CapturePreview.Html("Available now", 20)
            },
        )
        editor.setUrl("example.com/product")
        editor.setCaptureMode(com.bernaferrari.changedetection.repo.CaptureMode.HTML)
        editor.requestPreview()
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()
        assertEquals("https://example.com/product" to com.bernaferrari.changedetection.repo.CaptureMode.HTML, requested)
        assertTrue(editor.uiState.value.preview is CapturePreviewState.Ready)
        assertEquals(null, source.savedSite)
        assertFalse(editor.uiState.value.saveComplete)
    }

    @Test
    fun `switching back to a format shows its preview again without reloading the page`() = runTest {
        val captures = mutableListOf<com.bernaferrari.changedetection.repo.CaptureMode>()
        val editor = createViewModel(
            previewer = CapturePreviewer { _, mode ->
                captures += mode
                CapturePreview.Html(mode.name, 10)
            },
        )
        editor.setUrl("example.com")
        editor.setCaptureMode(com.bernaferrari.changedetection.repo.CaptureMode.TEXT)
        editor.requestPreview()
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()
        editor.setCaptureMode(com.bernaferrari.changedetection.repo.CaptureMode.HTML)
        editor.requestPreview()
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()
        editor.setCaptureMode(com.bernaferrari.changedetection.repo.CaptureMode.TEXT)
        // Shown at once, before any new request.
        assertEquals(CapturePreviewState.Ready(CapturePreview.Html("TEXT", 10)), editor.uiState.value.preview)
        editor.requestPreview()
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()
        assertEquals(listOf(com.bernaferrari.changedetection.repo.CaptureMode.TEXT, com.bernaferrari.changedetection.repo.CaptureMode.HTML), captures)
    }

    @Test
    fun `late preview cannot replace a sample for the edited URL`() = runTest {
        val editor = createViewModel(
            previewer = CapturePreviewer { url, _ ->
                if (url.contains("old")) kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) { kotlinx.coroutines.delay(1_000) }
                CapturePreview.Html(url, 10)
            },
        )
        editor.setUrl("old.example.com")
        editor.requestPreview()
        editor.setUrl("new.example.com")
        assertEquals(CapturePreviewState.Idle, editor.uiState.value.preview)
        editor.requestPreview()
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()
        assertEquals(CapturePreviewState.Ready(CapturePreview.Html("https://new.example.com", 10)), editor.uiState.value.preview)
        editor.dismissPreview()
        assertEquals(CapturePreviewState.Idle, editor.uiState.value.preview)
    }

    @Test
    fun `preview failure can be retried and changing method clears the sample`() = runTest {
        var attempts = 0
        val editor = createViewModel(
            previewer = CapturePreviewer { _, _ ->
                if (++attempts == 1) error("Offline")
                CapturePreview.Html("Recovered", 10)
            },
        )
        editor.setUrl("example.com")
        editor.requestPreview()
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()
        assertEquals(CapturePreviewState.Failed(PreviewFailure.UNAVAILABLE), editor.uiState.value.preview)
        editor.requestPreview()
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()
        assertTrue(editor.uiState.value.preview is CapturePreviewState.Ready)
        editor.setCaptureMode(com.bernaferrari.changedetection.repo.CaptureMode.HTML)
        assertEquals(CapturePreviewState.Idle, editor.uiState.value.preview)
    }

    @Test
    fun `capture choice stays a draft until saved and survives reopening`() = runTest {
        val source = FakeSitesDataSource()
        val editor = createViewModel(dataSource = source)
        editor.setUrl("example.com")
        editor.setCaptureMode(com.bernaferrari.changedetection.repo.CaptureMode.HTML)
        assertEquals(null, source.savedSite)
        editor.saveSite()
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()
        assertEquals(com.bernaferrari.changedetection.repo.CaptureMode.HTML, source.savedSite?.captureMode)
        val reopened = createViewModel(siteId = requireNotNull(source.savedSite).id, dataSource = source)
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()
        assertEquals(com.bernaferrari.changedetection.repo.CaptureMode.HTML, reopened.uiState.value.captureMode)
    }

    @Test
    fun `generated title follows URL until the user edits it`() {
        val viewModel = createViewModel()

        viewModel.setUrl("e")
        assertEquals("", viewModel.uiState.value.title)
        assertEquals(AddEditError.InvalidUrl, viewModel.uiState.value.urlError)

        viewModel.setUrl("example.com")
        assertEquals("example.com", viewModel.uiState.value.title)
        assertEquals(null, viewModel.uiState.value.urlError)

        viewModel.setTitle("Important page")
        viewModel.setUrl("www.example.org/news")
        assertEquals("Important page", viewModel.uiState.value.title)
    }

    @Test
    fun `missing monitor finishes loading with a useful error`() = runTest {
        val viewModel = createViewModel(siteId = "missing")

        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(AddEditError.SiteNotFound, viewModel.uiState.value.saveError)
    }

    @Test
    fun `successful persistence completes even if initial scheduling fails`() = runTest {
        val dataSource = FakeSitesDataSource()
        val viewModel = createViewModel(
            dataSource = dataSource,
            monitoringManager = FakeMonitoringManager(throwOnReload = true),
        )
        viewModel.setUrl("example.com")

        viewModel.saveSite()
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.saveComplete)
        assertEquals(dataSource.savedSite, viewModel.uiState.value.savedSite)
        assertFalse(viewModel.uiState.value.isSaving)
        assertEquals("https://example.com", dataSource.savedSite?.url)
        assertEquals("example.com", dataSource.savedSite?.title)
    }

    @Test
    fun `canonical duplicate is rejected before saving`() = runTest {
        val dataSource = FakeSitesDataSource().apply {
            savedSite = existingSite("saved-page", "https://example.com/").copy(captureMode = com.bernaferrari.changedetection.repo.CaptureMode.TEXT)
        }
        val viewModel = createViewModel(dataSource = dataSource)

        viewModel.setCaptureMode(com.bernaferrari.changedetection.repo.CaptureMode.TEXT)
        viewModel.setUrl("HTTPS://EXAMPLE.COM:443/#details")
        viewModel.saveSite()
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

        assertEquals("saved-page", viewModel.uiState.value.duplicateSiteId)
        assertFalse(viewModel.uiState.value.saveComplete)
        assertEquals("saved-page", dataSource.savedSite?.id)
    }

    @Test fun `new draft uses the saved text default and preserves explicit changes`() = runTest {
        val source = FakeSitesDataSource()
        val initial = androidx.datastore.preferences.core.preferencesOf(com.bernaferrari.changedetection.ui.settings.SettingsViewModel.COMPARISON_METHOD_KEY to com.bernaferrari.changedetection.repo.ComparisonMethod.HTML.name)
        val store = object : DataStore<Preferences> {
            override val data = flowOf(initial)
            override suspend fun updateData(transform: suspend (Preferences) -> Preferences) = transform(initial)
        }
        val editor = createViewModel(dataSource = source, preferences = store)
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()
        assertEquals(com.bernaferrari.changedetection.repo.CaptureMode.TEXT, editor.uiState.value.captureMode)
        editor.setUrl("localhost")
        editor.saveSite()
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()
        assertEquals(com.bernaferrari.changedetection.repo.CaptureMode.TEXT, source.savedSite?.captureMode)
        editor.setCaptureMode(com.bernaferrari.changedetection.repo.CaptureMode.SOURCE)
        assertEquals(com.bernaferrari.changedetection.repo.CaptureMode.SOURCE, editor.uiState.value.captureMode)
    }

    @Test fun `late preference loading cannot overwrite the users chosen mode`() = runTest {
        val ready = kotlinx.coroutines.CompletableDeferred<Preferences>()
        val store = object : DataStore<Preferences> {
            override val data = kotlinx.coroutines.flow.flow { emit(ready.await()) }
            override suspend fun updateData(transform: suspend (Preferences) -> Preferences) = transform(ready.await())
        }
        val editor = createViewModel(preferences = store)
        assertTrue(editor.uiState.value.isLoading)
        editor.setCaptureMode(com.bernaferrari.changedetection.repo.CaptureMode.SOURCE)
        ready.complete(androidx.datastore.preferences.core.preferencesOf(com.bernaferrari.changedetection.ui.settings.SettingsViewModel.COMPARISON_METHOD_KEY to com.bernaferrari.changedetection.repo.ComparisonMethod.HTML.name))
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()
        assertEquals(com.bernaferrari.changedetection.repo.CaptureMode.SOURCE, editor.uiState.value.captureMode)
        assertFalse(editor.uiState.value.isLoading)
    }

    @Test fun `browser hash routes identify distinct saved pages`() = runTest {
        val source = FakeSitesDataSource().apply { savedSite = existingSite("first", "https://example.com/#/product/a").copy(captureMode = com.bernaferrari.changedetection.repo.CaptureMode.SCREENSHOT) }
        val editor = createViewModel(dataSource = source)
        editor.setCaptureMode(com.bernaferrari.changedetection.repo.CaptureMode.SCREENSHOT)
        editor.setUrl("https://example.com/#/product/b")
        editor.saveSite()
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()
        assertTrue(editor.uiState.value.saveComplete)
        assertEquals(null, editor.uiState.value.duplicateSiteId)
    }

    @Test fun `a ready snapshot becomes the new page's first version instead of loading it again`() = runTest {
        val kept = mutableListOf<String>()
        val previewer = object : CapturePreviewer {
            override suspend fun capture(url: String, mode: com.bernaferrari.changedetection.repo.CaptureMode) = CapturePreview.Html("Sample", 6)
            override suspend fun keepAsFirstVersion(site: Site, preview: CapturePreview): Boolean {
                kept += requireNotNull(site.id)
                return true
            }
        }
        val manager = FakeMonitoringManager()
        val source = FakeSitesDataSource()
        val editor = createViewModel(dataSource = source, monitoringManager = manager, previewer = previewer)
        editor.setUrl("example.com")
        editor.requestPreview()
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()
        editor.saveSite()
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()
        assertEquals(listOf(requireNotNull(source.savedSite?.id)), kept)
        assertEquals(emptyList<String>(), manager.reloaded)
    }

    @Test fun `the browser capture survives naming edits and is saved without another capture`() = runTest {
        val visible = CapturePreview.Html("Only in the signed-in browser", 30)
        var kept: CapturePreview? = null
        val previewer = object : CapturePreviewer {
            override suspend fun capture(url: String, mode: com.bernaferrari.changedetection.repo.CaptureMode): CapturePreview = error("Must reuse the browser capture")
            override suspend fun keepAsFirstVersion(site: Site, preview: CapturePreview): Boolean {
                kept = preview
                return true
            }
        }
        val manager = FakeMonitoringManager()
        val editor = createViewModel(previewer = previewer, monitoringManager = manager)
        editor.setUrl("example.com")
        editor.requestLivePreview { visible }
        editor.setTitle("My page")
        editor.setIconColor(0x123456 to 0x654321)
        editor.saveSite()
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()
        org.junit.Assert.assertSame(visible, kept)
        assertTrue(editor.uiState.value.saveComplete)
        assertTrue(manager.reloaded.isEmpty())
    }

    @Test fun `changing the address or region invalidates the browser capture`() = runTest {
        val editor = createViewModel()
        editor.setUrl("example.com")
        editor.requestLivePreview { CapturePreview.Html("Old page", 8) }
        editor.setUrl("other.example.com")
        assertEquals(CapturePreviewState.Idle, editor.uiState.value.preview)
        editor.requestLivePreview { CapturePreview.Html("Whole page", 10) }
        editor.setTextRegion(com.bernaferrari.changedetection.detection.TextRegion("#price"))
        assertEquals(CapturePreviewState.Idle, editor.uiState.value.preview)
    }

    @Test fun `a pending live preview leaves the draft editable and is cancelled by an address change`() = runTest {
        val editor = createViewModel()
        editor.setUrl("example.com")
        editor.requestLivePreview {
            kotlinx.coroutines.delay(1_000)
            CapturePreview.Html("Old capture", 11)
        }
        assertEquals(CapturePreviewState.Loading, editor.uiState.value.preview)
        assertFalse(editor.uiState.value.isSaving)
        editor.setTitle("My new name")
        assertEquals("My new name", editor.uiState.value.title)
        editor.setUrl("new.example.com")
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()
        assertEquals(CapturePreviewState.Idle, editor.uiState.value.preview)
        assertEquals("new.example.com", editor.uiState.value.url)
        assertEquals("My new name", editor.uiState.value.title)
    }

    @Test fun `unsupported live capture falls back to the configured preview without blocking review`() = runTest {
        val fallback = CapturePreview.Html("Fetched source", 14)
        val editor = createViewModel(previewer = CapturePreviewer { _, _ -> fallback })
        editor.setUrl("example.com")
        editor.requestLivePreview { null }
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()
        org.junit.Assert.assertSame(fallback, (editor.uiState.value.preview as CapturePreviewState.Ready).content)
    }

    @Test fun `starting while the live preview is pending saves that capture without a second load`() = runTest {
        val ready = kotlinx.coroutines.CompletableDeferred<CapturePreview>()
        val visible = CapturePreview.Html("Visible browser state", 21)
        var kept: CapturePreview? = null
        val previewer = object : CapturePreviewer {
            override suspend fun capture(url: String, mode: com.bernaferrari.changedetection.repo.CaptureMode): CapturePreview = error("Must not load another page")
            override suspend fun keepAsFirstVersion(site: Site, preview: CapturePreview): Boolean {
                kept = preview
                return true
            }
        }
        val manager = FakeMonitoringManager()
        val editor = createViewModel(previewer = previewer, monitoringManager = manager)
        editor.setUrl("example.com")
        val thumbnail = CapturePreview.Image(byteArrayOf(1), isPreparing = true)
        editor.requestLivePreview { onPreview ->
            onPreview(thumbnail)
            ready.await()
        }
        org.junit.Assert.assertSame(thumbnail, (editor.uiState.value.preview as CapturePreviewState.Ready).content)
        editor.saveSite()
        assertTrue(editor.uiState.value.isSaving)
        assertFalse(editor.uiState.value.saveComplete)
        ready.complete(visible)
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()
        assertTrue(editor.uiState.value.saveComplete)
        org.junit.Assert.assertSame(visible, kept)
        assertTrue(manager.reloaded.isEmpty())
    }

    @Test fun `save does not finish before the visible preview has been committed`() = runTest {
        val committed = kotlinx.coroutines.CompletableDeferred<Unit>()
        val previewer = object : CapturePreviewer {
            override suspend fun capture(url: String, mode: com.bernaferrari.changedetection.repo.CaptureMode) = CapturePreview.Html("Visible page", 12)
            override suspend fun keepAsFirstVersion(site: Site, preview: CapturePreview): Boolean {
                committed.await()
                return true
            }
        }
        val manager = FakeMonitoringManager()
        val editor = createViewModel(previewer = previewer, monitoringManager = manager)
        editor.setUrl("example.com")
        editor.requestPreview()
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()
        editor.saveSite()
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()
        try {
            assertFalse("Do not navigate to an empty history while its baseline is still being saved", editor.uiState.value.saveComplete)
            assertTrue(editor.uiState.value.isSaving)
        } finally {
            committed.complete(Unit)
        }
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()
        assertTrue(editor.uiState.value.saveComplete)
        assertFalse(editor.uiState.value.isSaving)
        assertTrue(manager.reloaded.isEmpty())
    }

    @Test fun `saving before the snapshot is ready falls back to a regular first check`() = runTest {
        val manager = FakeMonitoringManager()
        val source = FakeSitesDataSource()
        val editor = createViewModel(dataSource = source, monitoringManager = manager, previewer = CapturePreviewer { _, _ -> kotlinx.coroutines.awaitCancellation() })
        editor.setUrl("example.com")
        editor.requestPreview()
        editor.saveSite()
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()
        assertTrue(editor.uiState.value.saveComplete)
        assertEquals(listOf(requireNotNull(source.savedSite?.id)), manager.reloaded)
    }

    private fun createViewModel(
        siteId: String? = null,
        dataSource: FakeSitesDataSource = FakeSitesDataSource(),
        monitoringManager: MonitoringManager = FakeMonitoringManager(),
        previewer: CapturePreviewer = CapturePreviewer { _, _ -> CapturePreview.Html("Sample", 6) },
        preferences: DataStore<Preferences> = object : DataStore<Preferences> {
            override val data = flowOf(androidx.datastore.preferences.core.emptyPreferences())
            override suspend fun updateData(transform: suspend (Preferences) -> Preferences) = transform(androidx.datastore.preferences.core.emptyPreferences())
        },
    ) = AddEditViewModel(
        siteId,
        SitesRepository(dataSource),
        previewer,
        monitoringManager,
        preferences,
    )

    private fun existingSite(id: String, url: String) = Site(
        title = "Existing page",
        url = url,
        timestamp = 0L,
        tags = "",
        id = id,
        colors = 0 to 0,
    )

    /** Minimal inert snaps source — AddEdit only touches snaps when pruning after a URL change. */
    private object EmptySnapsDataSource : SnapsDataSource {
        override fun observeStorageBytes() = kotlinx.coroutines.flow.flowOf(0L)
        override fun observeOlderVersionBytes() = kotlinx.coroutines.flow.flowOf(0L)
        override suspend fun clearOlderVersions() = Unit
        override suspend fun deleteSiteWithSnaps(siteId: String) = Unit
        override suspend fun getContentTypeInfo(siteId: String) = emptyList<ContentTypeInfo>()
        override suspend fun getMostRecentSnap(siteId: String) = null
        override suspend fun getAllMostRecentSnaps() = emptyMap<String, Snap>()
        override fun getSnaps(siteId: String) = flowOf(emptyList<Snap>())
        override fun observeSnapsTable() = flowOf(false)
        override fun getSnapsFiltered(siteId: String, filter: String) = flowOf(emptyList<Snap>())
        override suspend fun getAllSnapsForSite(siteId: String) = emptyList<Snap>()
        override suspend fun getSnapById(snapId: String) = null
        override suspend fun getSnapContent(snapId: String) = ByteArray(0)
        override suspend fun getLastNSnapsForSiteId(siteId: String, limit: Int) = null
        override suspend fun getSnapPair(originalId: String, newId: String) = null
        override suspend fun deleteSnap(snapId: String) {}
        override suspend fun pruneSnaps(siteId: String) {}
        override suspend fun getSingleSnapPair(id: String) = ByteArray(0)
        override suspend fun getLastContentHash(siteId: String) = null
        override suspend fun getLastProcessedContentHash(siteId: String) = null
    }

    private class FakeSitesDataSource : SitesDataSource {
        var savedSite: Site? = null

        override suspend fun getSites(): List<Site> = listOfNotNull(savedSite)

        override fun observeWatchlist() = flowOf(emptyList<com.bernaferrari.changedetection.repo.SiteAndLastSnap>())

        override fun getDataWithChanges(): Flow<List<Site>> = flowOf(listOfNotNull(savedSite))

        override suspend fun getSiteById(siteId: String): Site? = savedSite?.takeIf { it.id == siteId }

        override suspend fun getSiteByUrl(siteUrl: String): Site? = savedSite?.takeIf { it.url == siteUrl }

        override suspend fun saveSite(site: Site, resetHistory: Boolean, requireExisting: Boolean) {
            if (requireExisting && getSiteById(site.id) == null) throw com.bernaferrari.changedetection.repo.source.SiteNotFoundException(site.id)
            savedSite = site
        }

        override suspend fun updateIconShape(siteId: String, shape: PageIconShape) {
            getSiteById(siteId)?.let { saveSite(it.copy(iconShape = shape), false) }
        }

        override suspend fun updateIconColor(siteId: String, colors: com.bernaferrari.changedetection.repo.ColorGroup) {
            getSiteById(siteId)?.let { saveSite(it.copy(colors = colors), false) }
        }

        override suspend fun setMonitoringEnabled(siteId: String, enabled: Boolean) {
            savedSite = savedSite?.copy(isSyncEnabled = enabled)
        }

        override suspend fun setNotificationsEnabled(siteId: String, enabled: Boolean) {
            savedSite = savedSite?.copy(isNotificationEnabled = enabled)
        }

        override suspend fun undoSiteRead(siteId: String, snapId: String): Boolean = false

        override suspend fun markSiteRead(siteId: String, snapId: String): Boolean {
            savedSite = savedSite?.takeIf { it.id == siteId }?.copy(isRead = true) ?: savedSite
            return true
        }

        override suspend fun deleteAllSites() {
            savedSite = null
        }

        override suspend fun deleteSite(siteId: String) {
            if (savedSite?.id == siteId) savedSite = null
        }
    }

    private class FakeMonitoringManager(
        private val throwOnReload: Boolean = false,
    ) : MonitoringManager {
        override val quickMonitorState = kotlinx.coroutines.flow.MutableStateFlow(com.bernaferrari.changedetection.background.TurboState())
        override fun getSyncStatusFlow(): Flow<List<SyncJobInfo>> = flowOf(emptyList())

        val reloaded = mutableListOf<String>()

        override fun reloadSite(site: Site) {
            if (throwOnReload) error("scheduler unavailable")
            reloaded += requireNotNull(site.id)
        }

        override fun startQuickMonitor(siteIds: List<String>, intervalSeconds: Int, durationMinutes: Int) = Unit

        override fun stopQuickMonitor() = Unit

        override suspend fun updateSchedule() = Unit

        override fun cancelWork() = Unit
    }

    class MainDispatcherRule(
        val dispatcher: TestDispatcher = UnconfinedTestDispatcher(),
    ) : TestWatcher() {
        override fun starting(description: Description) {
            Dispatchers.setMain(dispatcher)
        }

        override fun finished(description: Description) {
            Dispatchers.resetMain()
        }
    }
}
