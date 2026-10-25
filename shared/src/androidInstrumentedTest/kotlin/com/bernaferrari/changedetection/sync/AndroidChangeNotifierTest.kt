package com.bernaferrari.changedetection.sync

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.SystemClock
import android.service.notification.StatusBarNotification
import androidx.activity.ComponentActivity
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bernaferrari.changedetection.MainActivityProvider
import com.bernaferrari.changedetection.repo.Site
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlinx.coroutines.async

@RunWith(AndroidJUnit4::class)
class AndroidChangeNotifierTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val manager = context.getSystemService(NotificationManager::class.java)
    private val notifier = AndroidChangeNotifier(context)

    @Before fun setup() {
        MainActivityProvider.register(ComponentActivity::class.java)
        if (Build.VERSION.SDK_INT >= 33) {
            InstrumentationRegistry.getInstrumentation().uiAutomation.grantRuntimePermission(context.packageName, Manifest.permission.POST_NOTIFICATIONS)
        }
        manager.cancelAll()
        awaitAlerts { it.isEmpty() }
        assertTrue(manager.areNotificationsEnabled())
    }

    @After fun cleanup() {
        manager.cancelAll()
    }

    @Test fun sameHashPagesKeepIndependentAlertsAndTapTokens() = kotlinx.coroutines.runBlocking {
        assertEquals("Aa".hashCode(), "BB".hashCode())
        notifier.notifyChange(site("Aa"), "capture-A")
        notifier.notifyChange(site("BB"), "capture-B")
        val alerts = awaitAlerts { alerts -> alerts.map { it.tag }.toSet() == setOf("Aa", "BB") }.associateBy { it.tag }
        assertEquals(setOf("Aa", "BB"), alerts.keys)
        assertNotEquals(alerts.getValue("Aa").notification.contentIntent, alerts.getValue("BB").notification.contentIntent)
    }

    @Test fun replacementAlertGetsANewSnapshotTokenWithoutStackingNotifications() = kotlinx.coroutines.runBlocking {
        notifier.notifyChange(site("Aa"), "capture-1")
        val previous = awaitAlerts { it.size == 1 }.single().notification.contentIntent
        notifier.notifyChange(site("Aa"), "capture-2")
        val replacement = awaitAlerts { it.size == 1 && it.single().notification.contentIntent != previous }.single()
        assertEquals("Aa", replacement.tag)
        assertNotEquals(previous, replacement.notification.contentIntent)
        // The old token cannot be repointed to a newer capture by updating extras.
    }

    @Test fun privateAlertHasGenericPublicVersionAndKeepsOnlyAlertOnce() = kotlinx.coroutines.runBlocking {
        notifier.notifyChange(site("private"), "saved-1")
        val notification = awaitAlerts { it.size == 1 }.single().notification
        assertTrue(notification.flags and android.app.Notification.FLAG_ONLY_ALERT_ONCE != 0)
        val public = requireNotNull(notification.publicVersion)
        assertEquals(context.getString(com.bernaferrari.changedetection.library.R.string.app_name), public.extras.getString(android.app.Notification.EXTRA_TITLE))
        assertFalse(public.extras.getString(android.app.Notification.EXTRA_TEXT).orEmpty().contains("private"))
        assertNull(public.actions)
        assertNull(public.contentIntent)
        assertFalse(public.extras.containsKey(android.app.Notification.EXTRA_PICTURE))
    }

    @Test fun browserActionIsImmutableAndSeparateFromTheExactComparison() = kotlinx.coroutines.runBlocking {
        notifier.notifyChange(site("browser"), "saved-1")
        val first = awaitAlerts { it.size == 1 }.single().notification
        val action = first.actions.single()
        assertEquals(context.getString(com.bernaferrari.changedetection.library.R.string.change_notification_open_browser), action.title.toString())
        assertNotEquals(first.contentIntent, action.actionIntent)
        if (Build.VERSION.SDK_INT >= 31) {
            assertTrue(action.actionIntent.isActivity)
            assertTrue(action.actionIntent.isImmutable)
        }
        notifier.notifyChange(site("browser"), "saved-2")
        val next = awaitAlerts { it.size == 1 && it.single().notification.contentIntent != first.contentIntent }.single().notification
        assertNotEquals(action.actionIntent, next.actions.single().actionIntent)
    }

    @Test fun nonWebAddressesHaveNoBrowserAction() = kotlinx.coroutines.runBlocking {
        notifier.notifyChange(site("bad").copy(url = "javascript:alert(1)"), "saved-1")
        assertTrue(awaitAlerts { it.size == 1 }.single().notification.actions.isNullOrEmpty())
    }

    @Test fun eligibilityRevokedDuringPreparationDoesNotReachTheNotificationManager() = kotlinx.coroutines.runBlocking {
        val entered = kotlinx.coroutines.CompletableDeferred<Unit>()
        val release = kotlinx.coroutines.CompletableDeferred<Unit>()
        val current = java.util.concurrent.atomic.AtomicBoolean(true)
        val preparing = AndroidChangeNotifier(context, prepare = { _, _ ->
            entered.complete(Unit)
            release.await()
            null
        })
        val owner = async {
            preparing.notifyChangeIfCurrent(site("revoked"), "saved-1") { current.get() }
        }
        entered.await()
        current.set(false)
        release.complete(Unit)
        owner.await()
        assertTrue(manager.activeNotifications.isEmpty())

        current.set(true)
        preparing.notifyChangeIfCurrent(site("revoked"), "saved-2") { current.get() }
        assertEquals("revoked", awaitAlerts { it.size == 1 }.single().tag)
    }

    private fun awaitAlerts(predicate: (Array<StatusBarNotification>) -> Boolean): Array<StatusBarNotification> {
        val deadline = SystemClock.elapsedRealtime() + 3_000
        do {
            val alerts = manager.activeNotifications
            if (predicate(alerts)) return alerts
            SystemClock.sleep(20)
        } while (SystemClock.elapsedRealtime() < deadline)
        fail("Android did not deliver the expected notification state within 3 seconds")
        return emptyArray()
    }

    private fun site(id: String) = Site("Test $id", "https://example.test/$id", 0L, "", id, 0 to 0)
}
