/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection

import androidx.compose.runtime.*
import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import com.bernaferrari.changedetection.di.initKoin
import com.bernaferrari.changedetection.util.DesktopBackDispatcher
import com.bernaferrari.changedetection.util.LocalDesktopBackDispatcher
import java.awt.Desktop
import java.net.URI
import java.net.URLEncoder

fun main() {
    initKoin()
    application {
        val back = remember { DesktopBackDispatcher() }
        Window(
            onCloseRequest = {
                com.bernaferrari.changedetection.browser.DesktopBrowserRuntime.dispose()
                exitApplication()
            },
            title = "Change Detection · Desktop",
            state = rememberWindowState(width = 430.dp, height = 900.dp),
            onPreviewKeyEvent = { it.key == Key.Escape && it.type == KeyEventType.KeyDown && back.dispatch() },
        ) {
            LaunchedEffect(Unit) {
                Desktop.getDesktop().requestForeground(true)
                window.toFront()
                window.requestFocus()
            }
            CompositionLocalProvider(LocalDesktopBackDispatcher provides back) {
                ChangeDetectionRootApp(
                    PlatformActions(
                        openUrl = { Desktop.getDesktop().browse(URI(it)) },
                        openEmail = { email, subject -> Desktop.getDesktop().mail(URI("mailto:$email?subject=${URLEncoder.encode(subject, "UTF-8").replace("+", "%20")}")) },
                        appVersion = "Desktop development",
                    ),
                )
            }
        }
    }
}
