/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.addedit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.bernaferrari.changedetection.extensions.normalizeUrl
import com.bernaferrari.changedetection.shared.generated.resources.Res
import com.bernaferrari.changedetection.shared.generated.resources.a11y_close
import com.bernaferrari.changedetection.shared.generated.resources.monitor_editor_add_title
import com.bernaferrari.changedetection.shared.generated.resources.monitor_editor_edit_title
import com.bernaferrari.changedetection.shared.generated.resources.monitor_editor_prompt_add
import com.bernaferrari.changedetection.shared.generated.resources.monitor_editor_prompt_add_supporting
import com.bernaferrari.changedetection.shared.generated.resources.monitor_editor_prompt_edit
import com.bernaferrari.changedetection.shared.generated.resources.monitor_editor_prompt_edit_supporting
import com.bernaferrari.changedetection.shared.generated.resources.monitor_editor_private_supporting
import com.bernaferrari.changedetection.shared.generated.resources.monitor_editor_private_title
import com.bernaferrari.changedetection.shared.generated.resources.save
import com.bernaferrari.changedetection.shared.generated.resources.saving
import com.bernaferrari.changedetection.shared.generated.resources.site_name
import com.bernaferrari.changedetection.shared.generated.resources.site_name_placeholder
import com.bernaferrari.changedetection.shared.generated.resources.start_monitoring
import com.bernaferrari.changedetection.shared.generated.resources.website_url
import com.bernaferrari.changedetection.shared.generated.resources.website_url_placeholder
import com.bernaferrari.changedetection.shared.generated.resources.a11y_navigate_back
import com.bernaferrari.changedetection.shared.generated.resources.clear
import com.bernaferrari.changedetection.ui.theme.spacing
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AddEditConfirmScreen(
    url: String,
    urlError: String?,
    title: String,
    isEditing: Boolean = false,
    isSaving: Boolean,
    saveError: String?,
    onUrlChange: (String) -> Unit,
    onTitleChange: (String) -> Unit,
    onNavigateUp: () -> Unit,
    onSave: () -> Unit,
) {
    val spacing = MaterialTheme.spacing
    val scrollState = rememberScrollState()
    val urlFocusRequester = remember { FocusRequester() }
    val titleFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val canSave = url.normalizeUrl() != null && urlError == null && !isSaving
    val websiteUrlLabel = stringResource(Res.string.website_url)
    val siteNameLabel = stringResource(Res.string.site_name)

    LaunchedEffect(Unit) {
        if (!isEditing && url.isBlank()) {
            urlFocusRequester.requestFocus()
        }
    }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (isEditing) Res.string.monitor_editor_edit_title else Res.string.monitor_editor_add_title,
                        ),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        onNavigateUp()
                    }) {
                        Icon(
                            if (isEditing) Icons.AutoMirrored.Filled.ArrowBack else Icons.Default.Close,
                            contentDescription = stringResource(
                                if (isEditing) Res.string.a11y_navigate_back else Res.string.a11y_close,
                            ),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
            ) {
                Box(
                    modifier = Modifier
                        .windowInsetsPadding(WindowInsets.ime)
                        .navigationBarsPadding()
                        .padding(horizontal = spacing.md, vertical = spacing.sm),
                ) {
                    Button(
                        onClick = onSave,
                        enabled = canSave,
                        shapes = ButtonDefaults.shapes(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(MaterialTheme.spacing.touchTarget),
                    ) {
                        if (isSaving) {
                            LoadingIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                            Spacer(modifier = Modifier.size(MaterialTheme.spacing.xs))
                            Text(stringResource(Res.string.saving))
                        } else {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.size(MaterialTheme.spacing.xs))
                            Text(stringResource(if (isEditing) Res.string.save else Res.string.start_monitoring))
                        }
                    }
                }
            }
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .widthIn(max = 600.dp)
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .verticalScroll(scrollState)
                    .padding(horizontal = spacing.lg),
                verticalArrangement = Arrangement.spacedBy(spacing.lg),
            ) {
                Spacer(modifier = Modifier.height(spacing.xs))

                Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                    Text(
                        text = stringResource(
                            if (isEditing) {
                                Res.string.monitor_editor_prompt_edit
                            } else {
                                Res.string.monitor_editor_prompt_add
                            },
                        ),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    )
                    Text(
                        text = stringResource(
                            if (isEditing) {
                                Res.string.monitor_editor_prompt_edit_supporting
                            } else {
                                Res.string.monitor_editor_prompt_add_supporting
                            },
                        ),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                OutlinedTextField(
                    value = url,
                    onValueChange = onUrlChange,
                    label = { Text(websiteUrlLabel) },
                    placeholder = { Text(stringResource(Res.string.website_url_placeholder)) },
                    supportingText = if (urlError != null) {
                        { Text(urlError) }
                    } else {
                        null
                    },
                    isError = urlError != null,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Next,
                    ),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = null,
                        )
                    },
                    trailingIcon = if (url.isNotBlank()) {
                        {
                            IconButton(onClick = { onUrlChange("") }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = stringResource(Res.string.clear),
                                )
                            }
                        }
                    } else {
                        null
                    },
                    keyboardActions = KeyboardActions(
                        onNext = { titleFocusRequester.requestFocus() },
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = websiteUrlLabel }
                        .focusRequester(urlFocusRequester),
                    shape = MaterialTheme.shapes.medium,
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    ),
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = onTitleChange,
                    label = { Text(siteNameLabel) },
                    placeholder = { Text(stringResource(Res.string.site_name_placeholder)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (canSave) {
                                keyboardController?.hide()
                                onSave()
                            }
                        },
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = siteNameLabel }
                        .focusRequester(titleFocusRequester),
                    shape = MaterialTheme.shapes.medium,
                    colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant),
                )

                if (!isEditing) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = spacing.xs),
                        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Lock,
                            contentDescription = null,
                            modifier = Modifier.padding(top = 1.dp).size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(1.dp),
                        ) {
                            Text(
                                text = stringResource(Res.string.monitor_editor_private_title),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                            )
                            Text(
                                text = stringResource(Res.string.monitor_editor_private_supporting),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                saveError?.let { message ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { liveRegion = LiveRegionMode.Polite },
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    ) {
                        Row(
                            modifier = Modifier.padding(MaterialTheme.spacing.md),
                            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = null,
                            )
                            Text(
                                text = message,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(MaterialTheme.spacing.lg))
            }
        }
    }
}
