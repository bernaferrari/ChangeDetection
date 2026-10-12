package com.bernaferrari.changedetection.ui.detail

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.bernaferrari.changedetection.repo.Snap
import com.bernaferrari.changedetection.repo.source.SnapsRepository
import com.bernaferrari.changedetection.shared.generated.resources.*
import com.bernaferrari.changedetection.ui.comparison.SnapshotImage
import com.bernaferrari.changedetection.util.decodeByteArray
import com.bernaferrari.changedetection.util.readableSnapshotText
import com.bernaferrari.changedetection.util.formatTimestamp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.bernaferrari.changedetection.localization.stringResource

internal sealed interface SavedPagePreview {
    data object Loading : SavedPagePreview
    data object Unavailable : SavedPagePreview
    data object Document : SavedPagePreview
    data class Text(val excerpt: String) : SavedPagePreview
    data class Image(val bytes: ByteArray) : SavedPagePreview
}

/** Loads one immutable saved version, never the current website and never acknowledges a change. */
internal suspend fun loadSavedPagePreview(snap: Snap, repository: SnapsRepository): SavedPagePreview = try {
    when {
        snap.contentType.startsWith("image/") -> SavedPagePreview.Image(repository.getSnapContent(snap.snapId))

        snap.contentType.startsWith("text/") || snap.contentType.contains("html") || snap.contentType.contains("json") -> {
            val bytes = repository.getSnapContent(snap.snapId)
            withContext(Dispatchers.Default) {
                SavedPagePreview.Text(readableSnapshotText(decodeByteArray(bytes, snap.contentCharset), snap.contentType.contains("html")).take(1200))
            }
        }

        else -> SavedPagePreview.Document
    }
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (_: Exception) {
    SavedPagePreview.Unavailable
}

@Composable
internal fun rememberSavedPagePreview(snap: Snap?, repository: SnapsRepository): SavedPagePreview {
    // Key the whole state holder, so a different page/version cannot display the previous preview.
    return key(snap?.snapId) {
        val preview by produceState<SavedPagePreview>(SavedPagePreview.Loading) {
            if (snap != null) value = loadSavedPagePreview(snap, repository)
        }
        preview
    }
}

@Composable
internal fun SavedPageTeaser(snap: Snap, preview: SavedPagePreview, canCompare: Boolean = true, onOpen: () -> Unit) {
    Card(onClick = onOpen, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
            Box(
                Modifier.fillMaxWidth().heightIn(min = if (preview is SavedPagePreview.Image || (preview == SavedPagePreview.Loading && snap.contentType.startsWith("image/"))) 208.dp else 96.dp),
                contentAlignment = Alignment.TopStart,
            ) {
                when (preview) {
                    is SavedPagePreview.Image -> SnapshotImage(preview.bytes, null, Modifier.fillMaxWidth().height(208.dp), ContentScale.Crop)

                    is SavedPagePreview.Text -> Text(
                        preview.excerpt.ifBlank { stringResource(Res.string.detail_empty_text) },
                        Modifier.padding(20.dp),
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 7,
                        overflow = TextOverflow.Ellipsis,
                    )

                    else -> Column(Modifier.align(Alignment.Center).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Outlined.Description, null, Modifier.size(32.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            stringResource(
                                when (preview) {
                                    SavedPagePreview.Loading -> Res.string.loading
                                    SavedPagePreview.Unavailable -> Res.string.detail_preview_unavailable
                                    else -> Res.string.detail_saved_document
                                },
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        }
        FlowRow(
            Modifier.fillMaxWidth().padding(18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(Modifier.padding(end = 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(Res.string.detail_last_saved), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(formatTimestamp(snap.timestamp, includeSeconds = true), style = MaterialTheme.typography.bodyMedium)
            }
            Row(Modifier.heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(if (canCompare) Res.string.detail_compare else Res.string.detail_open_saved), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
