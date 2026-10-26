@file:OptIn(ExperimentalMaterial3Api::class)

package com.bernaferrari.changedetection.ui.comparison

import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.material3.SheetValue
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.rounded.Article
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Difference
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.UnfoldMore
import androidx.compose.material.icons.rounded.Update
import androidx.compose.material.icons.rounded.ViewAgenda
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bernaferrari.changedetection.image.VisualRegion
import com.bernaferrari.changedetection.localization.stringResource
import com.bernaferrari.changedetection.preview.ChangePreview
import com.bernaferrari.changedetection.repo.Snap
import com.bernaferrari.changedetection.repo.source.SnapsRepository
import com.bernaferrari.changedetection.shared.generated.resources.*
import com.bernaferrari.changedetection.ui.components.SharedEmptyState
import com.bernaferrari.changedetection.ui.components.checkFailureTitle
import com.bernaferrari.changedetection.ui.kit.Callout
import com.bernaferrari.changedetection.ui.kit.CaptureTop
import androidx.compose.ui.graphics.Color
import com.bernaferrari.changedetection.ui.kit.ConnectedChoice
import com.bernaferrari.changedetection.ui.kit.GroupRow
import com.bernaferrari.changedetection.ui.kit.friendlyDateTime
import com.bernaferrari.changedetection.ui.kit.friendlyDay
import com.bernaferrari.changedetection.ui.kit.localDate
import com.bernaferrari.changedetection.ui.kit.name
import com.bernaferrari.changedetection.ui.kit.optional
import com.bernaferrari.changedetection.ui.kit.placeholderPulse
import com.bernaferrari.changedetection.ui.kit.previewLabel
import com.bernaferrari.changedetection.ui.kit.rememberPairPreview
import com.bernaferrari.changedetection.ui.theme.marks
import com.bernaferrari.changedetection.util.displayPageAddress
import com.bernaferrari.changedetection.util.formatTime
import com.bernaferrari.changedetection.util.formatTimestamp
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.koin.compose.viewmodel.koinViewModel

/**
 * Every saved version, newest first and grouped by day. Each row shows what changed in that version, so
 * "the one where the price dropped" is easy to spot; the pair on screen is marked.
 */
