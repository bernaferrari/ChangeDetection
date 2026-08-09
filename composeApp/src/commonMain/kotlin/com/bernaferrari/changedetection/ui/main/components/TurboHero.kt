/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.bernaferrari.changedetection.ui.main.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bernaferrari.changedetection.shared.generated.resources.Res
import com.bernaferrari.changedetection.shared.generated.resources.start
import com.bernaferrari.changedetection.shared.generated.resources.stop
import com.bernaferrari.changedetection.shared.generated.resources.turbo_cadence
import com.bernaferrari.changedetection.shared.generated.resources.turbo_cadence_value
import com.bernaferrari.changedetection.shared.generated.resources.turbo_live_label
import com.bernaferrari.changedetection.shared.generated.resources.turbo_mode_selected_sites
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

private data class IntervalOption(
    val seconds: Int,
    val shortLabel: String,
    val longLabel: String,
)

private val intervalOptions = listOf(
    IntervalOption(15, "15s", "15 seconds"),
    IntervalOption(30, "30s", "30 seconds"),
    IntervalOption(60, "1m", "1 minute"),
    IntervalOption(120, "2m", "2 minutes"),
)

/** Turbo's single hero moment: cadence, scope and the action that starts the session. */
@Composable
internal fun TurboHero(
    isRunning: Boolean,
    isSyncing: Boolean,
    intervalSeconds: Int,
    selectedCount: Int,
    onIntervalChange: (Int) -> Unit,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interval = intervalOptions.firstOrNull { it.seconds == intervalSeconds } ?: intervalOptions.first()
    val containerColor by animateColorAsState(
        targetValue = if (isRunning) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
        animationSpec = MaterialTheme.motionScheme.fastEffectsSpec(),
        label = "turboHeroColor",
    )

    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraLarge,
        color = containerColor,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(if (isRunning) Res.string.turbo_live_label else Res.string.turbo_cadence),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (isSyncing) {
                    LoadingIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Text(
                text = stringResource(Res.string.turbo_cadence_value, interval.longLabel),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = if (isRunning) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = pluralStringResource(
                    Res.plurals.turbo_mode_selected_sites,
                    selectedCount,
                    selectedCount,
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
            ) {
                intervalOptions.forEachIndexed { index, option ->
                    val selected = option.seconds == intervalSeconds
                    ToggleButton(
                        checked = selected,
                        onCheckedChange = { if (it) onIntervalChange(option.seconds) },
                        enabled = !isRunning,
                        modifier = Modifier.weight(1f).heightIn(min = 44.dp),
                        shapes = when (index) {
                            0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                            intervalOptions.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                            else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                        },
                        colors = ToggleButtonDefaults.toggleButtonColors(
                            checkedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            checkedContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    ) {
                        Text(option.shortLabel, maxLines = 1)
                    }
                }
            }

            Button(
                onClick = onAction,
                enabled = isRunning || selectedCount > 0,
                modifier = Modifier.fillMaxWidth(),
                shapes = ButtonDefaults.shapes(),
                colors = if (isRunning) {
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    )
                } else {
                    ButtonDefaults.buttonColors()
                },
            ) {
                Crossfade(
                    targetState = isRunning,
                    animationSpec = MaterialTheme.motionScheme.fastEffectsSpec(),
                    label = "turboAction",
                ) { running ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (running) Icons.Rounded.Stop else Icons.Rounded.Bolt,
                            contentDescription = null,
                        )
                        Spacer(Modifier.size(8.dp))
                        Text(stringResource(if (running) Res.string.stop else Res.string.start))
                    }
                }
            }
        }
    }
}
