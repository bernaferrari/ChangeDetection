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
import com.bernaferrari.changedetection.ui.comparison.isImageSnapshot
import com.bernaferrari.changedetection.ui.comparison.isTextSnapshot
import com.bernaferrari.changedetection.ui.comparison.isHtmlSnapshot
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
        snap.isImageSnapshot() -> SavedPagePreview.Image(repository.getSnapContent(snap.snapId))

        snap.isTextSnapshot() -> {
            val bytes = repository.getSnapContent(snap.snapId)
            withContext(Dispatchers.Default) {
                SavedPagePreview.Text(readableSnapshotText(decodeByteArray(bytes, snap.contentCharset), snap.isHtmlSnapshot()).take(1200))
            }
        }

        else -> SavedPagePreview.Document
    }
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (_: Exception) {
    SavedPagePreview.Unavailable
}
