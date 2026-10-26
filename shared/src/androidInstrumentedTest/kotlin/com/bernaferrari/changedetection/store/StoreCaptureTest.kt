@file:OptIn(ExperimentalTestApi::class)

package com.bernaferrari.changedetection.store

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import android.graphics.RectF
import android.graphics.Typeface
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.bernaferrari.changedetection.ChangeDetectionRootApp
import com.bernaferrari.changedetection.PlatformActions
import com.bernaferrari.changedetection.background.MonitoringSchedule
import com.bernaferrari.changedetection.di.initKoin
import com.bernaferrari.changedetection.repo.CaptureMode
import com.bernaferrari.changedetection.repo.Site
import com.bernaferrari.changedetection.repo.Snap
import com.bernaferrari.changedetection.repo.source.local.ChangeDatabase
import com.bernaferrari.changedetection.ui.theme.ThemeViewModel
import com.bernaferrari.changedetection.util.FileStorage
import com.bernaferrari.changedetection.shared.generated.resources.*
import com.bernaferrari.changedetection.localization.getString
import org.jetbrains.compose.resources.StringResource
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.android.ext.koin.androidContext

/**
 * Store screenshots of the real app on a dedicated emulator, driven by store-listing/capture-emulator.py.
 * The emulator's display size, density, night mode, and System UI demo mode are set by the host script;
 * this test seeds local example pages (reserved .example addresses, no network) and saves full-display
 * captures, status bar and gesture pill included, under the test app's external files directory.
 * It is skipped unless the host passes `-e store_profile`.
 */
class StoreCaptureTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val args = InstrumentationRegistry.getArguments()
    private val profile = args.getString("store_profile")
    private val dark = args.getString("store_theme") == "dark"
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val output by lazy { File(context.getExternalFilesDir(null), "store-capture/$profile/${if (dark) "dark" else "light"}").apply { deleteRecursively(); mkdirs() } }

    @Before fun freshData() {
        org.junit.Assume.assumeTrue(profile != null)
        context.deleteDatabase("changes.db")
        context.filesDir.listFiles()?.forEach { it.deleteRecursively() }
        val koin = initKoin { androidContext(context) }.koin
        runBlocking {
            koin.get<DataStore<Preferences>>().edit {
                it[ThemeViewModel.THEME_MODE_KEY] = if (dark) "DARK" else "LIGHT"
                it[MonitoringSchedule.ENABLED] = true
            }
            seed(koin.getOrNull<Any>(ChangeDatabase::class) as ChangeDatabase, koin.getOrNull<Any>(FileStorage::class) as FileStorage)
        }
    }

    @Test fun capture() {
        compose.runOnUiThread { compose.activity.enableEdgeToEdge() }
        compose.setContent { ChangeDetectionRootApp(PlatformActions(appVersion = "3.0")) }
        if (profile == "tablet10") wideTour() else tour()
    }

    private fun tour() = with(compose) {
        waitForTag("home-list")
        waitForText(text(Res.string.preview_before), timeout = 15_000)
        waitForText(text(Res.string.preview_after), timeout = 15_000)
        shot("01-home")
        onNodeWithTag("home-list").performScrollToKey("card-studio")
        onAllNodesWithText("Studio Display").onFirst().performClick()
        waitForText(text(Res.string.preview_before), timeout = 15_000)
        shot("02-compare-image")
        onNodeWithTag("version-navigator").performClick()
        waitForText(text(Res.string.reader_versions_title))
        waitUntil(timeoutMillis = 15_000) { onAllNodesWithText(text(Res.string.loading), substring = true).fetchSemanticsNodes().isEmpty() }
        shot("04-versions")
        onAllNodes(hasClickAction() and hasAnyAncestor(hasTestTag("version-list"))).onFirst().performClick()
        back()
        onNodeWithTag("home-list").performScrollToKey("card-fellowship")
        onAllNodesWithText("Fellowship guide").onFirst().performClick()
        waitForTag("comparison-lines")
        shot("03-compare-text")
        back()
        onNodeWithContentDescription(text(Res.string.turbo_mode)).performClick()
        waitForText(text(Res.string.turbo_sheet_for))
        onAllNodesWithText("Studio Display").onLast().performScrollTo().performClick()
        onAllNodesWithText("Fellowship guide").onLast().performScrollTo().performClick()
        shot("05-turbo")
        onAllNodes(hasClickAction() and (hasContentDescription(text(Res.string.a11y_close)) or hasText(text(Res.string.cancel)))).onFirst().runCatching { performClick() }
        Thread.sleep(400)
        if (onAllNodesWithText(text(Res.string.turbo_sheet_for)).fetchSemanticsNodes().isNotEmpty()) InstrumentationRegistry.getInstrumentation().uiAutomation.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK)
        waitForTag("home-list")
        onNodeWithText(text(Res.string.nav_settings)).performClick()
        waitForText(text(Res.string.settings_section_appearance))
        shot("06-appearance")
    }

    private fun wideTour() = with(compose) {
        waitForTag("home-list")
        waitForText(text(Res.string.preview_before), timeout = 15_000)
        waitForText(text(Res.string.preview_after), timeout = 15_000)
        shot("01-home")
        onNodeWithTag("home-list").performScrollToKey("card-studio")
        onAllNodesWithText("Studio Display").onFirst().performClick()
        waitForText(text(Res.string.preview_before), timeout = 15_000)
        shot("02-compare-image")
        // Wide layouts keep Home beside the detail pane, so the next page opens in place.
        onNodeWithTag("home-list").performScrollToKey("card-fellowship")
        onAllNodesWithText("Fellowship guide").onFirst().performClick()
        waitForTag("comparison-lines")
        shot("03-compare-text")
        onNodeWithContentDescription(text(Res.string.turbo_mode)).performClick()
        waitForText(text(Res.string.turbo_sheet_for))
        onAllNodesWithText("Studio Display").onLast().performScrollTo().performClick()
        onAllNodesWithText("Fellowship guide").onLast().performScrollTo().performClick()
        shot("05-turbo")
        InstrumentationRegistry.getInstrumentation().uiAutomation.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK)
        waitForTag("home-list")
        onNodeWithText(text(Res.string.nav_settings)).performClick()
        waitForText(text(Res.string.settings_section_appearance))
        shot("06-appearance")
    }

    private fun text(resource: StringResource): String = runBlocking { getString(resource) }

    private fun back() {
        compose.onNodeWithContentDescription(text(Res.string.a11y_navigate_back)).performClick()
        compose.waitForTag("home-list")
    }

    private fun ComposeTestRule.waitForText(text: String, substring: Boolean = false, timeout: Long = 10_000) =
        waitUntil(timeoutMillis = timeout) { onAllNodesWithText(text, substring = substring).fetchSemanticsNodes().isNotEmpty() }

    private fun ComposeTestRule.waitForTag(tag: String) {
        waitUntil(timeoutMillis = 8_000) { onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }
        waitForIdle()
    }

    /** The whole display, as the user sees it: system bars come from Android, not from this test. */
    private fun shot(name: String) {
        compose.waitForIdle()
        Thread.sleep(900)
        compose.waitForIdle()
        val bitmap = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        File(output, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        // Read the accessibility tree at the same settled screen as the capture. Its paths and screen
        // bounds let the shared studio move callouts to the corresponding element in another locale.
        val nodes = JSONArray()
        fun visit(node: AccessibilityNodeInfo, path: String) {
            val bounds = Rect().also(node::getBoundsInScreen)
            nodes.put(JSONObject().apply {
                put("path", path)
                put("b", JSONArray(listOf(bounds.left, bounds.top, bounds.right, bounds.bottom)))
                node.viewIdResourceName?.let { put("id", it) }
                node.text?.let { put("text", it.toString()) }
                node.contentDescription?.let { put("desc", it.toString()) }
                node.className?.let { put("cls", it.toString()) }
                node.packageName?.let { put("pkg", it.toString()) }
            })
            for (index in 0 until node.childCount) node.getChild(index)?.let { visit(it, "$path.$index") }
        }
        val root = checkNotNull(InstrumentationRegistry.getInstrumentation().uiAutomation.rootInActiveWindow)
        visit(root, "0")
        File(output, "$name.nodes.json").writeText(JSONObject().apply {
            put("source", "android-accessibility")
            put("width", bitmap.width)
            put("height", bitmap.height)
            put("nodes", nodes)
        }.toString())
    }
}

private const val MINUTE = 60_000L
private const val HOUR = 60 * MINUTE

