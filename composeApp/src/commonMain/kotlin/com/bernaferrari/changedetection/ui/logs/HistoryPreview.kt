/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.ui.logs

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalDensity
import org.jetbrains.compose.resources.stringResource
import com.bernaferrari.changedetection.shared.generated.resources.*
import com.bernaferrari.changedetection.repo.Snap
import com.bernaferrari.changedetection.ui.comparison.SnapshotImage
import com.bernaferrari.changedetection.ui.detail.SavedPagePreview

/** Reads the immutable saved capture; disposing a lazy-list row cancels its load. */
@Composable
internal fun HistoryPreview(snap: Snap, load: suspend (Snap) -> HistoryPreviewContent) {
    key(snap.snapId) {
        val preview by produceState<HistoryPreviewContent>(HistoryPreviewContent.Capture(SavedPagePreview.Loading)) {
            value = load(snap)
        }
        // Reserve the preview space during loading so arriving content does not move
        // the next card under the user's pointer. No network or acknowledgement here.
        Box(Modifier.fillMaxWidth().padding(end = 8.dp).height(with(LocalDensity.current) { 96.sp.toDp() + 12.dp }).clip(MaterialTheme.shapes.small)) {
            val passage = preview as? HistoryPreviewContent.Passage
            if (passage != null) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    PassageLine(stringResource(Res.string.comparison_mode_before), passage.before)
                    PassageLine(stringResource(Res.string.comparison_mode_after), passage.after)
                }
            } else {
                when (val content = (preview as HistoryPreviewContent.Capture).preview) {
                    is SavedPagePreview.Image -> SnapshotImage(content.bytes, null, Modifier.fillMaxSize(), ContentScale.Crop)

                    is SavedPagePreview.Text -> Text(
                        content.excerpt,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )

                    else -> Text(
                        stringResource(
                            when (content) {
                                SavedPagePreview.Loading -> Res.string.loading
                                SavedPagePreview.Unavailable -> Res.string.detail_preview_unavailable
                                else -> Res.string.detail_saved_document
                            },
                        ),
                        Modifier.align(Alignment.CenterStart),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun PassageLine(label: String, text: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        Text(text.ifEmpty { "—" }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, minLines = 2, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}
