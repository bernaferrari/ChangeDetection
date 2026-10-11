/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
@file:OptIn(ExperimentalTestApi::class)

package com.bernaferrari.changedetection.review

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.unit.Density
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.bernaferrari.changedetection.ChangeDetectionRootApp
import com.bernaferrari.changedetection.PlatformActions
import com.bernaferrari.changedetection.background.MonitoringManager
import com.bernaferrari.changedetection.background.MonitoringSchedule
import com.bernaferrari.changedetection.di.ChangeDetectionKoinApp
import com.bernaferrari.changedetection.repo.CaptureMode
import com.bernaferrari.changedetection.repo.Site
import com.bernaferrari.changedetection.repo.Snap
import com.bernaferrari.changedetection.repo.source.local.ChangeDatabase
import com.bernaferrari.changedetection.ui.theme.ThemeViewModel
import com.bernaferrari.changedetection.util.ApplicationScope
import com.bernaferrari.changedetection.util.FileStorage
import io.ktor.client.HttpClient
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Font
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files
import javax.imageio.ImageIO
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.koin.core.context.stopKoin
import org.koin.plugin.module.dsl.startKoin

/**
 * Renders the real app at phone size over realistic local fixtures and saves one PNG per screen
 * to build/desktop-review/v6. Nothing here touches the network.
 */
class DesignReviewTest {
    @Test fun lightTour() = tour(dark = false)

    /** Fast loop for visual work: the main screens in light and dark, each step independent. */
    @Test fun quickTour() {
        for (dark in listOf(false, true)) {
            withApp(dark, seed = true) {
                val p = if (dark) "q-dark-" else "q-"
                fun step(name: String, block: () -> Unit) {
                    block()
                    shot(p + name)
                }
                fun home() {
                    repeat(3) { if (onAllNodesWithTag("home-list").fetchSemanticsNodes().isEmpty()) runCatching { onNodeWithContentDescription("Navigate back").performClick() } }
                    if (onAllNodesWithTag("home-list").fetchSemanticsNodes().isEmpty()) onNodeWithText("Home").performClick()
                    waitForTag("home-list")
                }
                waitForText("new changes", substring = true)
                runCatching { waitForText("of the page changed", substring = true, timeout = 15_000) }
                step("01-home") {}
                step("02-home-pages") { onNodeWithTag("home-list").performScrollToKey("row-exam") }
                step("11-detail") {
                    onNodeWithTag("home-list").performScrollToKey("row-studio")
                    onAllNodesWithText("Studio Display").onLast().performClick()
                    runCatching { waitForText("of the page", substring = true, timeout = 8_000) }
                }
                step("12-detail-settings") { onNodeWithTag("page-details").performScrollToKey("watch") }
                home()
                step("10-detail-failing") {
                    onNodeWithTag("home-list").performScrollToKey("row-exam")
                    onAllNodesWithText("Exam results").onLast().performClick()
                    waitForIdle()
                }
                home()
                step("13-compare-text") {
                    onNodeWithTag("home-list").performScrollToKey("card-fellowship")
                    onAllNodesWithText("Fellowship guide").onFirst().performClick()
                    runCatching { waitForText("Change 1 of", substring = true, timeout = 8_000) }
                }
                home()
                step("15-compare-image") {
                    onNodeWithTag("home-list").performScrollToKey("card-studio")
                    onAllNodesWithText("Studio Display").onFirst().performClick()
                    runCatching { waitForText("Before", timeout = 15_000) }
                }
                home()
                step("08-activity") {
                    onNodeWithText("Activity").performClick()
                    runCatching { waitForText("changed", substring = true, timeout = 10_000) }
                }
                step("20-settings") { onNodeWithText("Settings").performClick() }
                step("21-checks") {
                    onNodeWithText("Automatic checks").performClick()
                    waitForText("Every", timeout = 5_000)
                }
                runCatching { onNodeWithText("Save").performClick() }
                home()
                step("30-add-url") {
                    onNodeWithContentDescription("Add page").performClick()
                    waitForText("What should we watch?")
                }
                step("31-add-review") {
                    onNodeWithText("Page address").performTextInput(fixtureUrl)
                    onNodeWithText("Open page").performClick()
                    waitForText("Applications are open", substring = true, timeout = 15_000)
                }
                runCatching { onNodeWithContentDescription("Navigate back").performClick() }
                runCatching { onNodeWithContentDescription("Close").performClick() }
                home()
                step("05-turbo-setup") {
                    onNodeWithContentDescription("Turbo").performClick()
                    waitForText("Stop after")
                }
            }
        }
    }

