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
                    androidx.compose.runtime.CompositionLocalProvider(com.bernaferrari.changedetection.addedit.LocalBrowserAvailable provides false, com.bernaferrari.changedetection.ui.kit.LocalPageIcons provides false) { ChangeDetectionRootApp(PlatformActions(appVersion = "Journey test")) }
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
            compose.onNodeWithContentDescription("Name").performTextReplacement("Journey schedule")
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
            // Labelled with text when it stands alone, or as the toolbar's check FAB when changes can be stepped through.
            compose.onAllNodes((hasText("Mark read") or hasContentDescription("Mark read")) and hasClickAction()).onLast().performClick()
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
            // Enter the actual edit destination: a component-injected callback cannot prove route wiring.
            val duplicate = page.copy(id = "duplicate", title = "Other page", url = "http://127.0.0.1:${server.address.port}/duplicate")
            runBlocking { db.siteDao().insertSite(duplicate) }
            compose.waitUntil(10_000) { compose.onAllNodes(hasText("Journey schedule") and hasClickAction()).fetchSemanticsNodes().isNotEmpty() }
            compose.onAllNodes(hasText("Journey schedule") and hasClickAction()).onLast().performClick()
            compose.waitUntil(10_000) { compose.onAllNodesWithContentDescription("Edit").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithContentDescription("Edit").performClick()
            compose.waitUntil(10_000) { compose.onAllNodesWithText("Edit page").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("Page address").performTextReplacement(duplicate.url)
            compose.onNodeWithText("Save").performClick()
            compose.onNodeWithText("Keep history").performClick()
            compose.onNodeWithText("Continue").performClick()
            compose.waitUntil(10_000) { compose.onAllNodesWithText("You're already monitoring this page").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("Open").performScrollTo().performClick()
            compose.waitUntil(10_000) { compose.onAllNodes(hasTestTag("page-name") and hasText("Other page")).fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithTag("page-name").assertTextEquals("Other page")
            compose.onNodeWithText("Edit page").assertDoesNotExist()
            assertEquals(page.url, runBlocking { db.siteDao().getSiteById(page.id)?.url })
            // Exercise the same repository through both real preview consumers, not just the comparison screen.
            val beforeBytes = flatPng(0xff000000.toInt())
            val afterBytes = flatPng(0xffffffff.toInt())
            val beforeId = java.util.UUID.randomUUID().toString()
            val afterId = java.util.UUID.randomUUID().toString()
            val now = System.currentTimeMillis()
            runBlocking {
                val files = application.koin.get<com.bernaferrari.changedetection.util.FileStorage>()
                db.siteDao().insertSite(page.copy(id = "visual", title = "Visual probe", url = "https://example.test/visual", captureMode = com.bernaferrari.changedetection.repo.CaptureMode.SCREENSHOT, timestamp = now, isRead = false))
                files.writeBytes(beforeId, beforeBytes)
                files.writeBytes(afterId, afterBytes)
                db.snapsDao().insertSnap(com.bernaferrari.changedetection.repo.Snap(beforeId, "visual", now - 10, "image/png", "UTF-8", beforeBytes.size))
                db.snapsDao().insertSnap(com.bernaferrari.changedetection.repo.Snap(afterId, "visual", now, "image/png", "UTF-8", afterBytes.size))
            }
            compose.onNodeWithText("Home").performClick()
            compose.waitUntil(10_000) { compose.onAllNodesWithText("Before").fetchSemanticsNodes().isNotEmpty() && compose.onAllNodesWithText("After").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("No visual change detected").assertDoesNotExist()
            compose.onNodeWithText("Activity").performClick()
            compose.waitUntil(10_000) { compose.onAllNodesWithText("of the page", substring = true).fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("No visual change detected").assertDoesNotExist()
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

    private fun flatPng(color: Int): ByteArray {
        val image = java.awt.image.BufferedImage(64, 64, java.awt.image.BufferedImage.TYPE_INT_ARGB)
        for (y in 0 until 64) for (x in 0 until 64) image.setRGB(x, y, color)
        return java.io.ByteArrayOutputStream().use { output ->
            ImageIO.write(image, "png", output)
            image.flush()
            output.toByteArray()
        }
    }
}
