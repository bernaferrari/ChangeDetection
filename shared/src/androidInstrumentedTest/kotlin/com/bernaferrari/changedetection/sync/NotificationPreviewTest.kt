package com.bernaferrari.changedetection.sync

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.graphics.*
import android.os.*
import androidx.activity.ComponentActivity
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.bernaferrari.changedetection.MainActivityProvider
import com.bernaferrari.changedetection.preview.ChangePreviewRepository
import com.bernaferrari.changedetection.repo.*
import com.bernaferrari.changedetection.repo.source.*
import com.bernaferrari.changedetection.repo.source.local.*
import com.bernaferrari.changedetection.storage.SnapshotFileCleanup
import com.bernaferrari.changedetection.util.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.junit.Assert.*
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import java.io.*

internal class AlertFixture : AutoCloseable {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val manager = context.getSystemService(NotificationManager::class.java)
    val prefs = MutableStateFlow<Preferences>(emptyPreferences())
    val dataStore = object : DataStore<Preferences> {
        override val data = prefs
        override suspend fun updateData(transform: suspend (Preferences) -> Preferences) = transform(prefs.value).also { prefs.value = it }
    }
    private val name = "alert-${java.util.UUID.randomUUID()}.db"
    val db = getRoomDatabase(Room.databaseBuilder<ChangeDatabase>(context, context.getDatabasePath(name).absolutePath), BundledSQLiteDriver())
    val files = AndroidFileStorage(context)
    private val cleanup = SnapshotFileCleanup(db.pendingFileDeletionDao(), db.snapsDao(), files)
    val snaps = SnapsRepository(SnapsLocalDataSource(AppExecutors(), db.snapsDao(), db.siteDao(), files, cleanup))
    val site = Site("Synthetic workshop tickets", "https://example.test/tickets", 0L, "", "synthetic-alert", 0 to 0).copy(captureMode = CaptureMode.SCREENSHOT)
    private val ids = listOf("alert-before-${java.util.UUID.randomUUID()}", "alert-after-${java.util.UUID.randomUUID()}")
    val oldestId get() = ids.first()
    val newestId get() = ids.last()
    init {
        MainActivityProvider.register(ComponentActivity::class.java)
        if (Build.VERSION.SDK_INT >= 33) InstrumentationRegistry.getInstrumentation().uiAutomation.grantRuntimePermission(context.packageName, Manifest.permission.POST_NOTIFICATIONS)
        manager.cancelAll()
        startKoin {
            modules(
                module {
                    single<Context> { context }
                    single<DataStore<Preferences>> { dataStore }
                    single { snaps }
                    single { ChangePreviewRepository(snaps) { db.snapsDao().getPrecedingSnap(it.siteId, it.timestamp, it.snapId) } }
                },
            )
        }
        runBlocking(Dispatchers.IO) {
            db.siteDao().insertSite(site)
            ids.forEachIndexed { index, id ->
                val bytes = picture(index == 1)
                files.writeBytes(id, bytes)
                db.snapsDao().insertSnap(Snap(id, site.id, 1_700_000_000_000L + index * 60_000, "image/png", "", bytes.size))
            }
        }
    }
    suspend fun post() = AndroidChangeNotifier(context).notifyChange(site, newestId)
    fun alert(predicate: (Notification) -> Boolean = { true }): Notification {
        val end = SystemClock.uptimeMillis() + 4_000
        while (SystemClock.uptimeMillis() < end) {
            manager.activeNotifications.firstOrNull { it.tag == site.id && predicate(it.notification) }?.let { return it.notification }
            SystemClock.sleep(20)
        }
        error("Expected synthetic change notification was not posted")
    }
    override fun close() {
        if (InstrumentationRegistry.getArguments().getString("leave_alert") != "true") manager.cancelAll()
        stopKoin()
        ids.forEach { files.deleteFile(it) }
        db.close()
        context.deleteDatabase(name)
    }
    private fun picture(changed: Boolean): ByteArray {
        val bitmap = Bitmap.createBitmap(720, 2200, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = Color.DKGRAY
        paint.textSize = 42f
        canvas.drawText("Workshop tickets", 30f, 90f, paint)
        paint.color = if (changed) Color.rgb(255, 225, 80) else Color.rgb(220, 235, 250)
        canvas.drawRect(24f, 260f, 696f, 540f, paint)
        paint.color = Color.BLACK
        paint.textSize = 68f
        canvas.drawText(if (changed) "€79 · Available" else "€99 · Sold out", 44f, 370f, paint)
        paint.textSize = 30f
        canvas.drawText("Synthetic example content", 44f, 450f, paint)
        return ByteArrayOutputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            bitmap.recycle()
            it.toByteArray()
        }
    }
}

