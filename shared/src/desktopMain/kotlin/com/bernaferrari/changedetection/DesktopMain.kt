package com.bernaferrari.changedetection

import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import com.bernaferrari.changedetection.di.initKoin
import com.bernaferrari.changedetection.shared.generated.resources.Res
import com.bernaferrari.changedetection.util.DesktopBackDispatcher
import com.bernaferrari.changedetection.util.LocalDesktopBackDispatcher
import java.awt.Desktop
import java.net.URI
import java.net.URLEncoder
import com.bernaferrari.changedetection.localization.stringResource
import com.bernaferrari.changedetection.shared.generated.resources.app_name
import com.bernaferrari.changedetection.shared.generated.resources.desktop_development
import com.bernaferrari.changedetection.shared.generated.resources.external_open_failed

fun main() {
    initKoin()
    application {
        val back = remember { DesktopBackDispatcher() }
        // A plain bundled PNG (src/desktopMain/resources), not a Compose resource.
        val appIcon = remember {
            androidx.compose.ui.graphics.painter.BitmapPainter(
                Thread.currentThread().contextClassLoader.getResourceAsStream("app_icon.png")!!.use {
                    org.jetbrains.skia.Image.makeFromEncoded(it.readBytes()).toComposeImageBitmap()
                },
            )
        }
        Window(
            onCloseRequest = {
                com.bernaferrari.changedetection.browser.DesktopBrowserRuntime.dispose()
                exitApplication()
            },
            title = stringResource(Res.string.app_name),
            icon = appIcon,
            state = rememberWindowState(width = 430.dp, height = 900.dp),
            onPreviewKeyEvent = { it.key == Key.Escape && it.type == KeyEventType.KeyDown && back.dispatch() },
        ) {
            val externalFailureMessage = stringResource(Res.string.external_open_failed)
            LaunchedEffect(Unit) {
                Desktop.getDesktop().requestForeground(true)
                window.toFront()
                window.requestFocus()
            }
            CompositionLocalProvider(LocalDesktopBackDispatcher provides back) {
                ChangeDetectionRootApp(
                    PlatformActions(
                        openUrl = { url -> openExternal(externalFailureMessage) { Desktop.getDesktop().browse(URI(url)) } },
                        openEmail = { email, subject -> openExternal(externalFailureMessage) { Desktop.getDesktop().mail(URI("mailto:$email?subject=${URLEncoder.encode(subject, "UTF-8").replace("+", "%20")}")) } },
                        appVersion = stringResource(Res.string.desktop_development),
                    ),
                )
            }
        }
    }
}

private fun openExternal(message: String, action: () -> Unit) {
    try { action() } catch (_: Exception) {
        javax.swing.JOptionPane.showMessageDialog(null, message, "Change Detection", javax.swing.JOptionPane.ERROR_MESSAGE)
    }
}