private suspend fun seed(db: ChangeDatabase, files: FileStorage) {
    val now = System.currentTimeMillis()
    suspend fun page(id: String, title: String, url: String, mode: CaptureMode = CaptureMode.TEXT, unread: Boolean = false, checked: Long = 4 * MINUTE) {
        db.siteDao().insertSite(
            Site(title, url, now - checked, "", id, 0 to 0)
                .copy(captureMode = mode, isSuccessful = true, lastSuccessfulCheck = now - checked, isRead = !unread),
        )
    }
    suspend fun version(siteId: String, at: Long, type: String, bytes: ByteArray) {
        val id = "$siteId-$at"
        files.writeBytes(id, bytes)
        db.snapsDao().insertSnap(Snap(id, siteId, at, type, "UTF-8", bytes.size))
    }
    val html = "text/html"
    page("studio", "Studio Display", "https://store.example/display", CaptureMode.SCREENSHOT, unread = true)
    version("studio", now - 3 * 24 * HOUR, "image/png", storePage("$1,599.00", "Ships in 2–3 weeks"))
    version("studio", now - 8 * MINUTE, "image/png", storePage("$1,349.00", "In stock · free delivery"))

    page("fellowship", "Fellowship guide", "https://opencall.example/fellowship", unread = true)
    version("fellowship", now - 2 * 24 * HOUR, html, fellowship("31 October", "€18,000"))
    version("fellowship", now - 2 * HOUR, html, fellowship("14 November", "€20,000"))

    page("apartments", "Apartment listings", "https://homes.example/rent?city=lisbon", unread = true)
    version("apartments", now - 5 * HOUR, html, listings(0))
    version("apartments", now - 11 * MINUTE, html, listings(7))

    page("conference", "Conference program", "https://offscript.example/program")
    version("conference", now - 9 * 24 * HOUR, html, "<html><body><h1>The program</h1><p>Keynote: to be announced</p></body></html>".encodeToByteArray())
    version("conference", now - 26 * HOUR, html, "<html><body><h1>The program</h1><p>Keynote: Ana Ruiz</p></body></html>".encodeToByteArray())

    page("release", "Release notes", "https://fieldnotes.example/changelog")
    version("release", now - 12 * 24 * HOUR, html, "<html><body><h2>3.1</h2><p>Bug fixes.</p></body></html>".encodeToByteArray())
    version("release", now - 3 * 24 * HOUR, html, "<html><body><h2>3.2</h2><p>Offline mode.</p><h2>3.1</h2><p>Bug fixes.</p></body></html>".encodeToByteArray())
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

/** The same small product page as the desktop design review, drawn with Android's canvas. */
private fun storePage(price: String, stock: String): ByteArray {
    val w = 720
    val h = 1400
    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val c = Canvas(bitmap)
    val p = Paint(Paint.ANTI_ALIAS_FLAG)
    fun text(value: String, x: Float, y: Float, size: Float, color: Int, bold: Boolean = false) {
        p.style = Paint.Style.FILL
        p.color = color
        p.textSize = size
        p.typeface = Typeface.create(Typeface.SANS_SERIF, if (bold) Typeface.BOLD else Typeface.NORMAL)
        c.drawText(value, x, y, p)
    }
    fun box(l: Float, t: Float, r: Float, b: Float, radius: Float, color: Int, stroke: Float = 0f) {
        p.color = color
        p.style = if (stroke > 0) Paint.Style.STROKE else Paint.Style.FILL
        p.strokeWidth = stroke
        c.drawRoundRect(RectF(l, t, r, b), radius, radius, p)
    }
    c.drawColor(Color.WHITE)
    box(0f, 88f, w.toFloat(), 90f, 0f, 0xFFEEEEEE.toInt())
    text("store", 40f, 58f, 30f, 0xFF1D1D1D.toInt(), bold = true)
    box(40f, 130f, w - 40f, 550f, 14f, 0xFFE3E8EE.toInt())
    box(300f, 280f, 420f, 380f, 9f, 0xFFB9C3CD.toInt(), stroke = 6f)
    text("Studio Display", 40f, 640f, 44f, 0xFF1D1D1D.toInt(), bold = true)
    text("Standard glass · Tilt-adjustable stand", 40f, 684f, 24f, 0xFF777777.toInt())
    text(price, 40f, 790f, 56f, 0xFF1D1D1D.toInt(), bold = true)
    text(stock, 40f, 836f, 26f, 0xFF444444.toInt())
    box(40f, 880f, w - 40f, 956f, 38f, 0xFF1D1D1D.toInt())
    text("Add to bag", w / 2 - 66f, 928f, 26f, Color.WHITE, bold = true)
    for (i in 0..2) box(40f + i * 220, 1040f, 240f + i * 220, 1240f, 10f, 0xFFEEEEEE.toInt())
    return ByteArrayOutputStream().use {
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        it.toByteArray()
    }
}