class NotificationPreviewTest {
    @Test fun newSavedChangesAlertWhileRetriesRemainSilentAcrossNotifierRecreation() = runBlocking {
        AlertFixture().use { f ->
            AndroidChangeNotifier(f.context) { _, _ -> null }.notifyChange(f.site, f.oldestId)
            val first = f.alert()
            assertEquals(0, first.flags and Notification.FLAG_ONLY_ALERT_ONCE)
            AndroidChangeNotifier(f.context) { _, _ -> null }.notifyChange(f.site, f.oldestId)
            f.alert { it.flags and Notification.FLAG_ONLY_ALERT_ONCE != 0 }
            AndroidChangeNotifier(f.context) { _, _ -> null }.notifyChange(f.site, f.newestId)
            val next = f.alert { it.contentIntent != first.contentIntent }
            assertEquals(0, next.flags and Notification.FLAG_ONLY_ALERT_ONCE)
            assertEquals(1, f.manager.activeNotifications.count { it.tag == f.site.id })
        }
    }

    @Test fun savedImageIsOffByDefaultAndAppearsOnlyAfterExplicitOptIn() = runBlocking {
        AlertFixture().use { f ->
            assertFalse(f.prefs.value[NotificationPreferences.IMAGE_PREVIEWS] ?: false)
            f.post()
            assertFalse(f.alert().extras.containsKey(Notification.EXTRA_PICTURE))
            f.dataStore.updateData { it.toMutablePreferences().apply { this[NotificationPreferences.IMAGE_PREVIEWS] = true } }
            f.post()
            val alert = f.alert { it.extras.containsKey(Notification.EXTRA_PICTURE) }

            @Suppress("DEPRECATION")
            val bitmap = alert.extras.getParcelable<Bitmap>(Notification.EXTRA_PICTURE)
            assertNotNull(bitmap)
            assertEquals(720, bitmap!!.width)
            assertEquals(300, bitmap.height)
            assertTrue(bitmap.allocationByteCount <= 1_000_000)
            assertNotNull(alert.contentIntent)
            assertEquals(1, alert.actions.size)
            assertTrue(alert.flags and Notification.FLAG_ONLY_ALERT_ONCE != 0)
            val public = requireNotNull(alert.publicVersion)
            assertFalse(public.extras.toString().contains("Synthetic"))
            assertFalse(public.extras.containsKey(Notification.EXTRA_PICTURE))
            assertNull(public.actions)
            val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
            automation.serviceInfo = automation.serviceInfo.apply { flags = flags or android.accessibilityservice.AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS }
            ParcelFileDescriptor.AutoCloseInputStream(automation.executeShellCommand("cmd statusbar expand-notifications")).use { it.readBytes() }
            val shownUntil = SystemClock.uptimeMillis() + 8_000
            var shown = false
            while (SystemClock.uptimeMillis() < shownUntil) {
                val nodes = automation.windows.flatMap { it.root?.findAccessibilityNodeInfosByText("Synthetic workshop").orEmpty() }
                if (nodes.isNotEmpty()) {
                    shown = true
                    break
                }
                SystemClock.sleep(100)
            }
            println("NOTIFICATION_UI visible=$shown active=${f.manager.activeNotifications.size} importance=${f.manager.getNotificationChannel(AndroidChangeNotifier.CHANNEL_ID)?.importance}")

            // Some emulator UiAutomation windows omit SystemUI notification text.
            // Inspect the physical-display screenshot separately; do not confuse observer access with delivery.
            automation.takeScreenshot()?.let { shot ->
                File(f.context.getExternalFilesDir(null), "notification-rich.png").outputStream().use { shot.compress(Bitmap.CompressFormat.PNG, 100, it) }
                shot.recycle()
            }
            automation.executeShellCommand("cmd statusbar collapse").close()
        }
    }

