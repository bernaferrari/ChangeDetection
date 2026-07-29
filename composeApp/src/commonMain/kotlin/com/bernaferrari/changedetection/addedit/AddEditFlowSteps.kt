/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.addedit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.bernaferrari.changedetection.extensions.isValidUrl
import com.bernaferrari.changedetection.shared.generated.resources.Res
import com.bernaferrari.changedetection.shared.generated.resources.a11y_navigate_back
import com.bernaferrari.changedetection.shared.generated.resources.add_edit_url_description
import com.bernaferrari.changedetection.shared.generated.resources.continue_label
import com.bernaferrari.changedetection.shared.generated.resources.enter_website_url
import com.bernaferrari.changedetection.shared.generated.resources.paste
import com.bernaferrari.changedetection.shared.generated.resources.website_url
import com.bernaferrari.changedetection.shared.generated.resources.website_url_placeholder
import com.bernaferrari.changedetection.ui.theme.spacing
import com.bernaferrari.changedetection.util.readText
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/**
 * A single-purpose first step. It validates an address without embedding a fragile browser inside
 * setup; the saved monitor remains the source of truth for content and rendering.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UrlInputStep(
    url: String,
    urlError: String?,
    onUrlChange: (String) -> Unit,
    onNavigateUp: () -> Unit,
    onContinue: () -> Unit,
) {
    val clipboard: Clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val isValid = url.isNotBlank() && url.isValidUrl() && urlError == null

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.website_url)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.a11y_navigate_back),
                        )
                    }
                },
            )
        },
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
                Button(
                    onClick = onContinue,
                    enabled = isValid,
                    modifier = Modifier
                        .fillMaxWidth()
                        .imePadding()
                        .padding(horizontal = MaterialTheme.spacing.lg, vertical = MaterialTheme.spacing.sm),
                ) {
                    Text(stringResource(Res.string.continue_label))
                }
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .widthIn(max = 600.dp)
                    .fillMaxWidth()
                    .padding(horizontal = MaterialTheme.spacing.lg),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = MaterialTheme.spacing.xl),
                ) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp),
                    )
                }
                Text(
                    text = stringResource(Res.string.enter_website_url),
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    text = stringResource(Res.string.add_edit_url_description),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = onUrlChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(Res.string.website_url)) },
                    placeholder = { Text(stringResource(Res.string.website_url_placeholder)) },
                    singleLine = true,
                    isError = urlError != null,
                    supportingText = urlError?.let { message -> { Text(message) } },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(onDone = { if (isValid) onContinue() }),
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                scope.launch {
                                    clipboard.readText()
                                        ?.trim()
                                        ?.takeIf { it.isNotBlank() }
                                        ?.let(onUrlChange)
                                }
                            },
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentPaste,
                                contentDescription = stringResource(Res.string.paste),
                            )
                        }
                    },
                )
            }
        }
    }
}
