package com.bernaferrari.changedetection.ui.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bernaferrari.changedetection.R
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun QuickMonitorCard(
    isActive: Boolean,
    sitesCount: Int,
    totalSitesCount: Int,
    intervalSeconds: Int,
    onStart: (Int) -> Unit,
    onStop: () -> Unit,
    onExtend: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val hapticFeedback = LocalHapticFeedback.current
    val hasSites = totalSitesCount > 0
    val visuals = rememberQuickMonitorVisuals()

    var selectedInterval by rememberSaveable { mutableStateOf(intervalSeconds.coerceAtLeast(15)) }
    LaunchedEffect(intervalSeconds, isActive) {
        if (isActive) {
            selectedInterval = intervalSeconds.coerceAtLeast(15)
        }
    }

    val countdownEnabled = isActive && intervalSeconds > 0
    val activeInterval = intervalSeconds.coerceAtLeast(1)
    var remainingSeconds by remember(countdownEnabled, activeInterval) { mutableStateOf(activeInterval) }

    LaunchedEffect(countdownEnabled, activeInterval) {
        if (!countdownEnabled) {
            remainingSeconds = activeInterval
            return@LaunchedEffect
        }

        remainingSeconds = activeInterval
        while (currentCoroutineContext().isActive) {
            delay(1000)
            remainingSeconds = if (remainingSeconds > 0) remainingSeconds - 1 else activeInterval
        }
    }

    val targetProgress = if (countdownEnabled) 1f - (remainingSeconds.toFloat() / activeInterval) else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 450, easing = FastOutSlowInEasing),
        label = "quick_monitor_progress"
    )

    val statusTitle = if (isActive) stringResource(id = R.string.quick_monitor_active) else stringResource(id = R.string.quick_monitor_title)
    val statusSubtitle = if (isActive) {
        stringResource(id = R.string.quick_monitor_next_check_in, remainingSeconds.coerceAtLeast(0))
    } else {
        stringResource(id = R.string.quick_monitor_choose_interval)
    }

    val cardBorder = if (isActive) {
        BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
    } else {
        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.1f))
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        border = cardBorder,
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isActive) 10.dp else 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = if (isActive) Color.Transparent else MaterialTheme.colorScheme.surfaceContainerLow,
                tonalElevation = if (isActive) 6.dp else 0.dp,
                shadowElevation = if (isActive) 4.dp else 0.dp,
                border = if (isActive) BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)) else null
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .let { base ->
                            if (isActive) base.background(visuals.gradient) else base
                        }
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (isActive) visuals.onGradient.copy(alpha = 0.18f) else MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = null,
                                tint = if (isActive) visuals.onGradient else MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier
                                    .padding(12.dp)
                                    .size(20.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = statusTitle,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = if (isActive) visuals.onGradient else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = statusSubtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isActive) visuals.onGradient.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MetricChip(
                            icon = Icons.Default.Schedule,
                            label = stringResource(
                                id = R.string.quick_monitor_every_seconds,
                                if (isActive) intervalSeconds else selectedInterval
                            ),
                            containerColor = if (isActive) visuals.onGradient.copy(alpha = 0.18f) else MaterialTheme.colorScheme.primaryContainer,
                            contentColor = if (isActive) visuals.onGradient else MaterialTheme.colorScheme.onPrimaryContainer
                        )

                        MetricChip(
                            icon = Icons.Default.Language,
                            label = if (isActive) {
                                pluralStringResource(
                                    R.plurals.turbo_mode_selected_sites,
                                    sitesCount,
                                    sitesCount
                                )
                            } else {
                                pluralStringResource(R.plurals.sites_count, totalSitesCount, totalSitesCount)
                            },
                            containerColor = if (isActive) visuals.onGradient.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (isActive) visuals.onGradient else MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (isActive) {
                            MetricChip(
                                icon = Icons.Default.Notifications,
                                label = stringResource(id = R.string.quick_monitor_alert_state_on),
                                containerColor = visuals.onGradient.copy(alpha = 0.14f),
                                contentColor = visuals.onGradient
                            )
                        }
                    }
                }
            }

            if (isActive) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .semantics {
                                progressBarRangeInfo = ProgressBarRangeInfo(animatedProgress, 0f..1f)
                            },
                        color = if (isActive) visuals.onGradient else MaterialTheme.colorScheme.primary,
                        trackColor = (if (isActive) visuals.onGradient else MaterialTheme.colorScheme.primary).copy(alpha = 0.2f)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(id = R.string.quick_monitor_next_check_in, remainingSeconds.coerceAtLeast(0)),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isActive) visuals.onGradient.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${(animatedProgress * 100).roundToInt()}%",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = if (isActive) visuals.onGradient else MaterialTheme.colorScheme.primary
                        )
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    val intervalOptions = listOf(15, 30, 60, 120, 180)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        intervalOptions.forEach { option ->
                            IntervalOptionChip(
                                value = option,
                                selected = selectedInterval == option,
                                onSelect = {
                                    selectedInterval = option
                                }
                            )
                        }
                    }

                    Text(
                        text = if (hasSites) stringResource(id = R.string.quick_monitor_choose_interval) else stringResource(id = R.string.turbo_mode_no_sites),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (hasSites) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isActive) {
                    OutlinedButton(
                        onClick = {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                            onExtend()
                        },
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = stringResource(id = R.string.action_extend_one_hour))
                    }

                    FilledTonalButton(
                        onClick = {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                            onStop()
                        },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = stringResource(id = R.string.turbo_mode_button_stop))
                    }
                } else {
                    OutlinedButton(
                        onClick = {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                            onStart(selectedInterval)
                        },
                        enabled = hasSites,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.FormatListBulleted, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = stringResource(id = R.string.turbo_mode_choose_sites))
                    }

                    FilledTonalButton(
                        onClick = {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                            onStart(selectedInterval)
                        },
                        enabled = hasSites,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = stringResource(id = R.string.turbo_mode_button_start))
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    containerColor: Color,
    contentColor: Color
) {
    Surface(
        modifier = Modifier.semantics { contentDescription = label },
        shape = RoundedCornerShape(14.dp),
        color = containerColor,
        contentColor = contentColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
            Text(label, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun IntervalOptionChip(
    value: Int,
    selected: Boolean,
    onSelect: () -> Unit
) {
    AssistChip(
        onClick = onSelect,
        label = { Text(text = "${value}s") },
        leadingIcon = {
            if (selected) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
            }
        },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
            labelColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
            leadingIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    )
}
