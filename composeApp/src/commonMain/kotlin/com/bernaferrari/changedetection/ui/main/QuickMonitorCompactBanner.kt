package com.bernaferrari.changedetection.ui.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bernaferrari.changedetection.shared.generated.resources.Res
import com.bernaferrari.changedetection.shared.generated.resources.quick_monitor_next_check_in
import com.bernaferrari.changedetection.shared.generated.resources.turbo_mode_button_stop
import com.bernaferrari.changedetection.shared.generated.resources.turbo_mode_selected_sites
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/**
 * Compact banner shown at the top when Quick Monitor is active.
 * Takes minimal space and shows only essential information.
 */
@Composable
fun QuickMonitorCompactBanner(
    isActive: Boolean,
    sitesCount: Int,
    intervalSeconds: Int,
    onStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isActive,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        val hapticFeedback = LocalHapticFeedback.current

        var remainingSeconds by remember(intervalSeconds) {
            mutableIntStateOf(intervalSeconds.coerceAtLeast(1))
        }

        LaunchedEffect(intervalSeconds) {
            remainingSeconds = intervalSeconds.coerceAtLeast(1)
            while (currentCoroutineContext().isActive) {
                delay(1000)
                remainingSeconds = if (remainingSeconds > 0) {
                    remainingSeconds - 1
                } else {
                    intervalSeconds.coerceAtLeast(1)
                }
            }
        }

        val progress = if (intervalSeconds > 0) {
            1f - (remainingSeconds.toFloat() / intervalSeconds.coerceAtLeast(1))
        } else {
            0f
        }

        val animatedProgress by animateFloatAsState(
            targetValue = progress.coerceIn(0f, 1f),
            animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
            label = "quick_monitor_compact_progress"
        )

        Surface(
            modifier = modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primaryContainer,
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )

                    Text(
                        text = buildString {
                            append(
                                pluralStringResource(
                                    Res.plurals.turbo_mode_selected_sites,
                                    sitesCount,
                                    sitesCount
                                )
                            )
                            append(" • ")
                            append(
                                stringResource(
                                    Res.string.quick_monitor_next_check_in,
                                    remainingSeconds.coerceAtLeast(0)
                                )
                            )
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                FilledTonalIconButton(
                    onClick = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        onStop()
                    },
                    modifier = Modifier.size(32.dp),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(Res.string.turbo_mode_button_stop),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 44.dp)
                    .clip(RoundedCornerShape(bottomStart = 0.dp, bottomEnd = 0.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
            )
        }
    }
}