    @Test fun darkTour() = tour(dark = true)

    @Test fun largeTextTour() = tour(dark = true, fontScale = 1.8f)

    @Test fun wideReview() = withApp(dark = false, seed = true, width = 1680, height = 1050, density = 1.25f) {
        waitForText("new changes", substring = true)
        shot("wide-01-home")
        onNodeWithTag("home-list").performScrollToKey("row-fellowship")
        onAllNodesWithText("Fellowship guide").onLast().performClick()
        waitForText("Latest change")
        shot("wide-11-detail")
        onNodeWithText("Compare", substring = false).performClick()
        waitForLabel("Changes")
        waitForText("Change 1 of", substring = true)
        shot("wide-13-reader")
        onNodeWithText("Settings", substring = false).performClick()
        waitForText("Appearance")
        shot("wide-20-settings")
    }

    @Test fun firstRun() = withApp(dark = false, seed = false) {
        waitForText("Watch a page")
        shot("00-empty")
    }

    private fun tour(dark: Boolean, fontScale: Float = 1f) = withApp(dark, seed = true, fontScale = fontScale) {
        val p = if (fontScale > 1f) {
            "large-"
        } else if (dark) {
            "dark-"
        } else {
            ""
        }
        waitForText("new changes", substring = true)
        runCatching { waitForText("of the page changed", substring = true, timeout = 15_000) }
        shot("${p}01-home")
        onNodeWithTag("home-list").performScrollToKey("card-fellowship")
        onAllNodesWithText("Fellowship guide").onFirst().performClick()
        waitForLabel("Changes")
        runCatching { waitForText("Change 1 of", substring = true, timeout = 8_000) }
        shot("${p}13-compare-text")
        onNodeWithTag("version-navigator").performClick()
        waitForText("Compare versions")
        shot("${p}14-versions")
        onAllNodes(hasClickAction() and hasAnyAncestor(hasTestTag("version-list"))).onFirst().performClick()
        waitForIdle()
        onNodeWithContentDescription("Navigate back").performClick()
        waitForTag("home-list")
        onNodeWithTag("home-list").performScrollToKey("card-studio")
        onAllNodesWithText("Studio Display").onFirst().performClick()
        waitForLabel("Slider")
        runCatching { waitForText("Before", timeout = 15_000) }
        shot("${p}15-compare-image")
        onNodeWithText("Slider").performClick()
        waitUntil(timeoutMillis = 15_000) { onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsActions.SetProgress)).fetchSemanticsNodes().isNotEmpty() }
        shot("${p}15b-compare-slider")
        onNodeWithText("Page").performClick()
        waitUntil(timeoutMillis = 15_000) { onAllNodesWithTag("highlight-changes").fetchSemanticsNodes().isNotEmpty() }
        shot("${p}16-compare-highlight")
        onNodeWithContentDescription("Navigate back").performClick()
        waitForTag("home-list")
        onNodeWithContentDescription("Turbo").performClick()
        waitForText("Stop after")
        onAllNodesWithText("Exam results").onLast().performScrollTo().performClick()
        onAllNodesWithText("Studio Display").onLast().performScrollTo().performClick()
        shot("${p}05-turbo-setup")
        onNodeWithText("Start Turbo", substring = true).performClick()
        waitForText("Every 30 seconds", timeout = 10_000)
        shot("${p}06-turbo-live")
        onNodeWithText("Stop Turbo").performClick()
        waitForText("Done")
        shot("${p}07-turbo-ended")
        onNodeWithText("Done").performClick()
        waitForTag("home-list")
        onNodeWithText("Activity").performClick()
        runCatching { waitForText("changed", substring = true, timeout = 10_000) }
        shot("${p}08-activity")
        onNodeWithText("Settings").performClick()
        waitForIdle()
        shot("${p}20-settings")
        onNodeWithText("Automatic checks").performClick()
        waitForText("Every", timeout = 5_000)
        shot("${p}21-checks")
        onNodeWithText("Save").performClick()
        waitForIdle()
        onNodeWithText("Home").performClick()
        waitForTag("home-list")
        onNodeWithTag("home-list").performScrollToKey("row-exam")
        shot("${p}02-home-pages")
        onAllNodesWithText("Exam results").onLast().performScrollTo().performClick()
        waitForIdle()
        shot("${p}10-detail-failing")
        onNodeWithContentDescription("Navigate back").performClick()
        waitForTag("home-list")
        onNodeWithTag("home-list").performScrollToKey("row-studio")
        onAllNodesWithText("Studio Display").onLast().performClick()
        waitForTag("page-details")
        onNodeWithTag("page-details").performScrollToKey("latest")
        waitForText("Latest change")
        runCatching { waitForText("of the page", substring = true, timeout = 8_000) }
        shot("${p}11-detail")
        onNodeWithTag("page-details").performScrollToKey("watch")
        shot("${p}12-detail-settings")
        onNodeWithContentDescription("Navigate back").performClick()
        waitForTag("home-list")
        onNodeWithContentDescription("Add page").performClick()
        waitForText("What should we watch?")
        shot("${p}30-add-url")
        onNodeWithText("Page address").performTextInput(fixtureUrl)
        onNodeWithText("Open page").performClick()
        waitForText("Applications are open", substring = true, timeout = 15_000)
        shot("${p}31-add-review")
        onNodeWithText("Start watching").performClick()
        runCatching { waitForText("Now watching", substring = true, timeout = 15_000) }
        shot("${p}32-added")
        waitForIdle()
        waitForTag("home-list")
        onNodeWithTag("home-list").performScrollToKey("row-roles")
        onAllNodesWithText("Design roles").onLast().performClick()
        waitForText("First version saved")
        shot("${p}17-detail-first")
        onNodeWithText("View saved version").performClick()
        runCatching { waitForText("No open roles", substring = true, timeout = 8_000) }
        shot("${p}18-single-version")
        onNodeWithContentDescription("Navigate back").performClick()
        onNodeWithContentDescription("Navigate back").performClick()
        onNodeWithText("Settings").performClick()
        waitForText("About")
        onNodeWithText("About").performScrollTo().performClick()
        waitForIdle()
        shot("${p}23-about")
        onNodeWithTag("about-list").performScrollToKey("licenses")
        onNodeWithText("Open-source licenses").performClick()
        waitForIdle()
        shot("${p}23b-licenses")
        onAllNodes(hasClickAction() and (hasText("Close") or hasText("Done") or hasContentDescription("Close"))).onFirst().performClick()
        waitForIdle()
        onNodeWithContentDescription("Navigate back").performClick()
        onNodeWithText("How it works").performScrollTo().performClick()
        waitForIdle()
        shot("${p}24-how")
        onNodeWithContentDescription("Navigate back").performClick()
        waitForText("Appearance")
        onNodeWithText("Color").performScrollTo()
        waitForIdle()
        shot("${p}22-appearance")
    }

    private var fixtureUrl = ""

    private fun withApp(dark: Boolean, seed: Boolean, fontScale: Float = 1f, width: Int = WIDTH_PX, height: Int = HEIGHT_PX, density: Float = DENSITY, block: ComposeUiTest.() -> Unit) {
        val server = com.sun.net.httpserver.HttpServer.create(java.net.InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/open-call") { exchange ->
            val bytes = String(fellowship("30 November", "€22,000")).encodeToByteArray()
            exchange.responseHeaders.add("Content-Type", "text/html; charset=utf-8")
            exchange.sendResponseHeaders(200, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        server.start()
        fixtureUrl = "http://127.0.0.1:${server.address.port}/open-call"
        val directory = Files.createTempDirectory("design-review").toFile()
        val previous = System.getProperty("changedetection.dataDir")
        System.setProperty("changedetection.dataDir", directory.path)
        val owner = object : ViewModelStoreOwner {
            override val viewModelStore = ViewModelStore()
        }
        val lifecycleOwner = object : LifecycleOwner {
            override val lifecycle = LifecycleRegistry.createUnsafe(this).apply { currentState = Lifecycle.State.RESUMED }
        }
        val application = startKoin<ChangeDetectionKoinApp> { }
        val koin = application.koin
        val db = koin.get<ChangeDatabase>()
        try {
            runBlocking {
                koin.get<DataStore<Preferences>>().edit {
                    it[ThemeViewModel.THEME_MODE_KEY] = if (dark) "DARK" else "LIGHT"
                    it[MonitoringSchedule.ENABLED] = true
                }
                if (seed) seedFixtures(db, koin.get())
            }
            runDesktopComposeUiTest(width = width, height = height, testTimeout = kotlin.time.Duration.parse("4m")) {
                setContent {
                    CompositionLocalProvider(
                        LocalViewModelStoreOwner provides owner,
                        LocalLifecycleOwner provides lifecycleOwner,
                        LocalDensity provides Density(density, fontScale),
                    ) {
                        Box(Modifier.fillMaxSize()) { CompositionLocalProvider(com.bernaferrari.changedetection.addedit.LocalBrowserAvailable provides false, com.bernaferrari.changedetection.ui.kit.LocalPageIcons provides false) { ChangeDetectionRootApp(PlatformActions(appVersion = "6.0")) } }
                    }
                }
                try {
                    block()
                } catch (failure: Throwable) {
                    runCatching { shot("zz-failure") }
                    throw failure
                }
            }
        } finally {
            owner.viewModelStore.clear()
            koin.get<MonitoringManager>().cancelWork()
            koin.get<ApplicationScope>().scope.cancel()
            koin.get<HttpClient>().close()
            db.close()
            stopKoin()
            server.stop(0)
            if (previous == null) System.clearProperty("changedetection.dataDir") else System.setProperty("changedetection.dataDir", previous)
            directory.deleteRecursively()
        }
    }

    private fun ComposeUiTest.waitForText(text: String, substring: Boolean = false, timeout: Long = 10_000) {
        waitUntil(timeoutMillis = timeout) { onAllNodesWithText(text, substring = substring).fetchSemanticsNodes().isNotEmpty() }
    }

    /** Controls can expose visible labels or an icon content description. */
    private fun ComposeUiTest.waitForLabel(label: String, timeout: Long = 10_000) {
        waitUntil(timeoutMillis = timeout) { onAllNodes(hasText(label) or hasContentDescription(label)).fetchSemanticsNodes().isNotEmpty() }
    }

    private fun ComposeUiTest.waitForTag(tag: String) {
        waitUntil(timeoutMillis = 8_000) { onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }
        waitForIdle()
    }

    private fun ComposeUiTest.shot(name: String) {
        waitForIdle()
        val file = File("../build/desktop-review/v6/$name.png")
        file.parentFile.mkdirs()
        ImageIO.write(onAllNodes(isRoot()).onLast().captureToImage().toAwtImage(), "png", file)
    }

    private companion object {
        const val DENSITY = 2.625f
        const val WIDTH_PX = 1080
        const val HEIGHT_PX = 2340
    }
}

private const val MINUTE = 60_000L
private const val HOUR = 60 * MINUTE

private suspend fun seedFixtures(db: ChangeDatabase, files: FileStorage) {
    val now = System.currentTimeMillis()
    val sites = db.siteDao()
    val snaps = db.snapsDao()
    suspend fun page(id: String, title: String, url: String, mode: CaptureMode = CaptureMode.TEXT, configure: (Site) -> Site = { it }) {
        val base = Site(title, url, now - 4 * MINUTE, "", id, 0 to 0)
            .copy(captureMode = mode, isSuccessful = true, lastSuccessfulCheck = now - 4 * MINUTE, isRead = true)
        sites.insertSite(configure(base))
    }
    suspend fun version(siteId: String, at: Long, type: String, bytes: ByteArray) {
        val id = "$siteId-$at"
        files.writeBytes(id, bytes)
        snaps.insertSnap(Snap(id, siteId, at, type, "UTF-8", bytes.size))
    }
    val html = "text/html"

    page("studio", "Studio Display", "https://store.example/display", CaptureMode.SCREENSHOT) { it.copy(isRead = false) }
    version("studio", now - 3 * 24 * HOUR, "image/png", storePage("\$1,599.00", "Ships in 2–3 weeks"))
    version("studio", now - 38 * MINUTE, "image/png", storePage("\$1,349.00", "In stock · free delivery"))

    page("fellowship", "Fellowship guide", "https://opencall.example/fellowship") { it.copy(isRead = false) }
    version("fellowship", now - 2 * 24 * HOUR, html, fellowship("31 October", "€18,000"))
    version("fellowship", now - 2 * HOUR, html, fellowship("14 November", "€20,000"))

    page("apartments", "Apartment listings", "https://homes.example/rent?city=lisbon") { it.copy(isRead = false) }
    version("apartments", now - 5 * HOUR, html, listings(0))
    version("apartments", now - 11 * MINUTE, html, listings(7))

    page("exam", "Exam results", "https://campus.example/results") {
        it.copy(isSuccessful = false, lastFailureCode = "SERVER", timestamp = now - 6 * MINUTE, lastSuccessfulCheck = now - 2 * HOUR)
    }
    version("exam", now - 3 * 24 * HOUR, html, "<html><body><h1>Results</h1><p>Results will be published at 12:00.</p></body></html>".encodeToByteArray())

    page("conference", "Conference program", "https://offscript.example/program")
    version("conference", now - 9 * 24 * HOUR, html, "<html><body><h1>The program</h1><p>Keynote: to be announced</p></body></html>".encodeToByteArray())
    version("conference", now - 26 * HOUR, html, "<html><body><h1>The program</h1><p>Keynote: Ana Ruiz</p></body></html>".encodeToByteArray())

    page("release", "Release notes", "https://fieldnotes.example/changelog")
    version("release", now - 12 * 24 * HOUR, html, "<html><body><h2>3.1</h2><p>Bug fixes.</p></body></html>".encodeToByteArray())
    version("release", now - 3 * 24 * HOUR, html, "<html><body><h2>3.2</h2><p>Offline mode.</p><h2>3.1</h2><p>Bug fixes.</p></body></html>".encodeToByteArray())

    page("roles", "Design roles", "https://forma.example/jobs")
    version("roles", now - 6 * 24 * HOUR, html, "<html><body><h1>Open roles</h1><p>No open roles right now.</p></body></html>".encodeToByteArray())

    page("poster", "Event poster", "https://studiogrid.example/poster") { it.copy(isSyncEnabled = false) }
    version("poster", now - 20 * 24 * HOUR, html, "<html><body><h1>Poster</h1></body></html>".encodeToByteArray())
}

private fun fellowship(deadline: String, amount: String) = """
    <html><body><main>
    <h1>Open Call Fellowship 2027</h1>
    <p>Applications are open to independent researchers, designers and writers working on public-interest projects.</p>
    <h2>Key dates</h2>
    <ul><li>Applications close $deadline, 23:59 CET</li><li>Interviews in January</li></ul>
    <h2>Funding</h2>
    <p>Each fellow receives $amount over six months, plus a travel budget.</p>
    <h2>Eligibility</h2>
    <p>Open to anyone, anywhere. Previous fellows may apply again after two years.</p>
    </main></body></html>
""".trimIndent().encodeToByteArray()

private fun listings(seed: Int): ByteArray {
    val streets = listOf("Rua Augusta", "Avenida da Liberdade", "Rua do Carmo", "Travessa do Fala-Só", "Rua da Rosa", "Calçada do Combro", "Rua de São Bento", "Largo do Rato", "Rua Garrett", "Rua dos Fanqueiros", "Rua Nova do Almada", "Rua da Prata")
    val items = (0 until 12).joinToString("") { i ->
        val n = (i + seed) % streets.size
        "<li><h3>${streets[n]} ${10 + n}</h3><p>T${1 + (n % 3)} · ${900 + n * 75} € / month · ${40 + n * 6} m²</p></li>"
    }
    return "<html><body><h1>Rentals in Lisbon</h1><ul>$items</ul></body></html>".encodeToByteArray()
}

/** A tiny product page drawn with AWT, so screenshot pages have real pixels to compare. */
private fun storePage(price: String, stock: String): ByteArray {
    val w = 720
    val h = 1400
    val image = BufferedImage(w, h, BufferedImage.TYPE_INT_RGB)
    val g = image.createGraphics()
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
    g.color = Color.WHITE
    g.fillRect(0, 0, w, h)
    g.color = Color(0xEEEEEE)
    g.fillRect(0, 88, w, 2)
    g.color = Color(0x1D1D1D)
    g.font = Font(Font.SANS_SERIF, Font.BOLD, 30)
    g.drawString("store", 40, 58)
    g.color = Color(0xE3E8EE)
    g.fillRoundRect(40, 130, w - 80, 420, 28, 28)
    g.color = Color(0xB9C3CD)
    g.stroke = BasicStroke(6f)
    g.drawRoundRect(300, 280, 120, 100, 18, 18)
    g.color = Color(0x1D1D1D)
    g.font = Font(Font.SANS_SERIF, Font.BOLD, 44)
    g.drawString("Studio Display", 40, 640)
    g.color = Color(0x777777)
    g.font = Font(Font.SANS_SERIF, Font.PLAIN, 24)
    g.drawString("Standard glass · Tilt-adjustable stand", 40, 684)
    g.color = Color(0x1D1D1D)
    g.font = Font(Font.SANS_SERIF, Font.BOLD, 56)
    g.drawString(price, 40, 790)
    g.color = Color(0x444444)
    g.font = Font(Font.SANS_SERIF, Font.PLAIN, 26)
    g.drawString(stock, 40, 836)
    g.color = Color(0x1D1D1D)
    g.fillRoundRect(40, 880, w - 80, 76, 76, 76)
    g.color = Color.WHITE
    g.font = Font(Font.SANS_SERIF, Font.BOLD, 26)
    g.drawString("Add to bag", w / 2 - 66, 928)
    g.color = Color(0xEEEEEE)
    for (i in 0..2) g.fillRoundRect(40 + i * 220, 1040, 200, 200, 20, 20)
    g.dispose()
    return ByteArrayOutputStream().use {
        ImageIO.write(image, "png", it)
        it.toByteArray()
    }
}
