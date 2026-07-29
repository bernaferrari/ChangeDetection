/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.ui.main.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bernaferrari.changedetection.shared.generated.resources.Res
import com.bernaferrari.changedetection.shared.generated.resources.label_interval
import com.bernaferrari.changedetection.shared.generated.resources.start
import com.bernaferrari.changedetection.shared.generated.resources.stop
import com.bernaferrari.changedetection.shared.generated.resources.turbo_active_sites
import com.bernaferrari.changedetection.shared.generated.resources.turbo_mode_selected_sites
import com.bernaferrari.changedetection.ui.theme.ExpressiveSpring
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

/** Turbo controls are rendered from state supplied by [TurboScreen], without owning that state. */
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
    val containerColor by animateColorAsState(
        targetValue = if (isRunning) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
        animationSpec = spring(
            dampingRatio = ExpressiveSpring.GENTLE_DAMPING,
            stiffness = ExpressiveSpring.GENTLE_STIFFNESS,
        ),
        label = "turboHeroContainer",
    )
    val currentLabel = intervalOptions.find { it.seconds == intervalSeconds }?.label ?: "15s"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(containerColor),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
            ) {
                when {
                    isSyncing -> CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 3.dp,
                    )
                    isRunning -> Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(36.dp),
                    )
                    else -> AnimatedContent(
                        targetState = currentLabel,
                        transitionSpec = {
                            slideInVertically { -it } + fadeIn() togetherWith
                                slideOutVertically { it } + fadeOut()
                        },
                        label = "turboInterval",
                    ) { label ->
                        Text(
                            text = label,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(spacing.lg))
        Text(
            text = if (isRunning) {
                pluralStringResource(
                    Res.plurals.turbo_active_sites,
                    selectedCount,
                    selectedCount,
                    currentLabel,
                )
            } else {
                pluralStringResource(
                    Res.plurals.turbo_mode_selected_sites,
                    selectedCount,
                    selectedCount,
                )
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(spacing.md))
        Text(
            text = stringResource(Res.string.label_interval),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(spacing.xs))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.xs),
        ) {
            intervalOptions.forEach { option ->
                val selected = intervalSeconds == option.seconds
                Surface(
                    onClick = { onIntervalChange(option.seconds) },
                    enabled = !isRunning,
                    shape = RoundedCornerShape(spacing.sm),
                    color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
                    contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                ) {
                    Box(
                        modifier = Modifier.padding(vertical = spacing.sm),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(option.label, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(spacing.md + spacing.xxs))
        TurboStartStopButton(
            isRunning = isRunning,
            enabled = canToggle,
            onClick = onToggle,
        )
    }
}

@Composable
private fun TurboStartStopButton(
    isRunning: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val spacing = MaterialTheme.spacing
    if (isRunning) {
        OutlinedButton(
            onClick = onClick,
            modifier = Modifier
                .width(160.dp)
                .height(spacing.touchTarget + spacing.xs),
            shape = RoundedCornerShape(28.dp),
            border = BorderStroke(2.dp, MaterialTheme.colorScheme.error),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Stop,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                )
                Spacer(modifier = Modifier.width(spacing.xs))
                Text(
                    text = stringResource(Res.string.stop),
                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    } else {
        FloatingActionButton(
            onClick = onClick,
            containerColor = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = if (enabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(160.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(horizontal = spacing.lg),
            ) {
                Icon(Icons.Default.Bolt, contentDescription = null)
                Spacer(modifier = Modifier.width(spacing.xs))
                Text(
                    text = stringResource(Res.string.start),
                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold),
                )
            }
        }
    }
}