    @Test fun groupedAndUngroupedMultipleWatchesKeepIndependentPrivateAlerts() = runBlocking {
        AlertFixture().use { f ->
            val notifier = AndroidChangeNotifier(f.context) { _, _ -> null }
            val sites = listOf("first", "second", "third").map { f.site.copy(id = "synthetic-$it", title = "Synthetic $it watch") }
            val modes = when (InstrumentationRegistry.getArguments().getString("grouping_mode")) {
                "grouped" -> listOf(true)
                "ungrouped" -> listOf(false)
                else -> listOf(true, false)
            }
            for (grouped in modes) {
                f.manager.cancelAll()
                val cleared = SystemClock.uptimeMillis() + 3_000
                while (f.manager.activeNotifications.isNotEmpty() && SystemClock.uptimeMillis() < cleared) SystemClock.sleep(20)
                // Avoid the platform enqueue-rate limiter while comparing group behavior.
                SystemClock.sleep(1_100)
                for (site in sites) {
                    notifier.notifyChange(site, "capture-${site.id}")
                    val submitted = SystemClock.uptimeMillis() + 3_000
                    while (f.manager.activeNotifications.none { it.tag == site.id } && SystemClock.uptimeMillis() < submitted) SystemClock.sleep(20)
                    val posted = f.manager.activeNotifications.single { it.tag == site.id }.notification
                    // Build both controlled variants identically from the same posted content and tap tokens.
                    val builder = Notification.Builder(f.context, AndroidChangeNotifier.CHANNEL_ID)
                        .setSmallIcon(posted.smallIcon)
                        .setContentTitle(posted.extras.getCharSequence(Notification.EXTRA_TITLE))
                        .setContentText(posted.extras.getCharSequence(Notification.EXTRA_TEXT))
                        .setContentIntent(posted.contentIntent)
                        .setPublicVersion(posted.publicVersion)
                        .setVisibility(posted.visibility)
                        .setOnlyAlertOnce(true)
                        .setAutoCancel(true)
                        .setGroup(if (grouped) AndroidChangeNotifier.GROUP_KEY else null)
                    posted.actions.forEach { builder.addAction(it) }
                    val controlled = builder.build()
                    assertEquals(if (grouped) AndroidChangeNotifier.GROUP_KEY else null, controlled.group)
                    SystemClock.sleep(1_100)
                    f.manager.notify(site.id, AndroidChangeNotifier.NOTIFICATION_ID, controlled)
                }
                val until = SystemClock.uptimeMillis() + 3_000
                var alerts = f.manager.activeNotifications.filter { it.tag in sites.map { site -> site.id } }
                while (SystemClock.uptimeMillis() < until && (alerts.size != 3 || alerts.any { it.notification.group != if (grouped) AndroidChangeNotifier.GROUP_KEY else null })) {
                    SystemClock.sleep(30)
                    alerts = f.manager.activeNotifications.filter { it.tag in sites.map { site -> site.id } }
                }
                assertEquals(3, alerts.size)
                assertEquals(3, alerts.map { it.notification.contentIntent }.toSet().size)
                assertEquals(3, alerts.map { it.notification.actions.single().actionIntent }.toSet().size)
                alerts.forEach { posted ->
                    assertEquals(if (grouped) AndroidChangeNotifier.GROUP_KEY else null, posted.notification.group)
                    assertNotNull(posted.notification.publicVersion)
                    assertNull(posted.notification.publicVersion.contentIntent)
                    assertTrue(posted.notification.flags and Notification.FLAG_ONLY_ALERT_ONCE != 0)
                }
            }
        }
    }

