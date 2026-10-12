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
    val args = platform.Foundation.NSProcessInfo.processInfo.arguments.map { it.toString() }
    // Debug aid: `-seedCaptures <url> <minutes>` captures the page now and again after <minutes>, as a real screenshot page.
    args.indexOf("-seedCaptures").takeIf { it >= 0 }?.let { i -> args.getOrNull(i + 1)?.let { it to (args.getOrNull(i + 2)?.toLongOrNull() ?: 3L) } }?.let { (url, minutes) ->
        kotlinx.coroutines.MainScope().launch {
            kotlinx.coroutines.delay(3_000)
            val sites = koin.get<com.bernaferrari.changedetection.repo.source.SitesRepository>()
            val existing = sites.getSiteByUrl(url)
            val site = existing ?: com.bernaferrari.changedetection.repo.Site(null, url, 0L, "", "seed-" + com.bernaferrari.changedetection.util.getCurrentTimeMillis(), 0 to 0)
                .copy(captureMode = com.bernaferrari.changedetection.repo.CaptureMode.SCREENSHOT).also { sites.saveSite(it, false) }
            val runner = koin.get<com.bernaferrari.changedetection.sync.IosCheckRunner>()
            runner.check(site.id)
            kotlinx.coroutines.delay(minutes * 60_000)
            runner.check(site.id)
        }
    }
    // Debug aid: `-captureTest <url>` writes one WebKit capture to tmp/last-capture.png.
    args.indexOf("-captureTest").takeIf { it >= 0 }?.let { args.getOrNull(it + 1) }?.let { url ->
        kotlinx.coroutines.MainScope().launch {
            kotlinx.coroutines.delay(3_000)
            runCatching { com.bernaferrari.changedetection.browser.IosPageCapture.capture(url, 2_500) }
                .onFailure { io.github.aakira.napier.Napier.e("captureTest failed", it) }
        }
    }
    // iOS gives background refresh only occasionally, so returning to the app catches up on due pages.
    NSNotificationCenter.defaultCenter.addObserverForName(UIApplicationDidBecomeActiveNotification, null, NSOperationQueue.mainQueue) { _ ->
        foreground.launch {
            val schedule = MonitoringSchedule.from(koin.get<androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences>>().data.first())
            if (schedule.enabled) {
                try {
                    koin.get<com.bernaferrari.changedetection.sync.IosCheckRunner>().checkAll(onlyDueAfterMinutes = schedule.intervalMinutes)
                } catch (cancelled: kotlinx.coroutines.CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    io.github.aakira.napier.Napier.w("Foreground checks failed", error)
                }
            }
        }
    }
    // Turbo only runs while the app is open; keep the screen on for the length of a session.
    kotlinx.coroutines.MainScope().launch {
        koin.get<com.bernaferrari.changedetection.background.MonitoringManager>().quickMonitorState.collect { state ->
            UIApplication.sharedApplication.idleTimerDisabled = state.isActive
        }
    }
    val platformActions = PlatformActions(
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
    )
    // Native tab bar and navigation stacks around shared Compose screens; `-composeNav` keeps the all-Compose shell.
    if ("-composeNav" !in args) return IosNativeShell(platformActions).root
    return ComposeUIViewController {
        val link by IosDeepLinks.pending.collectAsState()
        ChangeDetectionRootApp(
            platformActions = platformActions,
            initialSiteId = link?.first,
            initialSnapId = link?.second,
            onInitialSiteIdHandled = IosDeepLinks::consume,
        )
    }
}
