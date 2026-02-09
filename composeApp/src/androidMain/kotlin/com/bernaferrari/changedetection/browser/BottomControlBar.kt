package com.bernaferrari.changedetection.browser

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bernaferrari.changedetection.library.R

@Composable
fun BottomControlBar(
    uiState: InteractiveBrowserUiState,
    onElementSelectToggle: () -> Unit,
    onElementZapToggle: () -> Unit,
    onShowZapManager: () -> Unit
) {
    val monitoredCount = uiState.selectedElements.size
    val zappedCount = uiState.zappedElements.size
    val hasAnyRules = monitoredCount > 0 || zappedCount > 0
    val isPageReady = !uiState.isLoading && uiState.currentUrl.isNotBlank()
    val statusText = if (isPageReady) {
        stringResource(id = R.string.browser_ready_select_hint)
    } else {
        stringResource(id = R.string.browser_wait_for_load)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        shape = RoundedCornerShape(26.dp),
        tonalElevation = 8.dp,
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ModeButton(
                    icon = Icons.Default.TouchApp,
                    text = stringResource(id = R.string.browser_monitor_label),
                    description = stringResource(id = R.string.tip_element_selection),
                    count = monitoredCount,
                    isActive = uiState.interactionMode == InteractionMode.ELEMENT_SELECT,
                    activeColor = MaterialTheme.colorScheme.primary,
                    enabled = isPageReady,
                    onClick = onElementSelectToggle,
                    modifier = Modifier.weight(1f)
                )

                ModeButton(
                    icon = Icons.Default.Block,
                    text = stringResource(id = R.string.browser_zap_label),
                    description = stringResource(id = R.string.tip_element_zapping),
                    count = zappedCount,
                    isActive = uiState.interactionMode == InteractionMode.ELEMENT_ZAP,
                    activeColor = MaterialTheme.colorScheme.error,
                    enabled = isPageReady,
                    onClick = onElementZapToggle,
                    modifier = Modifier.weight(1f)
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                AnimatedVisibility(visible = hasAnyRules) {
                    Text(
                        text = stringResource(
                            id = R.string.browser_monitored_zapped_counts,
                            monitoredCount,
                            zappedCount
                        ),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(visible = hasAnyRules) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onShowZapManager) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ListAlt,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(id = R.string.browser_manage_elements),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ModeButton(
    icon: ImageVector,
    text: String,
    description: String,
    count: Int,
    isActive: Boolean,
    activeColor: Color,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val targetContainerColor = when {
        !enabled -> MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
        isActive -> activeColor
        else -> MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
    }
    val containerColor by animateColorAsState(targetContainerColor, label = "mode_container")

    val targetIconColor = when {
        !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        isActive -> MaterialTheme.colorScheme.onPrimary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val iconColor by animateColorAsState(targetIconColor, label = "mode_icon")

    val targetTextColor = when {
        !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        isActive -> MaterialTheme.colorScheme.onPrimary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val textColor by animateColorAsState(targetTextColor, label = "mode_text")

    val targetCountColor = when {
        !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        isActive -> MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val countColor by animateColorAsState(targetCountColor, label = "mode_count")

    Card(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(18.dp),
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isActive) 8.dp else 2.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = description,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium
                ),
                color = textColor
            )
            AnimatedVisibility(visible = count > 0) {
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = countColor
                )
            }
        }
    }
}