    @Test fun tappingAnOlderAlertTokenStillOpensItsExactSavedCapture() = runBlocking {
        AlertFixture().use { f ->
            androidx.test.core.app.ActivityScenario.launch(ComponentActivity::class.java).use {
                MainActivityProvider.register(AlertTargetActivity::class.java)
                AlertTargetActivity.received = null
                val notifier = AndroidChangeNotifier(f.context) { _, _ -> null }
                notifier.notifyChange(f.site, f.oldestId)
                val old = f.alert().contentIntent
                notifier.notifyChange(f.site, f.newestId)
                f.alert { it.contentIntent != old }
                old.send()
                val until = SystemClock.uptimeMillis() + 5_000
                while (AlertTargetActivity.received == null && SystemClock.uptimeMillis() < until) SystemClock.sleep(20)
                val intent = requireNotNull(AlertTargetActivity.received)
                assertTrue(intent.getBooleanExtra(ChangeNotificationContract.EXTRA_OPEN_COMPARISON, false))
                assertEquals(f.site.id, intent.getStringExtra(ChangeNotificationContract.EXTRA_SITE_ID))
                assertEquals(f.oldestId, intent.getStringExtra(ChangeNotificationContract.EXTRA_SNAP_ID))
                assertNotNull(f.snaps.getSnapById(f.oldestId))
                InstrumentationRegistry.getInstrumentation().runOnMainSync { AlertTargetActivity.active?.finish() }
                MainActivityProvider.register(ComponentActivity::class.java)
            }
        }
    }

    @Test fun malformedOversizedAndMissingSavedImagesGracefullyFallBack() = runBlocking {
        AlertFixture().use { f ->
            f.dataStore.updateData { it.toMutablePreferences().apply { this[NotificationPreferences.IMAGE_PREVIEWS] = true } }
            f.files.deleteFile(f.newestId)
            f.post()
            assertFalse(f.alert().extras.containsKey(Notification.EXTRA_PICTURE))
            assertNull(readNotificationSnapshot(f.context, "../outside"))
            f.files.writeBytes(f.newestId, byteArrayOf(1, 2, 3))
            f.manager.cancelAll()
            f.post()
            assertFalse(f.alert().extras.containsKey(Notification.EXTRA_PICTURE))
            val name = "oversized-alert-fixture"
            try {
                withContext(Dispatchers.IO) { f.context.openFileOutput(name, 0).use { it.write(ByteArray(MAX_PREVIEW_INPUT_BYTES + 1)) } }
                assertNull(readNotificationSnapshot(f.context, name))
            } finally {
                f.context.deleteFile(name)
            }
        }
    }

    @Test fun channelHealthDetectsBlockedChannelWithoutChangingTheLiveChannel() {
        AlertFixture().use { f ->
            val id = "health-fixture-${java.util.UUID.randomUUID()}"
            f.manager.createNotificationChannel(android.app.NotificationChannel(id, "Synthetic health fixture", NotificationManager.IMPORTANCE_NONE))
            try {
                assertEquals(NotificationHealth.ChannelDisabled, readNotificationHealth(f.context, id))
            } finally {
                f.manager.deleteNotificationChannel(id)
            }
            assertEquals(NotificationHealth.Enabled, readNotificationHealth(f.context))
        }
    }
}

/** Test-only activity receives a real immutable notification PendingIntent through Android. */
class AlertTargetActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        active = this
        received = intent
    }
    companion object {
        @Volatile var received: android.content.Intent? = null

        @Volatile var active: AlertTargetActivity? = null
    }
}
