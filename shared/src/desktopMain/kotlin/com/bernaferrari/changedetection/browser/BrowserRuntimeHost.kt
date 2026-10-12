package com.bernaferrari.changedetection.browser

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.bernaferrari.changedetection.shared.generated.resources.*
import kotlinx.coroutines.*
import kotlinx.coroutines.swing.Swing
import com.bernaferrari.changedetection.localization.stringResource

@Composable
internal actual fun BrowserRuntimeHost(modifier: Modifier, content: @Composable () -> Unit) {
    val state by DesktopBrowserRuntime.state.collectAsState()
    LaunchedEffect(Unit) { DesktopBrowserRuntime.start() }
    if (state == DesktopBrowserRuntime.State.Ready) {
        content()
    } else {
        Surface(modifier, color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (state != DesktopBrowserRuntime.State.Failed) CircularProgressIndicator(Modifier.size(24.dp))
                Text(stringResource(if (state == DesktopBrowserRuntime.State.Failed) Res.string.browser_runtime_failed else Res.string.browser_runtime_loading), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

/** Compile the native Swing bridge against our Compose version instead of an older binary ABI. */
@Composable
internal actual fun PlatformBrowserView(state: com.multiplatform.webview.web.WebViewState, modifier: Modifier, onNavigationRequest: ((String) -> Boolean)?, highlightUpdate: com.bernaferrari.changedetection.detection.RegionHighlightUpdate?) {
    val navigation by rememberUpdatedState(onNavigationRequest)
    val content = state.content
    key(content) {
        var loaded by remember { mutableStateOf(false) }
        var timedOut by remember { mutableStateOf(false) }
        LaunchedEffect(content) {
            delay(20_000)
            if (!loaded) timedOut = true
        }
        if (timedOut) {
            DesktopBrowserFallback(content, modifier)
        } else {
            val session = remember(content) {
                runCatching {
                    DesktopBrowserSession.create(content, state.webSettings.desktopWebSettings.disablePopupWindows, onNavigationRequest = { navigation?.invoke(it) ?: true }) { loaded = true }
                }
            }
            val native = session.getOrNull()
            if (native == null) {
                DesktopBrowserFallback(content, modifier)
            } else {
                DisposableEffect(native) {
                    onDispose {
                        native.close()
                    }
                }
                LaunchedEffect(native, loaded, highlightUpdate) {
                    if (loaded && highlightUpdate != null) {
                        withContext(Dispatchers.Swing) {
                            native.browser.executeJavaScript(highlightUpdate.script(), native.browser.url, 0)
                        }
                    }
                }
                val loadDescription = stringResource(if (loaded) Res.string.done else Res.string.loading)
                androidx.compose.ui.awt.SwingPanel(
                    factory = { native.browser.uiComponent },
                    modifier = modifier.testTag("desktop-webview").semantics { stateDescription = loadDescription },
                )
            }
        }
    }
}

@Composable
private fun DesktopBrowserFallback(content: com.multiplatform.webview.web.WebContent, modifier: Modifier) {
    val text = remember(content) {
        (content as? com.multiplatform.webview.web.WebContent.Data)?.data?.let {
            com.bernaferrari.changedetection.util.readableSnapshotText(it, isHtml = true)
        }
    }
    Surface(modifier, color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
            Text(stringResource(if (text != null) Res.string.browser_text_fallback else Res.string.browser_runtime_failed), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (text != null) SelectionContainer { Text(text, style = MaterialTheme.typography.bodyLarge) }
        }
    }
}

internal actual val supportsPageRegionSelection: Boolean = true
