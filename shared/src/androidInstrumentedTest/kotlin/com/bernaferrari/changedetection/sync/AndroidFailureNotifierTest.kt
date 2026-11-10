package com.bernaferrari.changedetection.sync

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.SystemClock
import android.service.notification.StatusBarNotification
import androidx.activity.ComponentActivity
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.bernaferrari.changedetection.MainActivityProvider
import com.bernaferrari.changedetection.error.CheckFailure
import com.bernaferrari.changedetection.library.R
import com.bernaferrari.changedetection.repo.Site
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AndroidFailureNotifierTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val manager = context.getSystemService(NotificationManager::class.java)
    private val notifier = AndroidFailureNotifier(context)

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

    @Test fun namesThePageSaysWhoMustActAndIsWithdrawnOnRecovery() = runBlocking {
        notifier.notifyStoppedWatching(site("grants"), CheckFailure.LOGIN)
        val alert = awaitAlerts { it.size == 1 }.single()
        assertEquals("grants", alert.tag)
        assertEquals(context.getString(R.string.failure_notification_named, "Test grants"), alert.notification.extras.getString(Notification.EXTRA_TITLE))
        assertEquals(context.getString(R.string.failure_notification_needs_you), alert.notification.extras.getCharSequence(Notification.EXTRA_TEXT).toString())
        if (Build.VERSION.SDK_INT >= 31) assertTrue(alert.notification.contentIntent.isImmutable)

        notifier.clear("grants")
        awaitAlerts { it.isEmpty() }
        Unit
    }

    @Test fun aProblemThatMayPassIsWordedAsOneAndUntitledPagesUseTheirHost() = runBlocking {
        notifier.notifyStoppedWatching(site("down").copy(title = null, url = "https://www.example.test/status"), CheckFailure.SERVER)
        val notification = awaitAlerts { it.size == 1 }.single().notification
        assertEquals(context.getString(R.string.failure_notification_named, "example.test"), notification.extras.getString(Notification.EXTRA_TITLE))
        assertEquals(context.getString(R.string.failure_notification_keeps_failing), notification.extras.getCharSequence(Notification.EXTRA_TEXT).toString())
    }

    @Test fun theLockScreenDoesNotRevealWhichPageIsWatched() = runBlocking {
        notifier.notifyStoppedWatching(site("private"), CheckFailure.NOT_FOUND)
        val notification = awaitAlerts { it.size == 1 }.single().notification
        assertEquals(Notification.VISIBILITY_PRIVATE, notification.visibility)
        val public = requireNotNull(notification.publicVersion)
        assertEquals(context.getString(R.string.app_name), public.extras.getString(Notification.EXTRA_TITLE))
        assertFalse(public.extras.getCharSequence(Notification.EXTRA_TEXT).toString().contains("private"))
        assertNull(public.contentIntent)
    }

    @Test fun aPageKeepsItsChangeAlertBesideItsProblemAlert() = runBlocking {
        AndroidChangeNotifier(context).notifyChange(site("both"), "saved-1")
        notifier.notifyStoppedWatching(site("both"), CheckFailure.LOGIN)
        val alerts = awaitAlerts { it.size == 2 }
        assertEquals(setOf("both"), alerts.map { it.tag }.toSet())
        assertEquals(setOf(AndroidChangeNotifier.NOTIFICATION_ID, AndroidFailureNotifier.NOTIFICATION_ID), alerts.map { it.id }.toSet())

        // Recovery withdraws the problem and leaves the change the person has not read yet.
        notifier.clear("both")
        assertEquals(AndroidChangeNotifier.NOTIFICATION_ID, awaitAlerts { it.size == 1 }.single().id)
    }

    @Test fun eachPageOpensItself() = runBlocking {
        notifier.notifyStoppedWatching(site("Aa"), CheckFailure.LOGIN)
        notifier.notifyStoppedWatching(site("BB"), CheckFailure.LOGIN)
        val alerts = awaitAlerts { alerts -> alerts.map { it.tag }.toSet() == setOf("Aa", "BB") }.associateBy { it.tag }
        assertNotEquals(alerts.getValue("Aa").notification.contentIntent, alerts.getValue("BB").notification.contentIntent)
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
