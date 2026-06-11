/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.browser

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.multiplatform.webview.web.WebView
import com.multiplatform.webview.web.rememberWebViewState
import com.multiplatform.webview.web.rememberWebViewNavigator
import com.multiplatform.webview.web.LoadingState

/**
 * iOS implementation of the browser screen.
 * Uses the multiplatform webview library for consistent behavior.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
actual fun ModernInteractiveBrowserScreen(
    initialUrl: String,
    initialSession: BrowserSessionData?,
    onNavigateUp: () -> Unit,
    onSaveComplete: (BrowserSessionData) -> Unit,
) {
    var currentUrl by remember { mutableStateOf(initialUrl) }
    var pageTitle by remember { mutableStateOf("") }

    val webViewState = rememberWebViewState(url = currentUrl)
    val navigator = rememberWebViewNavigator()

    // Update page title when page loads
    LaunchedEffect(webViewState.pageTitle) {
        pageTitle = webViewState.pageTitle ?: ""
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Configure Site",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.SemiBold,
                            ),
                        )
                        if (pageTitle.isNotBlank()) {
                            Text(
                                text = pageTitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { navigator.reload() }) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Refresh",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    onSaveComplete(
                        BrowserSessionData(
                            url = currentUrl,
                            title = pageTitle,
                            cookies = "",
                            userAgent = "",
                            selectedElements = emptyList(),
                            zappedElements = emptyList(),
                        ),
                    )
                },
                icon = { Icon(Icons.Default.Check, contentDescription = null) },
                text = { Text("Save Site") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            // URL Bar
            OutlinedTextField(
                value = currentUrl,
                onValueChange = { currentUrl = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                singleLine = true,
                placeholder = { Text("Enter URL") },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    imeAction = androidx.compose.ui.text.input.ImeAction.Go,
                ),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                    onGo = {
                        val url = currentUrl.trim()
                        if (url.isNotBlank()) {
                            // Ensure URL has protocol
                            val finalUrl = if (url.startsWith("http://") || url.startsWith("https://")) {
                                url
                            } else {
                                "https://$url"
                            }
                            currentUrl = finalUrl
                            navigator.loadUrl(finalUrl)
                        }
                    },
                ),
            )

            // Loading indicator
            if (webViewState.loadingState is LoadingState.Loading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // WebView
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
            ) {
                WebView(
                    state = webViewState,
                    navigator = navigator,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}
