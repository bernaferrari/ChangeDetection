package com.bernaferrari.changedetection.localBrowser

import android.webkit.WebView
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import org.koin.compose.viewmodel.koinViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bernaferrari.changedetection.R
import com.bernaferrari.changedetection.extensions.replaceRelativePathWithAbsolute

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalBrowserScreen(
    snapId: String,
    url: String,
    onDismiss: () -> Unit,
    viewModel: LocalBrowserViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val isLocalSnapshot = snapId.isNotBlank()
    val safeUrl = remember(url) { if (url.isNotBlank()) url else "https://google.com" }
    
    LaunchedEffect(snapId) {
        if (isLocalSnapshot) {
            viewModel.fetchData(snapId)
        } else {
            viewModel.reset()
        }
    }
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Top bar
        TopAppBar(
            title = {
                val titleRes = if (isLocalSnapshot) {
                    R.string.local_browser_title_snapshot
                } else {
                    R.string.local_browser_title_live
                }
                Text(stringResource(id = titleRes))
            },
            navigationIcon = {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
        )

        // Content
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            if (isLocalSnapshot && state.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                // Track WebView reference for proper cleanup
                var webView by remember { mutableStateOf<WebView?>(null) }

                // Cleanup WebView when composable leaves the composition
                DisposableEffect(Unit) {
                    onDispose {
                        try {
                            webView?.stopLoading()
                            webView?.clearHistory()
                            webView?.clearCache(true)
                            webView?.destroy()
                            webView = null
                        } catch (e: Exception) {
                            com.orhanobut.logger.Logger.e("❌ Error cleaning up WebView: ${e.message}")
                        }
                    }
                }

                AndroidView(
                    factory = { context ->
                        WebView(context).apply {
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                        }.also { webView = it }
                    },
                    update = { webViewInstance ->
                        if (isLocalSnapshot) {
                            if (state.content.isNotBlank()) {
                                // Sanitize HTML snapshot to prevent XSS
                                val sanitizedContent = com.bernaferrari.changedetection.util.HtmlSanitizer.sanitize(
                                    state.content
                                )
                                val processedContent = sanitizedContent.replaceRelativePathWithAbsolute(safeUrl)
                                webViewInstance.settings.defaultTextEncodingName = state.charset
                                webViewInstance.loadDataWithBaseURL(
                                    /* baseUrl = */ "",
                                    /* data = */ processedContent,
                                    /* mimeType = */ state.mimeType,
                                    /* encoding = */ state.charset,
                                    /* historyUrl = */ ""
                                )
                            } else {
                                webViewInstance.loadUrl("about:blank")
                            }
                        } else {
                            webViewInstance.loadUrl(safeUrl)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
