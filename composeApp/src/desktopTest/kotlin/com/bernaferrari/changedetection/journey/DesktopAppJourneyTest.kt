/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.journey

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.bernaferrari.changedetection.ChangeDetectionRootApp
import com.bernaferrari.changedetection.PlatformActions
import com.bernaferrari.changedetection.background.MonitoringManager
import com.bernaferrari.changedetection.di.ChangeDetectionKoinApp
import com.bernaferrari.changedetection.repo.source.local.ChangeDatabase
import com.bernaferrari.changedetection.util.ApplicationScope
import com.sun.net.httpserver.HttpServer
import io.ktor.client.HttpClient
import java.io.File
import java.net.InetSocketAddress
import java.nio.file.Files
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import javax.imageio.ImageIO
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.koin.core.context.stopKoin
import org.koin.plugin.module.dsl.startKoin

class DesktopAppJourneyTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun addBaselineRefreshCompareAndAcknowledgeThroughTheActualApp() {
        val directory = Files.createTempDirectory("desktop-app-journey").toFile()
        val previousDirectory = System.getProperty("changedetection.dataDir")
        System.setProperty("changedetection.dataDir", directory.path)
        val content = AtomicReference("<html><body><h1>Program</h1><p>Opening session 09:00</p></body></html>")
        val requests = AtomicInteger()
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/program") { exchange ->
            requests.incrementAndGet()
            val bytes = content.get().encodeToByteArray()
            exchange.responseHeaders.add("Content-Type", "text/html; charset=utf-8")
            exchange.sendResponseHeaders(200, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        server.start()
        val owner = object : ViewModelStoreOwner {
            override val viewModelStore = ViewModelStore()
        }
        val lifecycleOwner = object : LifecycleOwner {
            override val lifecycle = LifecycleRegistry.createUnsafe(this).apply { currentState = Lifecycle.State.RESUMED }
        }
        val application = startKoin<ChangeDetectionKoinApp> { }
        val db = application.koin.get<ChangeDatabase>()
        try {
            compose.setContent {
                CompositionLocalProvider(LocalViewModelStoreOwner provides owner, LocalLifecycleOwner provides lifecycleOwner) {
                    androidx.compose.runtime.CompositionLocalProvider(com.bernaferrari.changedetection.addedit.LocalBrowserAvailable provides false) { ChangeDetectionRootApp(PlatformActions(appVersion = "Journey test")) }
                }
            }
            compose.waitForIdle()
            compose.waitUntil(10_000) { compose.onAllNodesWithText("Watch a page").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("Watch a page").performClick()
            compose.onNodeWithText("Page address").performTextInput("http://127.0.0.1:${server.address.port}/program")
            compose.onNodeWithText("Open page").performClick()
            compose.waitUntil(10_000) { compose.onAllNodesWithText("Opening session 09:00", substring = true).fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("Opening session 09:00", substring = true).assertIsDisplayed()
            capture("app-journey-setup-preview")
            compose.onNodeWithText("Name").performTextReplacement("Journey schedule")
            compose.onNode(hasText("Start watching") and hasClickAction()).performClick()
            compose.waitUntil(10_000) { compose.onAllNodesWithText("Now watching", substring = true).fetchSemanticsNodes().isNotEmpty() }
            val page = runBlocking { db.siteDao().getSitesList().single() }
            val first = runBlocking { requireNotNull(db.snapsDao().getLastSnapForSiteId(page.id)) }
            assertTrue(requests.get() >= 1)
            capture("app-journey-baseline")
            content.set("<html><body><h1>Program</h1><p>Opening session 10:00</p></body></html>")
            compose.onNodeWithContentDescription("More options").performClick()
            compose.onNodeWithContentDescription("Refresh all").performClick()
            compose.waitUntil(10_000) {
                runBlocking { db.snapsDao().getLastSnapForSiteId(page.id)?.snapId != first.snapId }
            }
            compose.waitUntil(10_000) { compose.onAllNodesWithText("1 new change").fetchSemanticsNodes().isNotEmpty() }
            // The "Now watching" message also names the page; pick the clickable list row.
            compose.onAllNodes(hasText("Journey schedule") and hasClickAction()).onLast().performClick()
            compose.waitUntil(10_000) { compose.onAllNodesWithText("Latest change").fetchSemanticsNodes().isNotEmpty() }
            assertFalse(runBlocking { requireNotNull(db.siteDao().getSiteById(page.id)).isRead })
            capture("app-journey-detail")
            compose.onAllNodesWithText("Compare").onFirst().performClick()
            compose.waitUntil(10_000) { compose.onAllNodesWithText("10:00", substring = true).fetchSemanticsNodes().isNotEmpty() }
            assertFalse(runBlocking { requireNotNull(db.siteDao().getSiteById(page.id)).isRead })
            compose.onAllNodesWithText("09:00", substring = true).onFirst().assertExists()
            capture("app-journey-comparison")
            compose.onAllNodesWithText("Mark read").onLast().performClick()
            compose.waitUntil(10_000) { runBlocking { db.siteDao().getSiteById(page.id)?.isRead == true } }
            assertEquals(2, runBlocking { db.snapsDao().getAllSnapsForSiteId(page.id).size })
            compose.onNodeWithText("Activity").performClick()
            compose.onNodeWithText("Home").performClick()
            compose.waitUntil(10_000) { compose.onAllNodesWithContentDescription("Add page").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithContentDescription("Add page").performClick()
            compose.onNodeWithText("Page address").assertIsDisplayed()
            compose.onNodeWithContentDescription("Close").performClick()
            compose.onNodeWithText("Page address").assertDoesNotExist()
            assertEquals(1, runBlocking { db.siteDao().getSitesList().size })
        } finally {
            compose.runOnIdle { owner.viewModelStore.clear() }
            application.koin.get<MonitoringManager>().cancelWork()
            application.koin.get<ApplicationScope>().scope.cancel()
            application.koin.get<HttpClient>().close()
            db.close()
            stopKoin()
            server.stop(0)
            if (previousDirectory == null) System.clearProperty("changedetection.dataDir") else System.setProperty("changedetection.dataDir", previousDirectory)
            directory.deleteRecursively()
        }
    }

    private fun capture(name: String) {
        val file = File("../build/desktop-review/$name.png")
        file.parentFile.mkdirs()
        ImageIO.write(compose.onAllNodes(isRoot()).onLast().captureToImage().toAwtImage(), "png", file)
    }
}
