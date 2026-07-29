/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.ui.main.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bernaferrari.changedetection.shared.generated.resources.Res
import com.bernaferrari.changedetection.shared.generated.resources.label_interval
import com.bernaferrari.changedetection.shared.generated.resources.start
import com.bernaferrari.changedetection.shared.generated.resources.stop
import com.bernaferrari.changedetection.shared.generated.resources.turbo_active_sites
import com.bernaferrari.changedetection.shared.generated.resources.turbo_mode_selected_sites
import com.bernaferrari.changedetection.ui.theme.spacing
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

private data class IntervalOption(val seconds: Int, val label: String)

private val intervalOptions = listOf(
    IntervalOption(15, "15s"),
    IntervalOption(30, "30s"),
    IntervalOption(60, "1m"),
    IntervalOption(120, "2m"),
)

/** Turbo's control surface: one clear status, one interval choice, and one primary action. */
@Composable
internal fun TurboHero(
    isRunning: Boolean,
    isSyncing: Boolean,
    intervalSeconds: Int,
    selectedCount: Int,
    onIntervalChange: (Int) -> Unit,
    onToggle: () -> Unit,
    canToggle: Boolean,
) {
    val spacing = MaterialTheme.spacing
    val intervalLabel = intervalOptions.firstOrNull { it.seconds == intervalSeconds }?.label ?: intervalOptions.first().label
    val statusLabel = if (isRunning) {
        pluralStringResource(Res.plurals.turbo_active_sites, selectedCount, selectedCount, intervalLabel)
    } else {
        pluralStringResource(Res.plurals.turbo_mode_selected_sites, selectedCount, selectedCount)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.lg, vertical = spacing.md),
        shape = MaterialTheme.shapes.extraLarge,
        color = if (isRunning) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
    ) {
        Column(
            modifier = Modifier.padding(spacing.lg),
            verticalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(44.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    if (isSyncing) {
                        CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = if (isRunning) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.primary
                            },
                            modifier = Modifier.size(28.dp),
                        )
                    }
                }
                Spacer(modifier = Modifier.width(spacing.sm))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isRunning) stringResource(Res.string.stop) else stringResource(Res.string.start),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = statusLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (!isRunning) {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                    Text(
                        text = stringResource(Res.string.label_interval),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
                        intervalOptions.forEach { option ->
                            val selected = option.seconds == intervalSeconds
                            if (selected) {
                                Button(
                                    onClick = { onIntervalChange(option.seconds) },
                                    contentPadding = ButtonDefaults.ContentPadding,
                                ) {
                                    Text(option.label)
                                }
                            } else {
                                OutlinedButton(
                                    onClick = { onIntervalChange(option.seconds) },
                                    contentPadding = ButtonDefaults.ContentPadding,
                                ) {
                                    Text(option.label)
                                }
                            }
                        }
                    }
                }
            }

            FilledTonalButton(
                onClick = onToggle,
                enabled = canToggle,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (isRunning) {
                    Icon(
                        imageVector = Icons.Rounded.Stop,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(spacing.xs))
                }
                Text(stringResource(if (isRunning) Res.string.stop else Res.string.start))
            }
        }
    }
}
