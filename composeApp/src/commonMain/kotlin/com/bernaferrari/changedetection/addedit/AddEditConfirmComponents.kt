/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.addedit

import androidx.compose.foundation.layout.offset
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.LoadingIndicator
import com.bernaferrari.changedetection.ui.theme.marks
import com.bernaferrari.changedetection.shared.generated.resources.*
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
        if (url.normalizeUrl() != null) step = 1
        onPreview()
    }
    val scrollState = rememberScrollState()
    val urlFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val urlErrorMessage = urlError?.localizedMessage()
    val saveErrorMessage = saveError?.localizedMessage()
    val scope = rememberCoroutineScope()
    var copiedLink by remember { mutableStateOf<String?>(null) }
    var advanced by rememberSaveable { mutableStateOf(false) }
    val clipboard = LocalClipboard.current
    LaunchedEffect(url) {
        if (!isEditing && url.isBlank()) {
            copiedLink = clipboard.readText()?.trim()?.takeIf { it.length < 2_000 && ' ' !in it && (it.startsWith("http://") || it.startsWith("https://") || it.contains('.')) }
        }
    }
    LaunchedEffect(Unit) { if (!isEditing && url.isBlank()) urlFocusRequester.requestFocus() }
    LaunchedEffect(urlError) { if (urlError != null) step = 0 }
    LaunchedEffect(step) { scrollState.scrollTo(0) }
    val textMode = captureMode == com.bernaferrari.changedetection.repo.CaptureMode.TEXT
    val visualMode = captureMode in setOf(com.bernaferrari.changedetection.repo.CaptureMode.SCREENSHOT, com.bernaferrari.changedetection.repo.CaptureMode.DEFAULT)
    fun changeMode(mode: com.bernaferrari.changedetection.repo.CaptureMode) {
        if (mode == captureMode) return
        onCaptureModeChange(mode)
        if (preview !is CapturePreviewState.Idle) onPreview()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            androidx.compose.material3.TopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (isEditing) {
                                Res.string.editor_edit_title
                            } else if (choosingPage) {
                                Res.string.home_watch_page
                            } else {
                                Res.string.editor_review_title
                            },
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
                            contentDescription = stringResource(if (isEditing || step > 0) Res.string.a11y_navigate_back else Res.string.a11y_close),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
        bottomBar = {
            Box(Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.ime).navigationBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp), contentAlignment = Alignment.Center) {
                Button(
                    onClick = { if (choosingPage) continueToContent() else onSave() },
                    enabled = !isSaving && (!choosingPage || url.isNotBlank()),
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = if (choosingPage) ButtonDefaults.buttonColors() else ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer),
                    modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth().heightIn(min = 60.dp),
                ) {
                    if (isSaving) {
                        LoadingIndicator(Modifier.size(24.dp))
                        Spacer(Modifier.size(8.dp))
                        Text(stringResource(Res.string.saving), style = MaterialTheme.typography.titleMedium)
                    } else {
                        Text(
                            stringResource(
                                if (choosingPage) {
                                    Res.string.continue_label
                                } else if (isEditing) {
                                    Res.string.save
                                } else {
                                    Res.string.editor_start
                                },
                            ),
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                }
            }
        },
    ) { paddingValues ->
        Box(Modifier.fillMaxSize().padding(paddingValues)) {
            Column(
                Modifier.align(Alignment.TopCenter).widthIn(max = 600.dp).fillMaxWidth().fillMaxHeight().verticalScroll(scrollState).padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (choosingPage || isEditing) {
                    if (choosingPage) {
                        Text(stringResource(Res.string.editor_paste_title), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(top = 8.dp))
                        Text(stringResource(Res.string.editor_paste_body), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(4.dp))
                    }
                    OutlinedTextField(
                        value = url,
                        onValueChange = onUrlChange,
                        label = { Text(stringResource(Res.string.website_url)) },
                        placeholder = { Text(stringResource(Res.string.website_url_placeholder)) },
                        supportingText = urlErrorMessage?.let { { Text(it) } },
                        isError = urlError != null,
                        singleLine = true,
                        textStyle = MaterialTheme.typography.titleMedium,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = if (choosingPage) ImeAction.Go else ImeAction.Next),
                        keyboardActions = KeyboardActions(onGo = { continueToContent() }),
                        leadingIcon = { Icon(Icons.Default.Language, null) },
                        trailingIcon = if (url.isNotBlank()) {
                            { IconButton(onClick = { onUrlChange("") }) { Icon(Icons.Default.Clear, stringResource(Res.string.clear_page_address)) } }
                        } else {
                            null
                        },
                        modifier = Modifier.fillMaxWidth().semantics { urlErrorMessage?.let { error(it) } }.focusRequester(urlFocusRequester),
                        shape = MaterialTheme.shapes.large,
                        colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant, focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest, unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                    )
                    val link = copiedLink
                    if (choosingPage && url.isBlank() && link != null) {
                        Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.marks.highlightSoft) {
                            Row(Modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Icon(Icons.Default.ContentPaste, null, Modifier.size(20.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(stringResource(Res.string.empty_clipboard_label), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(link.removePrefix("https://").removePrefix("http://"), style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                Button(onClick = {
                                    scope.launch {
                                        onUrlChange(link)
                                        keyboardController?.hide()
                                    }
                                }) { Text(stringResource(Res.string.editor_use_link)) }
                            }
                        }
                    }
                }

                if (duplicateSiteId != null) DuplicatePageWarning(siteId = duplicateSiteId, onOpen = onOpenDuplicate, onDismiss = onDismissDuplicate)

                if (!choosingPage) {
                    if (!isEditing) {
                        Text(url.removePrefix("https://").removePrefix("http://").removePrefix("www."), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    OutlinedTextField(
                        value = title,
                        onValueChange = onTitleChange,
                        label = { Text(stringResource(Res.string.site_name)) },
                        placeholder = { Text(stringResource(Res.string.site_name_placeholder)) },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.titleLarge,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() }),
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant, focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest, unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                    )

                    EditorLabel(stringResource(Res.string.editor_compare))
                    val simpleModes = buildList {
                        add(com.bernaferrari.changedetection.repo.CaptureMode.TEXT)
                        if (com.bernaferrari.changedetection.repo.supportsVisualMonitoring || captureMode == com.bernaferrari.changedetection.repo.CaptureMode.SCREENSHOT) add(com.bernaferrari.changedetection.repo.CaptureMode.SCREENSHOT)
                        if (captureMode !in this) add(captureMode)
                    }
                    if (simpleModes.size > 1) {
                        com.bernaferrari.changedetection.ui.turbo.ConnectedChoice(simpleModes, captureMode, ::changeMode, Modifier.padding(horizontal = 0.dp).offset(x = (-16).dp).fillMaxWidth()) { modeName(it) }
                    }
                    Text(stringResource(modeExplanation(captureMode)), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    EditorLabel(stringResource(Res.string.editor_preview))
                    CapturePreviewPanel(
                        preview,
                        !isSaving && urlError == null && url.isNotBlank(),
                        onPreview,
                        onClosePreview,
                        if (textMode) {
                            { selected ->
                                onTextRegionChange(selected)
                                onPreview()
                            }
                        } else {
                            null
                        },
                        initialRegion = textRegion,
                    )
                    val ready = (preview as? CapturePreviewState.Ready)?.content as? CapturePreview.Html
                    if (textMode && ready != null && ready.text.split(Regex("\\s+")).count { it.isNotBlank() } < 25 && com.bernaferrari.changedetection.repo.supportsVisualMonitoring) {
                        com.bernaferrari.changedetection.ui.kit.Callout(
                            title = stringResource(Res.string.editor_little_text_title),
                            body = stringResource(Res.string.editor_little_text_body),
                            icon = Icons.Default.Warning,
                            container = MaterialTheme.marks.warningSoft,
                            iconTint = MaterialTheme.marks.warning,
                            modifier = Modifier.offset(x = (-12).dp).fillMaxWidth(),
                            actions = { Button(onClick = { changeMode(com.bernaferrari.changedetection.repo.CaptureMode.SCREENSHOT) }) { Text(stringResource(Res.string.editor_use_screenshot)) } },
                        )
                    }

                    if (textMode) {
                        Spacer(Modifier.height(8.dp))
                        TextRegionFields(textRegion, regionError, !isSaving) {
                            onTextRegionChange(it)
                        }
                    }
                    if (visualMode) {
                        EditorLabel(stringResource(Res.string.editor_sensitivity))
                        VisualChangeFilterField(visualChangeThresholdBps, !isSaving, onVisualChangeThresholdChange)
                    }
                    if (legacyThreshold > 0 && !(visualMode && visualChangeThresholdBps > 0) && captureMode !in setOf(com.bernaferrari.changedetection.repo.CaptureMode.TEXT, com.bernaferrari.changedetection.repo.CaptureMode.SOURCE)) {
                        LegacyThresholdNotice(legacyThreshold, !isSaving, onRemoveLegacyThreshold)
                    }

                    TextButton(onClick = { advanced = !advanced }, modifier = Modifier.offset(x = (-12).dp)) {
                        Text(stringResource(Res.string.editor_advanced))
                        Icon(if (advanced) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null)
                    }
                    if (advanced) {
                        CaptureModeField(captureMode, ::changeMode, enabled = !isSaving, isEditing = isEditing)
                    }
                    if (isEditing) {
                        Text(stringResource(Res.string.editor_baseline_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        Text(stringResource(Res.string.monitor_editor_cadence_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                saveErrorMessage?.let { message ->
                    Surface(
                        modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    ) {
                        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ErrorOutline, null)
                            Text(message, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    if (showUrlChangeDialog) AddressChangeDialog(onCancel, onKeepHistory, onStartFresh)
}

@Composable
private fun EditorLabel(text: String) {
    Text(text, Modifier.padding(top = 12.dp), style = MaterialTheme.typography.titleSmall)
}

@Composable
private fun modeName(mode: com.bernaferrari.changedetection.repo.CaptureMode): String = stringResource(
    when (mode) {
        com.bernaferrari.changedetection.repo.CaptureMode.SCREENSHOT -> Res.string.mode_screenshot
        com.bernaferrari.changedetection.repo.CaptureMode.SOURCE -> Res.string.mode_source
        com.bernaferrari.changedetection.repo.CaptureMode.HTML -> Res.string.mode_html
        com.bernaferrari.changedetection.repo.CaptureMode.DEFAULT -> Res.string.mode_default
        com.bernaferrari.changedetection.repo.CaptureMode.TEXT -> Res.string.mode_text
    },
)

private fun modeExplanation(mode: com.bernaferrari.changedetection.repo.CaptureMode) = when (mode) {
    com.bernaferrari.changedetection.repo.CaptureMode.TEXT -> Res.string.editor_text_body
    com.bernaferrari.changedetection.repo.CaptureMode.SCREENSHOT -> Res.string.editor_screenshot_body
    com.bernaferrari.changedetection.repo.CaptureMode.SOURCE -> Res.string.editor_source_body
    com.bernaferrari.changedetection.repo.CaptureMode.HTML -> Res.string.editor_html_body
    com.bernaferrari.changedetection.repo.CaptureMode.DEFAULT -> Res.string.editor_default_body
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
