package com.bernaferrari.changedetection.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bernaferrari.changedetection.R
import com.bernaferrari.changedetection.ui.design.CanvasHero
import com.bernaferrari.changedetection.ui.design.CanvasSectionHeader
import com.bernaferrari.changedetection.ui.design.InsightCard
import kotlin.math.max

/**
 * Bottom sheet for Quick Monitor controls.
 * Shows full interval selection and site selection options.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun QuickMonitorBottomSheet(
    isActive: Boolean,
    sitesCount: Int,
    totalSitesCount: Int,
    intervalSeconds: Int,
    onStart: (Int) -> Unit,
    onStop: () -> Unit,
    onExtend: () -> Unit,
    onDismiss: () -> Unit,
    sheetState: SheetState,
    modifier: Modifier = Modifier
) {
    val hapticFeedback = LocalHapticFeedback.current
    val hasSites = totalSitesCount > 0

    var selectedInterval by rememberSaveable { mutableIntStateOf(intervalSeconds.coerceAtLeast(15)) }
    LaunchedEffect(intervalSeconds) {
        selectedInterval = intervalSeconds.coerceAtLeast(15)
    }

    val heroBadge = if (isActive) {
        stringResource(R.string.quick_monitor_sheet_badge_live)
    } else {
        stringResource(R.string.quick_monitor_sheet_badge_idle)
    }
    val heroTitle = if (isActive) {
        stringResource(R.string.home_quick_monitor_active)
    } else {
        stringResource(R.string.home_quick_monitor_idle)
    }
    val heroSubtitle = if (isActive) {
        stringResource(
            R.string.home_quick_monitor_description_active,
            intervalSeconds,
            max(sitesCount, 1)
        )
    } else {
        stringResource(R.string.home_quick_monitor_description_idle)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            CanvasHero(
                badge = heroBadge,
                title = heroTitle,
                subtitle = heroSubtitle,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                InsightCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Language,
                    label = stringResource(id = R.string.label_sites),
                    value = if (isActive) sitesCount.toString() else totalSitesCount.toString(),
                    supportingText = if (isActive) {
                        stringResource(id = R.string.quick_monitor_sites_selected_label)
                    } else {
                        stringResource(id = R.string.home_stats_sites_supporting)
                    }
                )
                InsightCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Schedule,
                    label = stringResource(id = R.string.label_interval),
                    value = stringResource(
                        id = R.string.quick_monitor_interval_value,
                        if (isActive) intervalSeconds else selectedInterval
                    ),
                    supportingText = stringResource(id = R.string.quick_monitor_banner_service_running)
                )
            }

            if (!isActive) {
                IntervalPicker(
                    selectedInterval = selectedInterval,
                    onIntervalSelected = { selectedInterval = it }
                )
            } else {
                ActiveSessionSummary(
                    intervalSeconds = intervalSeconds,
                    sitesCount = sitesCount,
                    onExtend = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        onExtend()
                    }
                )
            }

            Text(
                text = stringResource(id = R.string.quick_monitor_sheet_limit_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (!hasSites) {
                MissingSitesCallout()
            }

            if (isActive) {
                Button(
                    onClick = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        onStop()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = sitesCount > 0
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = stringResource(id = R.string.home_quick_monitor_stop))
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onStart(selectedInterval)
                        },
                        enabled = hasSites,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.FormatListBulleted,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = stringResource(R.string.home_quick_monitor_manage))
                    }

                    Button(
                        onClick = {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onStart(selectedInterval)
                        },
                        enabled = hasSites,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = stringResource(R.string.home_quick_monitor_start))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IntervalPicker(
    selectedInterval: Int,
    onIntervalSelected: (Int) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CanvasSectionHeader(
            title = stringResource(id = R.string.quick_monitor_sheet_interval_title)
        )
        Text(
            text = stringResource(id = R.string.quick_monitor_choose_interval),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(15, 30, 60, 120, 180).forEach { interval ->
                FilterChip(
                    selected = selectedInterval == interval,
                    onClick = { onIntervalSelected(interval) },
                    label = {
                        Text(
                            text = stringResource(
                                R.string.quick_monitor_interval_value,
                                interval
                            )
                        )
                    },
                    leadingIcon = if (selectedInterval == interval) {
                        {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    } else null,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        selectedLeadingIconColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                )
            }
        }
    }
}

@Composable
private fun ActiveSessionSummary(
    intervalSeconds: Int,
    sitesCount: Int,
    onExtend: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CanvasSectionHeader(title = stringResource(id = R.string.quick_monitor_sheet_active_section_title))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Bolt,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        shape = CircleShape
                    )
                    .padding(8.dp)
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                val selectedLabel = stringResource(id = R.string.quick_monitor_sites_selected_label)
                Text(
                    text = selectedLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = pluralStringResource(
                        id = R.plurals.turbo_mode_selected_sites,
                        count = sitesCount,
                        sitesCount
                    ),
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                )
                Text(
                    text = stringResource(
                        id = R.string.quick_monitor_every_seconds,
                        intervalSeconds
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            OutlinedButton(onClick = onExtend) {
                Text(text = stringResource(id = R.string.quick_monitor_extend_1h))
            }
        }
    }
}

@Composable
private fun MissingSitesCallout() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f),
                shape = CircleShape
            )
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Text(
            text = stringResource(id = R.string.quick_monitor_sheet_empty_title),
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center
        )
        Text(
            text = stringResource(id = R.string.quick_monitor_sheet_empty_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onErrorContainer,
            textAlign = TextAlign.Center
        )
    }
}
