/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.addedit

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.preferencesOf
import androidx.lifecycle.ViewModelStore
import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import com.bernaferrari.changedetection.background.*
import com.bernaferrari.changedetection.browser.TestPageServer
import com.bernaferrari.changedetection.repo.*
import com.bernaferrari.changedetection.repo.source.SitesRepository
import com.bernaferrari.changedetection.repo.source.local.*
import com.bernaferrari.changedetection.storage.SnapshotFileCleanup
import com.bernaferrari.changedetection.ui.settings.SettingsViewModel
import com.bernaferrari.changedetection.ui.theme.ChangeDetectionTheme
import com.bernaferrari.changedetection.util.AndroidFileStorage
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

class LiveBrowserWatchJourneyTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun constantTitlePushStateNavigationUsesTheLiveUrlForPreviewAndSave() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "spa-watch-${java.util.UUID.randomUUID()}.db"
        val db = getRoomDatabase(Room.databaseBuilder<ChangeDatabase>(context, context.getDatabasePath(name).absolutePath), BundledSQLiteDriver())
        val files = AndroidFileStorage(context)
        val repository = SitesRepository(SitesLocalDataSource(AppExecutors(), db.siteDao(), SnapshotFileCleanup(db.pendingFileDeletionDao(), db.snapsDao(), files)))
        val initial = preferencesOf(SettingsViewModel.COMPARISON_METHOD_KEY to ComparisonMethod.HTML.name)
        val preferences = object : DataStore<Preferences> {
            override val data = flowOf(initial)
            override suspend fun updateData(transform: suspend (Preferences) -> Preferences) = transform(initial)
        }
        val monitoring = object : MonitoringManager {
            override val quickMonitorState = MutableStateFlow(TurboState())
            override fun getSyncStatusFlow() = flowOf(emptyList<SyncJobInfo>())
            override fun reloadSite(site: Site) = Unit
            override fun startQuickMonitor(siteIds: List<String>, intervalSeconds: Int, durationMinutes: Int) = Unit
            override fun stopQuickMonitor() = Unit
            override suspend fun updateSchedule() = Unit
            override fun cancelWork() = Unit
        }
        val previewUrl = AtomicReference<String>()
        val saved = CompletableDeferred<String>()
        val store = ViewModelStore()
        val html = """<meta name="viewport" content="width=device-width,initial-scale=1"><title>Constant title</title><button id="product" onclick="history.pushState({},'', '/product/b')">Product B</button><p>Product details</p>"""
        try {
            TestPageServer(200, html).use { server ->
                val draftUrl = server.url + "catalog"
                val expected = server.url + "product/b"
                lateinit var model: AddEditViewModel
                compose.runOnUiThread {
                    model = AddEditViewModel(
                        null,
                        repository,
                        CapturePreviewer { url, _ ->
                            previewUrl.set(url)
                            CapturePreview.Html("Preview product B", 17, pageTitle = "Product B")
                        },
                        monitoring,
                        preferences,
                    )
                    store.put("draft", model)
                }
                compose.setContent {
                    ChangeDetectionTheme(darkTheme = false, dynamicColor = false) {
                        AddEditFlow(viewModel = model, onNavigateUp = {}, onSaveComplete = {}, onOpenSite = {}, onPageAdded = { id, _ -> saved.complete(id) })
                    }
                }
                compose.onNodeWithText("Page address").performTextReplacement(draftUrl)
                compose.onNodeWithText("Open page").performClick()
                val ready = AtomicBoolean(false)
                val web = AtomicReference<WebView>()
                compose.waitUntil(30_000) {
                    compose.runOnUiThread {
                        findWeb(compose.activity.window.decorView)?.let {
                            web.set(it)
                            it.evaluateJavascript("!!document.getElementById('product')") { value -> ready.set(value == "true") }
                        }
                    }
                    ready.get()
                }
                val current = AtomicReference<String>()
                compose.runOnUiThread {
                    web.get().evaluateJavascript("document.getElementById('product').click();window.location.href") { current.set(decodeBrowserUrl(it)) }
                }
                compose.waitUntil(5_000) { current.get() == expected }
                compose.onNode(hasText("Watch this page") and hasClickAction()).performClick()
                compose.waitUntil(5_000) { previewUrl.get() == expected }
                assertEquals(expected, model.uiState.value.url)
                compose.onNode(hasText("Start watching") and hasClickAction()).performClick()
                compose.waitUntil(5_000) { saved.isCompleted }
                assertEquals(expected, runBlocking { db.siteDao().getSiteById(saved.await())?.url })
            }
        } finally {
            compose.runOnUiThread { store.clear() }
            db.close()
            context.deleteDatabase(name)
        }
    }

    private fun findWeb(view: View): WebView? {
        if (view is WebView) return view
        if (view is ViewGroup) for (index in 0 until view.childCount) findWeb(view.getChildAt(index))?.let { return it }
        return null
    }
}
