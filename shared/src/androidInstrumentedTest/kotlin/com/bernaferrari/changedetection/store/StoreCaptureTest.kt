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
import android.Manifest
import android.accessibilityservice.AccessibilityService
import com.bernaferrari.changedetection.ChangeDetectionRootApp
import com.bernaferrari.changedetection.MainActivityProvider
import com.bernaferrari.changedetection.sync.AndroidChangeNotifier
import com.bernaferrari.changedetection.sync.NotificationPreferences
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
 * this test seeds local example pages (fictional names; the host script turns the network off) and saves full-display
 * captures, status bar and gesture pill included, under the test app's external files directory.
 * It is skipped unless the host passes `-e store_profile`.
 */
class StoreCaptureTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val args = InstrumentationRegistry.getArguments()
    private val profile = args.getString("store_profile")
    private val dark = args.getString("store_theme") == "dark"
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private var studioSnap = ""
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
                // The alert scene shows the saved page in the notification, as users who opt in see it.
                it[NotificationPreferences.IMAGE_PREVIEWS] = true
            }
            studioSnap = seed(
                koin.getOrNull<Any>(ChangeDatabase::class) as ChangeDatabase,
                koin.getOrNull<Any>(FileStorage::class) as FileStorage
            )
        }
    }

    @Test fun capture() {
        compose.runOnUiThread { compose.activity.enableEdgeToEdge() }
        compose.setContent { ChangeDetectionRootApp(PlatformActions(appVersion = "3.0")) }
        if (profile == "tablet10") wideTour() else tour()
    }

    /**
     * The outcome users install for: the app's real change alert in Android's notification shade, posted by
     * the production notifier for the seeded display price change, over the app's Home screen.
     */
    private fun alert() = with(compose) {
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        automation.grantRuntimePermission(
            context.packageName,
            Manifest.permission.POST_NOTIFICATIONS
        )
        MainActivityProvider.register(ComponentActivity::class.java)
        val now = System.currentTimeMillis()
        val site =
            Site(DISPLAY, "https://northwind.shop/lumen-27", now, "", "studio", 0 to 0).copy(
                captureMode = CaptureMode.SCREENSHOT
            )
        runBlocking { AndroidChangeNotifier(context).notifyChange(site, studioSnap) }
        Thread.sleep(800)
        automation.performGlobalAction(AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS)
        Thread.sleep(2_000)
        shot("00-alert", settle = false)
        automation.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
        Thread.sleep(800)
        context.getSystemService(android.app.NotificationManager::class.java).cancelAll()
        waitForTag("home-list")
    }

    private fun tour() = with(compose) {
        waitForTag("home-list")
        waitForText(text(Res.string.preview_before), timeout = 15_000)
        waitForText(text(Res.string.preview_after), timeout = 15_000)
        shot("01-home")
        alert()
        onNodeWithTag("home-list").performScrollToKey("card-studio")
        onAllNodesWithText(DISPLAY).onFirst().performClick()
        waitForText(text(Res.string.preview_before), timeout = 15_000)
        shot("02-compare-image")
        // The whole page with the change marked, then the before/after slider: the clearest picture of what a check finds.
        onNodeWithContentDescription(text(Res.string.reader_mode_page)).performClick()
        Thread.sleep(1_200)
        shot("02c-compare-page")
        onNodeWithContentDescription(text(Res.string.reader_mode_slider)).performClick()
        Thread.sleep(1_200)
        shot("02d-compare-slider")
        onNodeWithContentDescription(text(Res.string.reader_mode_changes)).performClick()
        onNodeWithTag("version-navigator").performClick()
        waitForText(text(Res.string.reader_versions_title))
        waitUntil(timeoutMillis = 15_000) { onAllNodesWithText(text(Res.string.loading), substring = true).fetchSemanticsNodes().isEmpty() }
        shot("04-versions")
        onAllNodes(hasClickAction() and hasAnyAncestor(hasTestTag("version-list"))).onFirst().performClick()
        back()
        onNodeWithTag("home-list").performScrollToKey("card-restock")
        onAllNodesWithText(SHOES).onFirst().performClick()
        waitForText(text(Res.string.preview_before), timeout = 15_000)
        shot("02b-compare-restock")
        back()
        onNodeWithTag("home-list").performScrollToKey("card-fellowship")
        onAllNodesWithText("Fellowship guide").onFirst().performClick()
        waitForTag("comparison-lines")
        shot("03-compare-text")
        back()
        onNodeWithContentDescription(text(Res.string.turbo_mode)).performClick()
        waitForText(text(Res.string.turbo_sheet_for))
        onAllNodesWithText(DISPLAY).onLast().performScrollTo().performClick()
        onAllNodesWithText("Fellowship guide").onLast().performScrollTo().performClick()
        // The interval and duration pickers are the point of this scene: scroll the sheet to its end.
        val sheetScroll = onNode(hasScrollAction() and hasAnyAncestor(hasTestTag("turbo-setup")))
        repeat(4) { sheetScroll.performTouchInput { swipeUp(startY = bottom - 10f, endY = top + 10f) } }
        shot("05-turbo")
        onAllNodes(hasClickAction() and (hasContentDescription(text(Res.string.a11y_close)) or hasText(text(Res.string.cancel)))).onFirst().runCatching { performClick() }
        Thread.sleep(400)
        if (onAllNodesWithText(text(Res.string.turbo_sheet_for)).fetchSemanticsNodes().isNotEmpty()) InstrumentationRegistry.getInstrumentation().uiAutomation.performGlobalAction(
            AccessibilityService.GLOBAL_ACTION_BACK)
        waitForTag("home-list")
        onNodeWithText(text(Res.string.nav_settings)).performClick()
        waitForText(text(Res.string.settings_section_appearance))
        // Bring the colour style picker into view: it is what Appearance looks like now.
        onNode(hasContentDescription(text(Res.string.color_style_title))).performScrollTo()
        // Lift it clear of the bottom bar.
        onRoot().performTouchInput { swipeUp(startY = centerY, endY = centerY - 220f) }
        shot("06-appearance")
    }

    private fun wideTour() = with(compose) {
        waitForTag("home-list")
        waitForText(text(Res.string.preview_before), timeout = 15_000)
        waitForText(text(Res.string.preview_after), timeout = 15_000)
        shot("01-home")
        alert()
        onNodeWithTag("home-list").performScrollToKey("card-studio")
        onAllNodesWithText(DISPLAY).onFirst().performClick()
        waitForText(text(Res.string.preview_before), timeout = 15_000)
        shot("02-compare-image")
        // The whole page with the change marked, then the before/after slider: the clearest picture of what a check finds.
        onNodeWithContentDescription(text(Res.string.reader_mode_page)).performClick()
        Thread.sleep(1_200)
        shot("02c-compare-page")
        onNodeWithContentDescription(text(Res.string.reader_mode_slider)).performClick()
        Thread.sleep(1_200)
        shot("02d-compare-slider")
        onNodeWithContentDescription(text(Res.string.reader_mode_changes)).performClick()
        onNodeWithTag("version-navigator").performClick()
        waitForText(text(Res.string.reader_versions_title))
        waitUntil(timeoutMillis = 15_000) { onAllNodesWithText(text(Res.string.loading), substring = true).fetchSemanticsNodes().isEmpty() }
        shot("04-versions")
        InstrumentationRegistry.getInstrumentation().uiAutomation.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK)
        Thread.sleep(600)
        onNodeWithTag("home-list").performScrollToKey("card-restock")
        onAllNodesWithText(SHOES).onFirst().performClick()
        waitForText(text(Res.string.preview_before), timeout = 15_000)
        shot("02b-compare-restock")
        // Wide layouts keep Home beside the detail pane, so the next page opens in place.
        onNodeWithTag("home-list").performScrollToKey("card-fellowship")
        onAllNodesWithText("Fellowship guide").onFirst().performClick()
        waitForTag("comparison-lines")
        shot("03-compare-text")
        onNodeWithContentDescription(text(Res.string.turbo_mode)).performClick()
        waitForText(text(Res.string.turbo_sheet_for))
        onAllNodesWithText(DISPLAY).onLast().performScrollTo().performClick()
        onAllNodesWithText("Fellowship guide").onLast().performScrollTo().performClick()
        // The interval and duration pickers are the point of this scene: scroll the sheet to its end.
        val sheetScroll = onNode(hasScrollAction() and hasAnyAncestor(hasTestTag("turbo-setup")))
        repeat(4) { sheetScroll.performTouchInput { swipeUp(startY = bottom - 10f, endY = top + 10f) } }
        shot("05-turbo")
        InstrumentationRegistry.getInstrumentation().uiAutomation.performGlobalAction(
            AccessibilityService.GLOBAL_ACTION_BACK)
        waitForTag("home-list")
        onNodeWithText(text(Res.string.nav_settings)).performClick()
        waitForText(text(Res.string.settings_section_appearance))
        // Bring the colour style picker into view: it is what Appearance looks like now.
        onNode(hasContentDescription(text(Res.string.color_style_title))).performScrollTo()
        // Lift it clear of the bottom bar.
        onRoot().performTouchInput { swipeUp(startY = centerY, endY = centerY - 220f) }
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
    private fun shot(name: String, settle: Boolean = true) {
        // The shade is System UI: Compose idling does not describe it, so that scene waits on its own.
        if (settle) compose.waitForIdle()
        Thread.sleep(900)
        if (settle) compose.waitForIdle()
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

/** Fictional products and sites: natural-looking names that belong to no real brand. */
private const val DISPLAY = "Lumen 27 Display"
private const val SHOES = "Trail Runner 5"

/** Seeds the example pages and returns the newest display version, which the alert scene announces. */
private suspend fun seed(db: ChangeDatabase, files: FileStorage): String {
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
    // A real-looking price history, so Versions has a story: 1,699 → 1,649 → 1,599 → 1,349.
    page("studio", DISPLAY, "https://northwind.shop/lumen-27", CaptureMode.SCREENSHOT, unread = true, checked = 3 * MINUTE)
    version("studio", now - 9 * 24 * HOUR, "image/png", storePage("northwind", DISPLAY, "27-inch 5K · Height-adjustable stand", "$1,699.00", "Ships in 3–4 weeks"))
    version("studio", now - 6 * 24 * HOUR, "image/png", storePage("northwind", DISPLAY, "27-inch 5K · Height-adjustable stand", "$1,649.00", "Ships in 3–4 weeks"))
    version("studio", now - 3 * 24 * HOUR, "image/png", storePage("northwind", DISPLAY, "27-inch 5K · Height-adjustable stand", "$1,599.00", "Ships in 2–3 weeks"))
    version("studio", now - 3 * MINUTE, "image/png", storePage("northwind", DISPLAY, "27-inch 5K · Height-adjustable stand", "$1,349.00", "In stock · free delivery"))

    page("restock", SHOES, "https://summitgear.shop/trail-runner-5", CaptureMode.SCREENSHOT, unread = true, checked = 14 * MINUTE)
    version("restock", now - 2 * 24 * HOUR, "image/png", storePage("summit gear", SHOES, "Size 42 · Glacier blue", "$129.00", "Sold out", button = "Notify me", stockColor = 0xFFC62828.toInt(), shoe = true))
    version("restock", now - 14 * MINUTE, "image/png", storePage("summit gear", SHOES, "Size 42 · Glacier blue", "$129.00", "In stock · ships today", stockColor = 0xFF1B7F3B.toInt(), shoe = true))
    val studioNewest = "studio-${now - 3 * MINUTE}"

    page("fellowship", "Fellowship guide", "https://openfellows.org/2027", unread = true)
    version("fellowship", now - 2 * 24 * HOUR, html, fellowship("31 October", "€18,000"))
    version("fellowship", now - 2 * HOUR, html, fellowship("14 November", "€20,000"))

    page("apartments", "Apartment listings", "https://lisboahomes.com/rent", unread = true)
    version("apartments", now - 5 * HOUR, html, listings(0))
    version("apartments", now - 11 * MINUTE, html, listings(7))

    page("conference", "Conference program", "https://offscript.events/program")
    version("conference", now - 9 * 24 * HOUR, html, "<html><body><h1>The program</h1><p>Keynote: to be announced</p></body></html>".encodeToByteArray())
    version("conference", now - 26 * HOUR, html, "<html><body><h1>The program</h1><p>Keynote: Ana Ruiz</p></body></html>".encodeToByteArray())

    page("release", "Release notes", "https://fieldnotes.app/changelog")
    version("release", now - 12 * 24 * HOUR, html, "<html><body><h2>3.1</h2><p>Bug fixes.</p></body></html>".encodeToByteArray())
    version("release", now - 3 * 24 * HOUR, html, "<html><body><h2>3.2</h2><p>Offline mode.</p><h2>3.1</h2><p>Bug fixes.</p></body></html>".encodeToByteArray())
    return studioNewest
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

/** A small fictional product page, drawn with Android's canvas so screenshot pages have real pixels to compare. */
private fun storePage(
    store: String,
    product: String,
    subtitle: String,
    price: String,
    stock: String,
    button: String = "Add to bag",
    stockColor: Int = 0xFF444444.toInt(),
    shoe: Boolean = false,
): ByteArray {
    val w = 720
    val h = 1400
    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val c = Canvas(bitmap)
    val p = Paint(Paint.ANTI_ALIAS_FLAG)
    fun text(value: String, x: Float, y: Float, size: Float, color: Int, bold: Boolean = false) {
        p.shader = null
        p.style = Paint.Style.FILL
        p.color = color
        p.textSize = size
        p.typeface = Typeface.create(Typeface.SANS_SERIF, if (bold) Typeface.BOLD else Typeface.NORMAL)
        c.drawText(value, x, y, p)
    }
    fun box(l: Float, t: Float, r: Float, b: Float, radius: Float, color: Int, stroke: Float = 0f) {
        p.shader = null
        p.color = color
        p.style = if (stroke > 0) Paint.Style.STROKE else Paint.Style.FILL
        p.strokeWidth = stroke
        c.drawRoundRect(RectF(l, t, r, b), radius, radius, p)
    }
    fun gradient(l: Float, t: Float, r: Float, b: Float, radius: Float, from: Int, to: Int) {
        p.style = Paint.Style.FILL
        p.shader = android.graphics.LinearGradient(l, t, r, b, from, to, android.graphics.Shader.TileMode.CLAMP)
        c.drawRoundRect(RectF(l, t, r, b), radius, radius, p)
        p.shader = null
    }
    val ink = 0xFF1D1D1D.toInt()
    val grey = 0xFF777777.toInt()
    c.drawColor(Color.WHITE)
    // Header: the store's name, a search field, and a bag.
    text(store, 40f, 60f, 32f, ink, bold = true)
    box(300f, 26f, 610f, 72f, 23f, 0xFFF1F2F4.toInt())
    text("Search", 326f, 57f, 22f, 0xFF9A9A9A.toInt())
    box(636f, 30f, 676f, 70f, 10f, ink, stroke = 4f)
    box(0f, 96f, w.toFloat(), 98f, 0f, 0xFFEEEEEE.toInt())
    text(if (shoe) "Running  ›  Trail" else "Displays  ›  27-inch", 40f, 140f, 22f, grey)
    // The product photo, drawn.
    if (shoe) {
        gradient(40f, 164f, w - 40f, 560f, 18f, 0xFFE4F1F6.toInt(), 0xFFCFE3EC.toInt())
        box(170f, 430f, 560f, 470f, 20f, 0xFF2B3A44.toInt())
        gradient(190f, 300f, 540f, 440f, 70f, 0xFF4FA3C7.toInt(), 0xFF2E6F95.toInt())
        box(190f, 300f, 330f, 440f, 60f, 0xFF3D8DB4.toInt())
        for (i in 0..3) box(300f + i * 40, 330f, 318f + i * 40, 348f, 9f, Color.WHITE)
    } else {
        gradient(40f, 164f, w - 40f, 560f, 18f, 0xFFF0F2F6.toInt(), 0xFFDDE2EA.toInt())
        box(196f, 226f, 524f, 422f, 14f, 0xFF20232B.toInt())
        gradient(208f, 238f, 512f, 410f, 6f, 0xFF5B8CFF.toInt(), 0xFFB05BFF.toInt())
        box(346f, 422f, 374f, 482f, 0f, 0xFFB7BCC6.toInt())
        box(296f, 478f, 424f, 492f, 7f, 0xFFA9AFBA.toInt())
    }
    for (i in 0..3) box(332f + i * 18f, 532f, 342f + i * 18f, 542f, 5f, if (i == 0) ink else 0xFFC4C8CF.toInt())
    text(product, 40f, 636f, 44f, ink, bold = true)
    text(subtitle, 40f, 678f, 24f, grey)
    text("★★★★★", 40f, 722f, 24f, 0xFFF2A900.toInt())
    p.textSize = 24f
    text(if (shoe) "4.7 · 486 reviews" else "4.8 · 212 reviews", 52f + p.measureText("★★★★★"), 722f, 22f, grey)
    text(price, 40f, 806f, 56f, ink, bold = true)
    text(stock, 40f, 850f, 26f, stockColor, bold = stockColor != 0xFF444444.toInt())
    box(40f, 890f, w - 40f, 966f, 38f, ink)
    p.textSize = 26f
    p.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    text(button, w / 2 - p.measureText(button) / 2, 938f, 26f, Color.WHITE, bold = true)
    // Specs, then delivery details.
    val specs = if (shoe) listOf("Waterproof", "Grippy sole", "280 g") else listOf("5K Retina", "USB‑C 96 W", "Tilt + height")
    specs.forEachIndexed { i, spec ->
        box(40f + i * 220, 1010f, 240f + i * 220, 1100f, 16f, 0xFFF4F5F7.toInt())
        text(spec, 60f + i * 220, 1064f, 23f, ink, bold = true)
    }
    box(40f, 1140f, w - 40f, 1142f, 0f, 0xFFEEEEEE.toInt())
    text("Free returns within 30 days", 40f, 1194f, 24f, ink)
    text("Two-year warranty included", 40f, 1240f, 24f, ink)
    text("Pay in 4 interest-free instalments", 40f, 1286f, 24f, grey)
    return ByteArrayOutputStream().use {
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        it.toByteArray()
    }
}
