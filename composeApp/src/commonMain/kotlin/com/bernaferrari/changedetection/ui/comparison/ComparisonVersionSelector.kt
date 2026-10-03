/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.ui.comparison

import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.layout.heightIn

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bernaferrari.changedetection.repo.Snap
import com.bernaferrari.changedetection.shared.generated.resources.Res
import com.bernaferrari.changedetection.shared.generated.resources.a11y_close
import com.bernaferrari.changedetection.shared.generated.resources.comparison_section_snapshots
import com.bernaferrari.changedetection.shared.generated.resources.version_count
import com.bernaferrari.changedetection.shared.generated.resources.comparison_mode_before
import com.bernaferrari.changedetection.shared.generated.resources.comparison_mode_after
import com.bernaferrari.changedetection.shared.generated.resources.version_pair_latest
import com.bernaferrari.changedetection.shared.generated.resources.version_pair_previous
import com.bernaferrari.changedetection.ui.components.SegmentPosition
import com.bernaferrari.changedetection.ui.components.segmentShape
import com.bernaferrari.changedetection.util.formatTimestamp
import org.jetbrains.compose.resources.stringResource

/** One lazy history surface, anchored at the selected change. */
@Composable
internal fun VersionSelector(
    snaps: List<Snap>,
    selectedIndex: Int,
    onSelectPair: (Int) -> Unit,
    onClose: () -> Unit,
    footer: @Composable () -> Unit = {},
) {
    val pairCount = (snaps.size - 1).coerceAtLeast(0)
    val listState = androidx.compose.foundation.lazy.rememberLazyListState(
        initialFirstVisibleItemIndex = selectedIndex.coerceIn(0, (pairCount - 1).coerceAtLeast(0)),
    )
    Column(Modifier.fillMaxWidth().heightIn(max = 640.dp)) {
        Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(Res.string.comparison_section_snapshots), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(stringResource(Res.string.version_count, snaps.size), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onClose) {
                Icon(Icons.Rounded.Close, stringResource(Res.string.a11y_close))
            }
        }
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxWidth().weight(1f, fill = false).testTag("version-history").selectableGroup(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            items(count = pairCount, key = { snaps[it].snapId }) { index ->
                VersionTimelineEntry(
                    label = if (index == 0) stringResource(Res.string.version_pair_latest) else stringResource(Res.string.version_pair_previous, index),
                    beforeTime = formatTimestamp(snaps[index + 1].timestamp, includeSeconds = true),
                    afterTime = formatTimestamp(snaps[index].timestamp, includeSeconds = true),
                    selected = index == selectedIndex,
                    first = index == 0,
                    last = index == pairCount - 1,
                    onClick = { onSelectPair(index) },
                )
            }
            item(key = "page-facts") { Box(Modifier.padding(top = 16.dp)) { footer() } }
        }
    }
}

@Composable
private fun VersionTimelineEntry(
    label: String,
    beforeTime: String,
    afterTime: String,
    selected: Boolean,
    first: Boolean,
    last: Boolean,
    onClick: () -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(
            modifier = Modifier.width(24.dp).fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier.width(2.dp).height(18.dp)
                    .background(if (first) Color.Transparent else MaterialTheme.colorScheme.outlineVariant),
            )
            Surface(modifier = Modifier.size(18.dp), shape = CircleShape, color = MaterialTheme.colorScheme.surface) {
                Box(contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier.size(if (selected) 12.dp else 10.dp).background(
                            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            CircleShape,
                        ),
                    )
                }
            }
            Box(
                modifier = Modifier.width(2.dp).weight(1f)
                    .background(if (last) Color.Transparent else MaterialTheme.colorScheme.outlineVariant),
            )
        }
        Surface(
            onClick = onClick,
            modifier = Modifier.weight(1f).padding(bottom = if (last) 0.dp else 2.dp)
                .defaultMinSize(minHeight = 72.dp)
                .semantics {
                    this.selected = selected
                    role = Role.RadioButton
                },
            shape = segmentShape(
                when {
                    first && last -> SegmentPosition.Only
                    first -> SegmentPosition.First
                    last -> SegmentPosition.Last
                    else -> SegmentPosition.Middle
                },
            ),
            color = if (selected) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainerLow,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                    Text("${stringResource(Res.string.comparison_mode_before)} · $beforeTime", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${stringResource(Res.string.comparison_mode_after)} · $afterTime", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
