/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.localBrowser

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bernaferrari.changedetection.shared.generated.resources.Res
import com.bernaferrari.changedetection.shared.generated.resources.*
import com.bernaferrari.changedetection.extensions.replaceRelativePathWithAbsolute
import com.multiplatform.webview.web.WebView
import com.multiplatform.webview.web.rememberWebViewStateWithHTMLData
import com.multiplatform.webview.web.rememberWebViewState
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalBrowserScreen(
    snapId: String,
    url: String,
    onDismiss: () -> Unit,
    viewModel: LocalBrowserViewModel = koinViewModel(),
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
        modifier = Modifier.fillMaxSize(),
    ) {
        // Top bar
        TopAppBar(
            title = {
                val titleRes = if (isLocalSnapshot) {
                    Res.string.local_browser_title_snapshot
                } else {
                    Res.string.local_browser_title_live
                }
                Text(stringResource(titleRes))
            },
            navigationIcon = {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            },
        )

        // Content
        Box(
            modifier = Modifier.fillMaxSize(),
        ) {
            if (isLocalSnapshot && state.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                )
            } else {
                if (isLocalSnapshot) {
                    // Local snapshot - load HTML data
                    if (state.content.isNotBlank()) {
                        val sanitizedContent = com.bernaferrari.changedetection.util.HtmlSanitizer.sanitize(
                            state.content,
                        )
                        val processedContent = sanitizedContent.replaceRelativePathWithAbsolute(safeUrl)

                        val webViewState = rememberWebViewStateWithHTMLData(
                            data = processedContent,
                        )

                        WebView(
                            state = webViewState,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                } else {
                    // Live URL - load remote URL
                    val webViewState = rememberWebViewState(url = safeUrl)

                    WebView(
                        state = webViewState,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}
