/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.ui.comparison

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import com.bernaferrari.changedetection.repo.Snap
import com.bernaferrari.changedetection.shared.generated.resources.Res
import com.bernaferrari.changedetection.shared.generated.resources.version_count
import com.bernaferrari.changedetection.shared.generated.resources.version_latest
import com.bernaferrari.changedetection.shared.generated.resources.version_older
import com.bernaferrari.changedetection.shared.generated.resources.version_previous
import com.bernaferrari.changedetection.ui.theme.spacing
import com.bernaferrari.changedetection.util.formatTimestamp
import org.jetbrains.compose.resources.stringResource

/** Lets a person select adjacent snapshots while preserving context for the whole history. */
@Composable
internal fun VersionSelector(
    snaps: List<Snap>,
    selectedIndex: Int,
    onSelectPair: (Int) -> Unit,
) {
    val spacing = MaterialTheme.spacing
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(vertical = spacing.sm),
    ) {
        Text(
            text = stringResource(Res.string.version_count, snaps.size),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = spacing.md, vertical = spacing.xxs),
        )

        Row(
            modifier = Modifier
                .horizontalScroll(scrollState)
                .padding(horizontal = spacing.md),
            horizontalArrangement = Arrangement.spacedBy(spacing.xs),
        ) {
            for (index in 0 until snaps.size - 1) {
                val newerSnap = snaps[index]
                VersionChip(
                    olderLabel = if (index == snaps.size - 2) {
                        stringResource(Res.string.version_older)
                    } else {
                        stringResource(Res.string.version_previous)
                    },
                    newerLabel = if (index == 0) {
                        stringResource(Res.string.version_latest)
                    } else {
                        stringResource(Res.string.version_previous)
                    },
                    newerTime = formatTimestamp(newerSnap.timestamp),
                    isSelected = index == selectedIndex,
                    onClick = { onSelectPair(index) },
                )
            }
        }
    }
}

@Composable
private fun VersionChip(
    olderLabel: String,
    newerLabel: String,
    newerTime: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val spacing = MaterialTheme.spacing
    val containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh

    Surface(
        onClick = onClick,
        modifier = Modifier
            .heightIn(min = spacing.touchTarget)
            .semantics {
                selected = isSelected
                role = Role.RadioButton
            },
        shape = MaterialTheme.shapes.medium,
        color = containerColor,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = spacing.md, vertical = spacing.xs),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "$olderLabel → $newerLabel",
                style = MaterialTheme.typography.labelMedium,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
            Text(
                text = newerTime,
                style = MaterialTheme.typography.labelSmall,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
    }
}
