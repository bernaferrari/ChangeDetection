/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
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
