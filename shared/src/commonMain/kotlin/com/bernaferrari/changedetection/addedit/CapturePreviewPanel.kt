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
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.testTag
import com.bernaferrari.changedetection.shared.generated.resources.*
import com.bernaferrari.changedetection.ui.comparison.SnapshotImage
import com.bernaferrari.changedetection.localization.stringResource
import com.bernaferrari.changedetection.localization.pluralStringResource
import com.bernaferrari.changedetection.shared.generated.resources.editor_words

@OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun CapturePreviewPanel(state: CapturePreviewState, enabled: Boolean, onPreview: () -> Unit, onClose: () -> Unit, onSelectRegion: ((com.bernaferrari.changedetection.detection.TextRegion) -> Unit)? = null, initialRegion: com.bernaferrari.changedetection.detection.TextRegion = com.bernaferrari.changedetection.detection.TextRegion(), bare: Boolean = false) {
    if (bare && state is CapturePreviewState.Ready && state.content is CapturePreview.Image) {
        ScreenshotPreview(state.content)
        return
    }
    if (state is CapturePreviewState.Idle) {
        OutlinedButton(onClick = onPreview, enabled = enabled, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
            Text(stringResource(Res.string.capture_preview_action))
        }
        return
    }
    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceBright, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            when (state) {
                CapturePreviewState.Loading -> Row(Modifier.heightIn(min = 72.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    androidx.compose.material3.LoadingIndicator(Modifier.size(36.dp))
                    Text(stringResource(Res.string.capture_preview_loading), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    TextButton(onClick = onClose) { Text(stringResource(Res.string.cancel)) }
                }

                is CapturePreviewState.Failed -> {
                    Text(
                        stringResource(
                            when (state.reason) {
                                PreviewFailure.TIMEOUT -> Res.string.check_failure_timeout_detail
                                PreviewFailure.REGION -> Res.string.check_failure_region_detail
                                PreviewFailure.UNAVAILABLE -> Res.string.check_failure_unknown_detail
                            },
                        ),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    OutlinedButton(onClick = onPreview, enabled = enabled) { Text(stringResource(Res.string.retry)) }
                }

                is CapturePreviewState.Ready -> when (val content = state.content) {
                    is CapturePreview.Html -> {
                        val words = remember(content.text) { content.text.split(Regex("\\s+")).count { it.isNotBlank() } }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(pluralStringResource(Res.plurals.editor_words, words, words), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                            TextButton(onClick = onPreview, enabled = enabled) { Text(stringResource(Res.string.refresh)) }
                        }
                        Text(
                            content.text.ifBlank { stringResource(Res.string.capture_preview_empty) },
                            modifier = Modifier.fillMaxWidth().heightIn(max = 240.dp).verticalScroll(rememberScrollState()),
                            style = com.bernaferrari.changedetection.ui.theme.Reading.small.copy(color = MaterialTheme.colorScheme.onSurface),
                        )
                        if (onSelectRegion != null && content.selectableDocument != null) {
                            TextRegionPickerAction(content.selectableDocument, initialRegion, onSelectRegion)
                        }
                    }

                    is CapturePreview.Image -> ScreenshotPreview(content)
                }

                CapturePreviewState.Idle -> Unit
            }
        }
    }
}

@Composable
private fun ScreenshotPreview(content: CapturePreview.Image) {
    var expanded by remember(content.bytes) { mutableStateOf(false) }
    // The top of the page at full width, fading out: that is what people recognise, not a full-page sliver.
    Box(
        Modifier.fillMaxWidth().height(320.dp).clip(MaterialTheme.shapes.large)
            .background(androidx.compose.ui.graphics.Color.White)
            .testTag("browser-preview").clickable(enabled = !content.isPreparing) { expanded = true },
    ) {
        com.bernaferrari.changedetection.ui.comparison.SnapshotImage(content.bytes, stringResource(Res.string.capture_preview_title), Modifier.fillMaxWidth().wrapContentHeight(Alignment.Top, unbounded = true), ContentScale.FillWidth)
        Box(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(96.dp).background(
                androidx.compose.ui.graphics.Brush.verticalGradient(listOf(androidx.compose.ui.graphics.Color.Transparent, MaterialTheme.colorScheme.surfaceBright)),
            ),
        )
        if (content.isPreparing) {
            Surface(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp), shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.secondaryContainer) {
                Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    Text(stringResource(Res.string.capture_screenshot_finishing), style = MaterialTheme.typography.labelLarge)
                }
            }
        } else {
            FilledTonalButton(onClick = { expanded = true }, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp)) {
                Text(stringResource(Res.string.capture_preview_expand))
            }
        }
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
