/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.ui.comparison

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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.bernaferrari.changedetection.shared.generated.resources.comparison_choose_older
import com.bernaferrari.changedetection.shared.generated.resources.comparison_section_snapshots
import com.bernaferrari.changedetection.shared.generated.resources.version_count
import com.bernaferrari.changedetection.shared.generated.resources.version_pair_latest
import com.bernaferrari.changedetection.shared.generated.resources.version_pair_previous
import com.bernaferrari.changedetection.ui.theme.spacing
import com.bernaferrari.changedetection.ui.components.SegmentPosition
import com.bernaferrari.changedetection.ui.components.segmentShape
import com.bernaferrari.changedetection.util.formatTimestamp
import org.jetbrains.compose.resources.stringResource

/** A compact version timeline modeled after SDK Monitor's segmented history. */
@Composable
internal fun VersionSelector(
    snaps: List<Snap>,
    selectedIndex: Int,
    onSelectPair: (Int) -> Unit,
) {
    val spacing = MaterialTheme.spacing
    val pairCount = (snaps.size - 1).coerceAtLeast(0)
    val visibleIndices = remember(pairCount, selectedIndex) {
        if (pairCount <= 3) {
            (0 until pairCount).toList()
        } else {
            buildList {
                add(0)
                add(1)
                add(if (selectedIndex >= 2) selectedIndex else 2)
            }.distinct().sorted()
        }
    }
    var olderMenuOpen by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth().padding(top = spacing.xs)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(Res.string.comparison_section_snapshots),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = stringResource(Res.string.version_count, snaps.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (pairCount > 3) {
                Box {
                    TextButton(onClick = { olderMenuOpen = true }) {
                        Text(stringResource(Res.string.comparison_choose_older))
                    }
                    DropdownMenu(expanded = olderMenuOpen, onDismissRequest = { olderMenuOpen = false }) {
                        (2 until pairCount).forEach { index ->
                            DropdownMenuItem(
                                text = { Text(stringResource(Res.string.version_pair_previous, index)) },
                                onClick = {
                                    onSelectPair(index)
                                    olderMenuOpen = false
                                },
                            )
                        }
                    }
                }
            }
        }

        Column(modifier = Modifier.padding(top = spacing.xs)) {
            visibleIndices.forEachIndexed { visibleIndex, pairIndex ->
                VersionTimelineEntry(
                    label = if (pairIndex == 0) {
                        stringResource(Res.string.version_pair_latest)
                    } else {
                        stringResource(Res.string.version_pair_previous, pairIndex)
                    },
                    timeRange = "${formatTimestamp(snaps[pairIndex + 1].timestamp)} → ${formatTimestamp(snaps[pairIndex].timestamp)}",
                    selected = pairIndex == selectedIndex,
                    first = visibleIndex == 0,
                    last = visibleIndex == visibleIndices.lastIndex,
                    onClick = { onSelectPair(pairIndex) },
                )
            }
        }
    }
}

@Composable
private fun VersionTimelineEntry(
    label: String,
    timeRange: String,
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
                    Text(timeRange, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
