/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.addedit

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.bernaferrari.changedetection.shared.generated.resources.*
import com.bernaferrari.changedetection.ui.comparison.SnapshotImage
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun CapturePreviewPanel(state: CapturePreviewState, enabled: Boolean, onPreview: () -> Unit, onClose: () -> Unit, onSelectRegion: ((com.bernaferrari.changedetection.detection.TextRegion) -> Unit)? = null, initialRegion: com.bernaferrari.changedetection.detection.TextRegion = com.bernaferrari.changedetection.detection.TextRegion()) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (state is CapturePreviewState.Idle) {
            OutlinedButton(onClick = onPreview, enabled = enabled, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text(stringResource(Res.string.capture_preview_action))
            }
        }
        if (state !is CapturePreviewState.Idle) {
            Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerLow) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(stringResource(Res.string.capture_preview_title), Modifier.padding(vertical = 12.dp, horizontal = 0.dp), style = MaterialTheme.typography.titleMedium)
                        if (state !is CapturePreviewState.Loading) {
                            TextButton(onClick = onPreview, enabled = enabled) {
                                Text(stringResource(if (state is CapturePreviewState.Failed) Res.string.retry else Res.string.refresh))
                            }
                        }
                    }
                    when (state) {
                        CapturePreviewState.Loading -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                            Text(stringResource(Res.string.capture_preview_loading), style = MaterialTheme.typography.bodyMedium)
                        }

                        is CapturePreviewState.Failed -> Text(
                            stringResource(
                                when (state.reason) {
                                    PreviewFailure.TIMEOUT -> Res.string.check_failure_timeout_detail
                                    PreviewFailure.REGION -> Res.string.check_failure_region_detail
                                    PreviewFailure.UNAVAILABLE -> Res.string.check_failure_unknown_detail
                                },
                            ),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                        )

                        is CapturePreviewState.Ready -> when (val content = state.content) {
                            is CapturePreview.Html -> {
                                if (onSelectRegion != null && content.selectableDocument != null) {
                                    TextRegionPickerAction(content.selectableDocument, initialRegion, onSelectRegion)
                                }
                                Text(stringResource(Res.string.capture_preview_html), style = MaterialTheme.typography.labelLarge)
                                Text(
                                    content.text.ifBlank { stringResource(Res.string.capture_preview_empty) },
                                    modifier = Modifier.fillMaxWidth().heightIn(max = 280.dp).verticalScroll(rememberScrollState()),
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }

                            is CapturePreview.Image -> {
                                Text(stringResource(Res.string.capture_mode_screenshot), style = MaterialTheme.typography.labelLarge)
                                ScreenshotPreview(content)
                            }
                        }

                        CapturePreviewState.Idle -> Unit
                    }
                    Text(stringResource(Res.string.capture_preview_unsaved), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TextButton(onClick = onClose) { Text(stringResource(if (state is CapturePreviewState.Loading) Res.string.cancel else Res.string.capture_preview_close)) }
                }
            }
        }
    }
}

@Composable
private fun ScreenshotPreview(content: CapturePreview.Image) {
    var expanded by remember(content.bytes) { mutableStateOf(false) }
    SnapshotImage(
        content.bytes,
        stringResource(Res.string.capture_preview_title),
        Modifier.fillMaxWidth().height(240.dp),
        ContentScale.Fit,
    )
    TextButton(onClick = { expanded = true }) {
        Text(stringResource(Res.string.capture_preview_expand))
    }
    if (expanded) {
        Dialog(onDismissRequest = { expanded = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
                Column(Modifier.fillMaxSize().safeDrawingPadding()) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(stringResource(Res.string.capture_preview_title), Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
                        TextButton(onClick = { expanded = false }) { Text(stringResource(Res.string.done)) }
                    }
                    Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
                        SnapshotImage(content.bytes, stringResource(Res.string.capture_preview_title), Modifier.fillMaxWidth(), ContentScale.FillWidth)
                    }
                }
            }
        }
    }
}
