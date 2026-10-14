package com.bernaferrari.changedetection

import android.Manifest
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import com.bernaferrari.changedetection.sync.ChangeNotificationContract
import com.bernaferrari.changedetection.ui.main.MainViewModel
import org.koin.compose.viewmodel.koinViewModel

class MainActivity : ComponentActivity() {
    private var incomingUrl by mutableStateOf<String?>(null)
    private var incomingComparisonSiteId by mutableStateOf<String?>(null)
    private var incomingComparisonSnapId by mutableStateOf<String?>(null)
    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) { isGranted: Boolean ->
            // Permission granted or denied - notifications will work if granted
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        // Three-button navigation otherwise gets a system scrim; every screen already pads for the bar,
        // so let the app's own surfaces show through it.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) window.isNavigationBarContrastEnforced = false
        window.colorMode = ActivityInfo.COLOR_MODE_WIDE_COLOR_GAMUT

        // A launch intent is acted on once. A restored activity already holds its result in the saved back
        // stack, and Recents relaunches with the original intent; replaying either would reopen
        // "Watch a page" for a link that was already handled.
        val freshLaunch = savedInstanceState == null && intent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY == 0
        if (freshLaunch) {
            incomingUrl = intent.extractSharedUrl()
            intent.extractComparisonSiteAndSnapIds()?.let { (siteId, snapId) ->
                incomingComparisonSiteId = siteId
                incomingComparisonSnapId = snapId
            }
        }

        setContent {
            val mainViewModel: MainViewModel = koinViewModel()
            val context = LocalContext.current

            ChangeDetectionRootApp(
                platformActions =
                PlatformActions(
                    openUrl = { url -> openUrl(context, url) },
                    openEmail = { email, subject -> openEmail(context, email, subject) },
                    startTurbo = { siteIds, intervalSeconds, durationMinutes ->
                        requestNotificationPermissionIfNeeded()
                        mainViewModel.startQuickMonitor(siteIds, intervalSeconds, durationMinutes)
                    },
                    stopTurbo = {
                        mainViewModel.stopQuickMonitor()
                    },
                    requestNotifications = ::requestNotificationPermissionIfNeeded,
                    openNotificationSettings = {
                        startActivity(
                            Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                .putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, packageName),
                        )
                    },
                    appVersion = BuildConfig.VERSION_NAME,
                ),
                initialSiteId = incomingComparisonSiteId,
                initialSnapId = incomingComparisonSnapId,
                initialUrl = incomingUrl,
                onInitialSiteIdHandled = { incomingComparisonSiteId = null },
                onInitialSnapIdHandled = { incomingComparisonSnapId = null },
                onInitialUrlHandled = { incomingUrl = null },
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        incomingUrl = intent.extractSharedUrl()
        intent.extractComparisonSiteAndSnapIds()?.let { (siteId, snapId) ->
            incomingComparisonSiteId = siteId
            incomingComparisonSnapId = snapId
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            val permissionHistory = getSharedPreferences("permission_history", MODE_PRIVATE)
            if (permissionHistory.getBoolean("notifications_requested", false) ||
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)
            ) {
                return
            }
            // Both first-monitor creation and Turbo can reach here. Ask automatically only once.
            permissionHistory.edit().putBoolean("notifications_requested", true).apply()
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

private fun Intent.extractComparisonSiteAndSnapIds(): Pair<String, String?>? = if (getBooleanExtra(ChangeNotificationContract.EXTRA_OPEN_COMPARISON, false)) {
    getStringExtra(ChangeNotificationContract.EXTRA_SITE_ID)
        ?.takeIf(String::isNotBlank)
        ?.let { siteId -> siteId to getStringExtra(ChangeNotificationContract.EXTRA_SNAP_ID)?.takeIf(String::isNotBlank) }
} else {
    null
}

private fun Intent.extractSharedUrl(): String? = if (extractComparisonSiteAndSnapIds() != null) {
    null
} else {
    when (action) {
        Intent.ACTION_VIEW -> dataString

        Intent.ACTION_SEND -> getStringExtra(Intent.EXTRA_TEXT)
            ?.lineSequence()
            ?.flatMap { it.splitToSequence(' ', '\t') }
            ?.map { it.trim().trimEnd('.', ',', ')', ']') }
            ?.firstOrNull { it.startsWith("http://") || it.startsWith("https://") }

        else -> null
    }
}

private fun openUrl(
    context: android.content.Context,
    url: String,
) {
    try {
        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, url.toUri())
        context.startActivity(intent)
    } catch (e: Exception) {
        // Handle error
    }
}

private fun openEmail(
    context: android.content.Context,
    email: String,
    subject: String,
) {
    try {
        val intent =
            android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
                data = "mailto:$email?subject=${android.net.Uri.encode(subject)}".toUri()
            }
        context.startActivity(intent)
    } catch (e: Exception) {
        // Handle error
    }
}
