/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.localBrowser

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
                CircularProgressIndicator()
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
                WebView(
                    state = rememberWebViewStateWithHTMLData(data = content),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                )
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
