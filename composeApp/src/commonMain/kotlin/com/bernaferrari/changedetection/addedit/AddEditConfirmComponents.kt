/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.addedit

import com.bernaferrari.changedetection.extensions.normalizeUrl
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import com.bernaferrari.changedetection.shared.generated.resources.setup_page_question
import com.bernaferrari.changedetection.shared.generated.resources.setup_page_supporting
import com.bernaferrari.changedetection.shared.generated.resources.setup_content_heading
import com.bernaferrari.changedetection.shared.generated.resources.setup_step_page
import com.bernaferrari.changedetection.shared.generated.resources.setup_step_content

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import com.bernaferrari.changedetection.ui.components.SignalHeroShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import com.bernaferrari.changedetection.ui.theme.spacing
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bernaferrari.changedetection.util.readText
import kotlinx.coroutines.launch
import com.bernaferrari.changedetection.shared.generated.resources.Res
import com.bernaferrari.changedetection.shared.generated.resources.a11y_close
import com.bernaferrari.changedetection.shared.generated.resources.monitor_editor_add_title
import com.bernaferrari.changedetection.shared.generated.resources.monitor_editor_baseline_hint
import com.bernaferrari.changedetection.shared.generated.resources.monitor_editor_cadence_hint
import com.bernaferrari.changedetection.shared.generated.resources.monitor_editor_duplicate_open
import com.bernaferrari.changedetection.shared.generated.resources.monitor_editor_duplicate_warning
import com.bernaferrari.changedetection.shared.generated.resources.monitor_editor_edit_title
import com.bernaferrari.changedetection.shared.generated.resources.monitor_editor_error_invalid_url
import com.bernaferrari.changedetection.shared.generated.resources.monitor_editor_error_load
import com.bernaferrari.changedetection.shared.generated.resources.monitor_editor_error_not_found
import com.bernaferrari.changedetection.shared.generated.resources.monitor_editor_error_save
import com.bernaferrari.changedetection.shared.generated.resources.monitor_editor_prompt_edit_supporting
import com.bernaferrari.changedetection.shared.generated.resources.monitor_editor_step_label
import com.bernaferrari.changedetection.shared.generated.resources.monitor_editor_preview_title
import com.bernaferrari.changedetection.shared.generated.resources.monitor_editor_preview_supporting
import com.bernaferrari.changedetection.shared.generated.resources.monitor_editor_private_supporting
import com.bernaferrari.changedetection.shared.generated.resources.cancel
import com.bernaferrari.changedetection.shared.generated.resources.save
import com.bernaferrari.changedetection.shared.generated.resources.saving
import com.bernaferrari.changedetection.shared.generated.resources.site_name
import com.bernaferrari.changedetection.shared.generated.resources.site_name_placeholder
import com.bernaferrari.changedetection.shared.generated.resources.start_monitoring
import com.bernaferrari.changedetection.shared.generated.resources.website_url
import com.bernaferrari.changedetection.shared.generated.resources.website_url_placeholder
import com.bernaferrari.changedetection.shared.generated.resources.a11y_navigate_back
import com.bernaferrari.changedetection.shared.generated.resources.clear_page_address
import com.bernaferrari.changedetection.shared.generated.resources.continue_label
import com.bernaferrari.changedetection.ui.main.ChangeMark
import com.bernaferrari.changedetection.shared.generated.resources.address_changed_keep_history
import com.bernaferrari.changedetection.shared.generated.resources.address_changed_message
import com.bernaferrari.changedetection.shared.generated.resources.address_changed_start_fresh
import com.bernaferrari.changedetection.shared.generated.resources.address_changed_title
import com.bernaferrari.changedetection.shared.generated.resources.monitor_editor_private_title
import com.bernaferrari.changedetection.shared.generated.resources.paste
import com.bernaferrari.changedetection.shared.generated.resources.prune_confirm_message
import com.bernaferrari.changedetection.shared.generated.resources.prune_confirm_title
import com.bernaferrari.changedetection.ui.components.ConfirmDialog
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AddEditConfirmScreen(
    url: String,
    urlError: AddEditError?,
    title: String,
    captureMode: com.bernaferrari.changedetection.repo.CaptureMode = com.bernaferrari.changedetection.repo.CaptureMode.DEFAULT,
    onCaptureModeChange: (com.bernaferrari.changedetection.repo.CaptureMode) -> Unit = {},
    preview: CapturePreviewState = CapturePreviewState.Idle,
    legacyThreshold: Int = 0,
    visualChangeThresholdBps: Int = 0,
    onVisualChangeThresholdChange: (Int) -> Unit = {},
    textRegion: com.bernaferrari.changedetection.detection.TextRegion = com.bernaferrari.changedetection.detection.TextRegion(),
    regionError: Boolean = false,
    onTextRegionChange: (com.bernaferrari.changedetection.detection.TextRegion) -> Unit = {},
    onRemoveLegacyThreshold: () -> Unit = {},
    onPreview: () -> Unit = {},
    onClosePreview: () -> Unit = {},
    isEditing: Boolean = false,
    isSaving: Boolean,
    saveError: AddEditError?,
    duplicateSiteId: String? = null,
    onOpenDuplicate: (String) -> Unit = {},
    onDismissDuplicate: () -> Unit = {},
    onUrlChange: (String) -> Unit,
    onTitleChange: (String) -> Unit,
    onNavigateUp: () -> Unit,
    onSave: () -> Unit,
    showUrlChangeDialog: Boolean = false,
    onCancel: () -> Unit = {},
    onKeepHistory: () -> Unit = {},
    onStartFresh: () -> Unit = {},
) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    val choosingPage = !isEditing && step == 0
    val continueToContent = {
        if (url.normalizeUrl() != null) {
            step = 1
        }
        onPreview()
    }
    val scrollState = rememberScrollState()
    val urlFocusRequester = remember { FocusRequester() }
    val titleFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val websiteUrlLabel = stringResource(Res.string.website_url)
    val siteNameLabel = stringResource(Res.string.site_name)
    val urlErrorMessage = urlError?.localizedMessage()
    val saveErrorMessage = saveError?.localizedMessage()
    val pasteLabel = stringResource(Res.string.paste)
    val scope = rememberCoroutineScope()
    var clipboardHasText by remember { mutableStateOf(false) }
    val spacing = MaterialTheme.spacing

    val clipboard = LocalClipboard.current
    LaunchedEffect(url) {
        if (!isEditing && url.isBlank()) {
            clipboardHasText = clipboard.readText()?.isNotBlank() == true
        }
    }

    LaunchedEffect(Unit) {
        if (!isEditing && url.isBlank()) {
            urlFocusRequester.requestFocus()
        }
    }
    LaunchedEffect(urlError) {
        if (urlError != null) {
            step = 0
        }
    }
    LaunchedEffect(step) { scrollState.scrollTo(0) }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            androidx.compose.material3.CenterAlignedTopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (isEditing) Res.string.monitor_editor_edit_title else Res.string.monitor_editor_add_title,
                        ),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (!isEditing && step > 0) {
                            step = 0
                            onClosePreview()
                        } else {
                            onNavigateUp()
                        }
                    }) {
                        Icon(
                            if (isEditing || step > 0) Icons.AutoMirrored.Filled.ArrowBack else Icons.Default.Close,
                            contentDescription = stringResource(
                                if (isEditing || step > 0) Res.string.a11y_navigate_back else Res.string.a11y_close,
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
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                ) {
                    Button(
                        onClick = { if (choosingPage) continueToContent() else onSave() },
                        enabled = !isSaving,
                        shape = MaterialTheme.shapes.large,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp),
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
                                imageVector = if (choosingPage) Icons.AutoMirrored.Filled.ArrowForward else Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.size(MaterialTheme.spacing.xs))
                            Text(
                                stringResource(
                                    if (choosingPage) {
                                        Res.string.continue_label
                                    } else if (isEditing) {
                                        Res.string.save
                                    } else {
                                        Res.string.start_monitoring
                                    },
                                ),
                            )
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
                verticalArrangement = Arrangement.spacedBy(spacing.md),
            ) {
                Spacer(modifier = Modifier.height(spacing.xs))

                if (!isEditing) {
                    Text(stringResource(if (choosingPage) Res.string.setup_step_page else Res.string.setup_step_content), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(16.dp))
                    Text(stringResource(if (choosingPage) Res.string.setup_page_question else Res.string.setup_content_heading), style = MaterialTheme.typography.headlineLarge, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
                    Text(if (choosingPage) stringResource(Res.string.setup_page_supporting) else url, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(16.dp))
                } else {
                    Text(
                        text = stringResource(Res.string.monitor_editor_prompt_edit_supporting),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = spacing.xs),
                    )
                }

                if (choosingPage || isEditing) {
                    OutlinedTextField(
                        value = url,
                        onValueChange = onUrlChange,
                        label = { Text(websiteUrlLabel) },
                        placeholder = { Text(stringResource(Res.string.website_url_placeholder)) },
                        supportingText = when {
                            urlErrorMessage != null -> {
                                { Text(urlErrorMessage) }
                            }

                            else -> null
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
                                        contentDescription = stringResource(Res.string.clear_page_address),
                                    )
                                }
                            }
                        } else if (clipboardHasText) {
                            {
                                TextButton(
                                    onClick = {
                                        scope.launch {
                                            val text = clipboard.readText()?.trim()
                                            if (!text.isNullOrBlank()) {
                                                onUrlChange(text)
                                            }
                                        }
                                    },
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentPaste,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Spacer(modifier = Modifier.width(spacing.xs))
                                    Text(pasteLabel)
                                }
                            }
                        } else {
                            null
                        },
                        keyboardActions = KeyboardActions(
                            onNext = { if (choosingPage) continueToContent() else titleFocusRequester.requestFocus() },
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics {
                                urlErrorMessage?.let { error(it) }
                            }
                            .focusRequester(urlFocusRequester),
                        shape = MaterialTheme.shapes.medium,
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        ),
                    )
                }

                if (duplicateSiteId != null) {
                    DuplicatePageWarning(
                        siteId = duplicateSiteId,
                        onOpen = onOpenDuplicate,
                        onDismiss = onDismissDuplicate,
                    )
                }

                if (!choosingPage) {
                    CapturePreviewPanel(
                        preview,
                        !isSaving && urlError == null && url.isNotBlank(),
                        onPreview,
                        onClosePreview,
                        if (captureMode == com.bernaferrari.changedetection.repo.CaptureMode.TEXT) {
                            { selected ->
                                onTextRegionChange(selected)
                                onPreview()
                            }
                        } else {
                            null
                        },
                        initialRegion = textRegion,
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
                                if (!isSaving) {
                                    keyboardController?.hide()
                                    onSave()
                                }
                            },
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(titleFocusRequester),
                        shape = MaterialTheme.shapes.medium,
                        colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant),
                    )

                    val visualMode = captureMode in setOf(com.bernaferrari.changedetection.repo.CaptureMode.SCREENSHOT, com.bernaferrari.changedetection.repo.CaptureMode.DEFAULT)
                    CaptureModeField(captureMode, onCaptureModeChange, enabled = !isSaving, isEditing = isEditing)
                    if (visualMode) {
                        VisualChangeFilterField(visualChangeThresholdBps, !isSaving, onVisualChangeThresholdChange)
                    }
                    if (captureMode == com.bernaferrari.changedetection.repo.CaptureMode.TEXT) {
                        TextRegionFields(textRegion, regionError, !isSaving, onTextRegionChange)
                    }
                    if (legacyThreshold > 0 && !(visualMode && visualChangeThresholdBps > 0) && captureMode !in setOf(com.bernaferrari.changedetection.repo.CaptureMode.TEXT, com.bernaferrari.changedetection.repo.CaptureMode.SOURCE)) {
                        LegacyThresholdNotice(legacyThreshold, !isSaving, onRemoveLegacyThreshold)
                    }
                }

                if (!isEditing && !choosingPage) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = spacing.xs),
                        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Schedule,
                            contentDescription = null,
                            modifier = Modifier.padding(top = 1.dp).size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = stringResource(Res.string.monitor_editor_cadence_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                saveErrorMessage?.let { message ->
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

    if (showUrlChangeDialog) {
        AddressChangeDialog(onCancel, onKeepHistory, onStartFresh)
    }
}

@Composable
private fun DuplicatePageWarning(
    siteId: String,
    onOpen: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val spacing = MaterialTheme.spacing
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Polite },
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
    ) {
        Column(
            modifier = Modifier.padding(spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                verticalAlignment = Alignment.Top,
            ) {
                Icon(imageVector = Icons.Outlined.Info, contentDescription = null)
                Text(
                    text = stringResource(Res.string.monitor_editor_duplicate_warning),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(Res.string.cancel))
                }
                TextButton(onClick = { onOpen(siteId) }) {
                    Text(stringResource(Res.string.monitor_editor_duplicate_open))
                }
            }
        }
    }
}

@Composable
private fun AddEditError.localizedMessage(): String = stringResource(
    when (this) {
        AddEditError.InvalidUrl -> Res.string.monitor_editor_error_invalid_url
        AddEditError.SiteNotFound -> Res.string.monitor_editor_error_not_found
        AddEditError.LoadFailed -> Res.string.monitor_editor_error_load
        AddEditError.SaveFailed -> Res.string.monitor_editor_error_save
    },
)

@Composable
private fun MonitorEditorHero() {
    val spacing = MaterialTheme.spacing
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = SignalHeroShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(spacing.md),
            horizontalArrangement = Arrangement.spacedBy(spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(spacing.xs),
            ) {
                Text(
                    text = stringResource(Res.string.monitor_editor_step_label),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = stringResource(Res.string.monitor_editor_preview_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                )
                Text(
                    text = stringResource(Res.string.monitor_editor_preview_supporting),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                )
            }
            ChangeMark()
        }
    }
}
