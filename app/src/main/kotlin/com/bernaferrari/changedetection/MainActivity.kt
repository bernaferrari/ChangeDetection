/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection

import android.Manifest
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import com.bernaferrari.changedetection.ui.main.MainViewModel
import org.koin.compose.viewmodel.koinViewModel

class MainActivity : ComponentActivity() {
    private var incomingUrl by mutableStateOf<String?>(null)
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
        window.colorMode = ActivityInfo.COLOR_MODE_WIDE_COLOR_GAMUT

        incomingUrl = intent.extractSharedUrl()

        // Read deep link extras from notification
        val initialSiteId =
            if (intent.getBooleanExtra("openComparison", false)) {
                intent.getStringExtra("siteId")
            } else {
                null
            }

        setContent {
            val mainViewModel: MainViewModel = koinViewModel()
            val context = LocalContext.current

            ChangeDetectionRootApp(
                platformActions =
                PlatformActions(
                    openUrl = { url -> openUrl(context, url) },
                    openEmail = { email, subject -> openEmail(context, email, subject) },
                    startTurbo = { siteIds, intervalSeconds ->
                        requestNotificationPermissionIfNeeded()
                        mainViewModel.startQuickMonitor(siteIds, intervalSeconds)
                    },
                    stopTurbo = {
                        mainViewModel.stopQuickMonitor()
                    },
                    requestNotifications = ::requestNotificationPermissionIfNeeded,
                    appVersion = BuildConfig.VERSION_NAME,
                ),
                initialSiteId = initialSiteId,
                initialUrl = incomingUrl,
                onInitialUrlHandled = { incomingUrl = null },
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        incomingUrl = intent.extractSharedUrl()
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

private fun Intent.extractSharedUrl(): String? = when (action) {
    Intent.ACTION_VIEW -> dataString

    Intent.ACTION_SEND -> getStringExtra(Intent.EXTRA_TEXT)
        ?.lineSequence()
        ?.flatMap { it.splitToSequence(' ', '\t') }
        ?.map { it.trim().trimEnd('.', ',', ')', ']') }
        ?.firstOrNull { it.startsWith("http://") || it.startsWith("https://") }

    else -> null
}

private fun openUrl(
    context: android.content.Context,
    url: String,
) {
    try {
        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
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