@Composable
internal fun VersionSheet(snaps: List<Snap>, selectedIndex: Int, selectedSingleSnapId: String?, onDismiss: () -> Unit, onSelect: (Int) -> Unit) {
    val list = androidx.compose.foundation.lazy.rememberLazyListState()
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberBottomSheetState(initialValue = SheetValue.Hidden, enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)), containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
        LazyColumn(Modifier.fillMaxWidth().testTag("version-list"), state = list, contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp, top = 4.dp)) {
            item(key = "title") {
                Column(Modifier.padding(start = 8.dp, end = 8.dp, bottom = 8.dp)) {
                    Text(stringResource(Res.string.reader_versions_title), style = MaterialTheme.typography.headlineSmall)
                    Text(stringResource(Res.string.reader_versions_body), Modifier.padding(top = 4.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            snaps.indices.groupBy { snaps[it].timestamp.localDate() }.forEach { (_, indices) ->
                item(key = "day-" + snaps[indices.first()].snapId) {
                    Text(
                        friendlyDay(snaps[indices.first()].timestamp),
                        Modifier.padding(start = 8.dp, top = 16.dp, bottom = 8.dp),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                indices.forEachIndexed { position, index ->
                    val snap = snaps[index]
                    item(key = snap.snapId) {
                        val previous = snaps.getOrNull(index + 1)
                        val isAfter = index == selectedIndex || snap.snapId == selectedSingleSnapId
                        val isBefore = selectedIndex >= 0 && index == selectedIndex + 1
                        val shape = groupShape(position, indices.size)
                        Surface(
                            onClick = { onSelect(index) },
                            shape = shape,
                            color = if (isAfter) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceBright,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp),
                        ) {
                            // One preview per row feeds both the thumbnail and the summary.
                            val preview = previous?.let { rememberPairPreview(it.snapId, snap.snapId) }
                            Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp).heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                if (snap.isImageSnapshot()) ChangeThumb(snap, previous != null, preview, Modifier.size(width = 64.dp, height = 48.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(formatTime(snap.timestamp), style = MaterialTheme.typography.titleMedium, color = if (isAfter) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface)
                                    Text(
                                        if (previous == null) stringResource(Res.string.label_first_version) else previewLabel(preview),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (isAfter) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                when {
                                    snap.snapId == selectedSingleSnapId -> SelectionTag(stringResource(Res.string.comparison_selected))
                                    isAfter -> SelectionTag(stringResource(Res.string.preview_after))
                                    isBefore -> SelectionTag(stringResource(Res.string.preview_before), quiet = true)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    // Open with the pair on screen in view.
    LaunchedEffect(Unit) { if (selectedIndex > 2) list.scrollToItem(selectedIndex) }
}

/** Rounded at the ends of a day's group, like the app's other grouped lists. */
@Composable
private fun groupShape(position: Int, count: Int): androidx.compose.ui.graphics.Shape {
    val large = 20.dp
    val small = 4.dp
    return androidx.compose.foundation.shape.RoundedCornerShape(
        topStart = if (position == 0) large else small,
        topEnd = if (position == 0) large else small,
        bottomStart = if (position == count - 1) large else small,
        bottomEnd = if (position == count - 1) large else small,
    )
}

/** The same tags the change previews wear: After in the highlighter, Before quiet. */
@Composable
private fun SelectionTag(text: String, quiet: Boolean = false) = com.bernaferrari.changedetection.ui.kit.ChangeTag(text, highlighted = !quiet)

/**
 * What changed in [snap], as a thumbnail: the changed area of a screenshot (the top of the page when no area is
 * known). Text pages have no thumbnail; their row's summary says what changed.
 */
@Composable
private fun ChangeThumb(snap: Snap, hasPrevious: Boolean, preview: ChangePreview?, modifier: Modifier = Modifier) {
    val shape = MaterialTheme.shapes.small
    Box(modifier.clip(shape).background(Color.White).border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)) {
        val lead = (preview as? ChangePreview.Crop)?.lead
        when {
            // Hold the thumbnail's place until the change is known, instead of showing the page top and then swapping it.
            hasPrevious && preview == null -> Box(Modifier.matchParentSize().placeholderPulse())

            lead != null && preview is ChangePreview.Crop -> {
                val removed = lead.kind == VisualRegion.Kind.Removed
                SnapCrop(
                    snapId = if (removed) preview.beforeSnapId else preview.afterSnapId,
                    top = if (removed) lead.beforeTop else lead.afterTop,
                    bottom = if (removed) lead.beforeBottom else lead.afterBottom,
                    modifier = Modifier.matchParentSize(),
                )
            }

            else -> CaptureTop(snap.snapId, Modifier.matchParentSize(), decodeWidth = 240)
        }
    }
}

/**
 * The page at full width around a changed band, in the thumbnail's own proportions. A band alone is far wider than
 * the thumbnail, and cropping it to fit showed a giant slice of one glyph. Only that window is decoded (and cached).
 */
@Composable
private fun SnapCrop(snapId: String, top: Float, bottom: Float, modifier: Modifier = Modifier, thumbAspect: Float = 64f / 48f) {
    val crop by produceState<androidx.compose.ui.graphics.ImageBitmap?>(null, snapId, top, bottom) {
        val bytes = runCatching { optional<SnapsRepository>()?.getSnapContent(snapId) }.getOrNull() ?: return@produceState
        val pageAspect = imageAspect(bytes)
        // Height of a full-width window with the thumbnail's proportions, as a share of the page.
        val window = if (pageAspect > 0f) (pageAspect / thumbAspect).coerceAtMost(1f) else (bottom - top)
        val center = (top + bottom) / 2f
        // A change taller than the window shows its top, where it starts.
        val start = if (bottom - top > window) top else (center - window / 2f)
        val windowTop = start.coerceIn(0f, (1f - window).coerceAtLeast(0f))
        value = com.bernaferrari.changedetection.image.CropCache.get(bytes, 0f, windowTop, 1f, (windowTop + window).coerceAtMost(1f), 256)
    }
    crop?.let { androidx.compose.foundation.Image(it, null, modifier, contentScale = androidx.compose.ui.layout.ContentScale.Crop) }
}
