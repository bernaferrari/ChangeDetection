package com.bernaferrari.changedetection.ui.main

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.Role
import com.bernaferrari.changedetection.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlin.math.roundToInt

@Composable
fun QuickMonitorBanner(
    isActive: Boolean,
    sitesCount: Int,
    intervalSeconds: Int,
    onToggle: () -> Unit,
    startTime: Long? = null,
    deadlineTime: Long? = null,
    modifier: Modifier = Modifier
) {
    if (!isActive) return

    val hapticFeedback = LocalHapticFeedback.current

    var elapsedLabel by remember { mutableStateOf("--:--") }
    var remainingLabel by remember { mutableStateOf("--:--") }
    var nextCheck by remember { mutableStateOf(intervalSeconds.coerceAtLeast(0)) }

    LaunchedEffect(startTime, deadlineTime) {
        while (currentCoroutineContext().isActive) {
            startTime?.let { startedAt ->
                val elapsed = (System.currentTimeMillis() - startedAt).coerceAtLeast(0L)
                val minutes = (elapsed / 60000).toInt()
                val seconds = ((elapsed % 60000) / 1000).toInt()
                elapsedLabel = String.format("%02d:%02d", minutes, seconds)
            }

            deadlineTime?.takeIf { it > 0 }?.let { deadline ->
                val remaining = (deadline - System.currentTimeMillis()).coerceAtLeast(0L)
                val minutes = (remaining / 60000).toInt()
                val seconds = ((remaining % 60000) / 1000).toInt()
                remainingLabel = String.format("%02d:%02d", minutes, seconds)
            }

            delay(1000)
        }
    }

    LaunchedEffect(intervalSeconds) {
        nextCheck = intervalSeconds.coerceAtLeast(0)
        while (currentCoroutineContext().isActive) {
            delay(1000)
            nextCheck = if (nextCheck > 0) nextCheck - 1 else intervalSeconds.coerceAtLeast(0)
        }
    }

    val progressTarget = if (intervalSeconds > 0) 1f - (nextCheck.toFloat() / intervalSeconds) else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = progressTarget.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 450, easing = FastOutSlowInEasing),
        label = "quick_monitor_banner_progress"
    )

    val colorScheme = MaterialTheme.colorScheme
    val visuals = rememberQuickMonitorVisuals()
    val onGradient = visuals.onGradient
    val summary = stringResource(
        id = R.string.quick_monitor_banner_active_details,
        sitesCount,
        intervalSeconds,
        elapsedLabel,
        remainingLabel
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(28.dp),
        tonalElevation = 10.dp,
        shadowElevation = 18.dp,
        border = BorderStroke(1.dp, colorScheme.primary.copy(alpha = 0.25f)),
        color = Color.Transparent
    ) {
        Column(
            modifier = Modifier
                .background(visuals.gradient)
                .padding(horizontal = 24.dp, vertical = 22.dp)
                .semantics {
                    heading()
                    contentDescription = summary
                },
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = onGradient.copy(alpha = 0.16f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = onGradient,
                            modifier = Modifier
                                .padding(12.dp)
                                .size(22.dp)
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = stringResource(id = R.string.quick_monitor_banner_active_title),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = onGradient
                            )
                            AssistChip(
                                onClick = {},
                                enabled = false,
                                label = { Text(text = stringResource(id = R.string.quick_monitor_banner_live_badge)) },
                                leadingIcon = {
                                    Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(16.dp))
                                },
                                colors = AssistChipDefaults.assistChipColors(
                                    disabledContainerColor = onGradient.copy(alpha = 0.18f),
                                    disabledLabelColor = onGradient,
                                    disabledLeadingIconContentColor = onGradient
                                )
                            )
                        }
                        Text(
                            text = summary,
                            style = MaterialTheme.typography.bodySmall,
                            color = onGradient.copy(alpha = 0.9f)
                        )
                    }
                }

                FilledTonalButton(
                    onClick = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        onToggle()
                    },
                    modifier = Modifier.semantics { role = Role.Button },
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = onGradient.copy(alpha = 0.22f),
                        contentColor = onGradient
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(text = stringResource(id = R.string.action_stop_monitoring))
                }
            }

            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .semantics {
                        progressBarRangeInfo = ProgressBarRangeInfo(animatedProgress, 0f..1f)
                    },
                color = onGradient,
                trackColor = onGradient.copy(alpha = 0.25f)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BannerMetric(
                    label = stringResource(id = R.string.label_sites),
                    value = sitesCount.toString(),
                    labelColor = onGradient.copy(alpha = 0.7f),
                    valueColor = onGradient
                )
                BannerMetric(
                    label = stringResource(id = R.string.label_interval),
                    value = stringResource(id = R.string.quick_monitor_every_seconds, intervalSeconds),
                    labelColor = onGradient.copy(alpha = 0.7f),
                    valueColor = onGradient
                )
                BannerMetric(
                    label = stringResource(id = R.string.quick_monitor_next_check_in, nextCheck.coerceAtLeast(0)),
                    value = remainingLabel,
                    labelColor = onGradient.copy(alpha = 0.7f),
                    valueColor = onGradient
                )
            }
        }
    }
}

@Composable
private fun BannerMetric(
    label: String,
    value: String,
    labelColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = labelColor)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = valueColor)
    }
}
