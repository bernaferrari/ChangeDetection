/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.window.ComposeUIViewController
import com.bernaferrari.changedetection.background.MonitoringSchedule
import com.bernaferrari.changedetection.di.initKoinIos
import com.bernaferrari.changedetection.platform.openEmail
import com.bernaferrari.changedetection.platform.openUrl
import com.bernaferrari.changedetection.sync.IosDeepLinks
import com.bernaferrari.changedetection.sync.SyncEngine
import com.bernaferrari.changedetection.sync.requestIosNotificationPermission
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationDidBecomeActiveNotification
import platform.UIKit.UIApplicationOpenSettingsURLString
import platform.UIKit.UIViewController

private val foreground = CoroutineScope(SupervisorJob() + Dispatchers.Default)

fun MainViewController(): UIViewController {
    val koin = initKoinIos().koin
    // Debug aid: launch with `-demo` to fill an empty install with realistic pages and history.
    if ("-demo" in platform.Foundation.NSProcessInfo.processInfo.arguments.map { it.toString() }) {
        kotlinx.coroutines.runBlocking {
            com.bernaferrari.changedetection.debug.seedDemoContent(koin.get(), koin.get(), com.bernaferrari.changedetection.util.getCurrentTimeMillis())
        }
    }
    // iOS gives background refresh only occasionally, so returning to the app catches up on due pages.
    NSNotificationCenter.defaultCenter.addObserverForName(UIApplicationDidBecomeActiveNotification, null, NSOperationQueue.mainQueue) { _ ->
        foreground.launch {
            val schedule = MonitoringSchedule.from(koin.get<androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences>>().data.first())
            if (schedule.enabled) runCatching { koin.get<SyncEngine>().syncDueSites(schedule.intervalMinutes) }
        }
    }
    return ComposeUIViewController {
        val link by IosDeepLinks.pending.collectAsState()
        ChangeDetectionRootApp(
            platformActions = PlatformActions(
                openUrl = ::openUrl,
                openEmail = ::openEmail,
                requestNotifications = ::requestIosNotificationPermission,
                openNotificationSettings = {
                    NSURL.URLWithString(UIApplicationOpenSettingsURLString)?.let { UIApplication.sharedApplication.openURL(it, emptyMap<Any?, Any>(), null) }
                },
                appVersion = platform.Foundation.NSBundle.mainBundle
                    .objectForInfoDictionaryKey("CFBundleShortVersionString")
                    ?.toString()
                    .orEmpty(),
            ),
            initialSiteId = link?.first,
            initialSnapId = link?.second,
            onInitialSiteIdHandled = IosDeepLinks::consume,
        )
    }
}
