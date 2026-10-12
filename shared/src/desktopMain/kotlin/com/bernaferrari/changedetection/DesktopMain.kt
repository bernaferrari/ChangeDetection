package com.bernaferrari.changedetection

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import com.bernaferrari.changedetection.di.initKoin
import com.bernaferrari.changedetection.shared.generated.resources.Res
import com.bernaferrari.changedetection.shared.generated.resources.app_icon
import com.bernaferrari.changedetection.shared.generated.resources.app_icon_dark
import com.bernaferrari.changedetection.util.DesktopBackDispatcher
import com.bernaferrari.changedetection.util.LocalDesktopBackDispatcher
import java.awt.Desktop
import java.net.URI
import java.net.URLEncoder
import org.jetbrains.compose.resources.painterResource
import com.bernaferrari.changedetection.localization.stringResource
import com.bernaferrari.changedetection.shared.generated.resources.app_name
import com.bernaferrari.changedetection.shared.generated.resources.desktop_development

fun main() {
    initKoin()
    application {
        val back = remember { DesktopBackDispatcher() }
        val appIcon = if (isSystemInDarkTheme()) Res.drawable.app_icon_dark else Res.drawable.app_icon
        Window(
            onCloseRequest = {
                com.bernaferrari.changedetection.browser.DesktopBrowserRuntime.dispose()
                exitApplication()
            },
            title = stringResource(Res.string.app_name),
            icon = painterResource(appIcon),
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
                        appVersion = stringResource(Res.string.desktop_development),
                    ),
                )
            }
        }
    }
}
