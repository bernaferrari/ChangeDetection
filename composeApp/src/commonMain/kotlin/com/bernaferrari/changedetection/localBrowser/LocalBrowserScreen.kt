/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.localBrowser

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bernaferrari.changedetection.extensions.replaceRelativePathWithAbsolute
import com.bernaferrari.changedetection.shared.generated.resources.Res
import com.bernaferrari.changedetection.shared.generated.resources.a11y_close
import com.bernaferrari.changedetection.shared.generated.resources.local_browser_empty_snapshot
import com.bernaferrari.changedetection.shared.generated.resources.local_browser_error_body
import com.bernaferrari.changedetection.shared.generated.resources.local_browser_error_title
import com.bernaferrari.changedetection.shared.generated.resources.local_browser_title_live
import com.bernaferrari.changedetection.shared.generated.resources.local_browser_title_snapshot
import com.bernaferrari.changedetection.shared.generated.resources.retry
import com.bernaferrari.changedetection.ui.components.SharedEmptyState
import com.multiplatform.webview.web.WebView
import com.multiplatform.webview.web.rememberWebViewState
import com.multiplatform.webview.web.rememberWebViewStateWithHTMLData
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LocalBrowserScreen(
    snapId: String,
    url: String,
    onDismiss: () -> Unit,
    viewModel: LocalBrowserViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val isLocalSnapshot = snapId.isNotBlank()
    val safeUrl = remember(url) { url.trim() }

    LaunchedEffect(snapId) {
        if (isLocalSnapshot) viewModel.fetchData(snapId) else viewModel.reset()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val titleRes = if (isLocalSnapshot) Res.string.local_browser_title_snapshot else Res.string.local_browser_title_live
                    Text(stringResource(titleRes))
                },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(Res.string.a11y_close))
                    }
                },
            )
        },
    ) { paddingValues ->
        when {
            (isLocalSnapshot && state.hasError) || (!isLocalSnapshot && safeUrl.isBlank()) -> SharedEmptyState(
                icon = Icons.Default.ErrorOutline,
                title = stringResource(Res.string.local_browser_error_title),
                description = stringResource(Res.string.local_browser_error_body),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                primaryActionLabel = if (isLocalSnapshot) stringResource(Res.string.retry) else null,
                onPrimaryAction = if (isLocalSnapshot) ({ viewModel.fetchData(snapId) }) else null,
            )

            isLocalSnapshot && state.isLoading -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center,
            ) {
                LoadingIndicator()
            }

            isLocalSnapshot && state.content.isBlank() -> SharedEmptyState(
                icon = Icons.Default.ErrorOutline,
                title = stringResource(Res.string.local_browser_empty_snapshot),
                description = stringResource(Res.string.local_browser_error_body),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
            )

            isLocalSnapshot -> {
                val content = remember(state.content, safeUrl) {
                    com.bernaferrari.changedetection.util.HtmlSanitizer.sanitize(state.content)
                        .replaceRelativePathWithAbsolute(safeUrl)
                }
                if (state.mimeType.startsWith("text/html", ignoreCase = true)) {
                    WebView(
                        state = rememberWebViewStateWithHTMLData(data = snapshotDocument(content)),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                            .verticalScroll(rememberScrollState())
                            .padding(20.dp),
                    ) {
                        Text(
                            text = state.content,
                            style = MaterialTheme.typography.bodyLarge,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }

            else -> WebView(
                state = rememberWebViewState(url = safeUrl),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
            )
        }
    }
}

private fun snapshotDocument(body: String): String = """
    <!doctype html>
    <html>
      <head>
        <meta name="viewport" content="width=device-width, initial-scale=1" />
        <style>
          :root { color-scheme: light; }
          html, body { min-height: 100%; margin: 0; background: #ffffff; color: #1d1d1f; }
          body {
            box-sizing: border-box;
            padding: 20px;
            font: 16px/1.5 -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
            overflow-wrap: anywhere;
          }
          img, video, canvas, svg { max-width: 100%; height: auto; }
          table { max-width: 100%; border-collapse: collapse; }
        </style>
      </head>
      <body>$body</body>
    </html>
""".trimIndent()
