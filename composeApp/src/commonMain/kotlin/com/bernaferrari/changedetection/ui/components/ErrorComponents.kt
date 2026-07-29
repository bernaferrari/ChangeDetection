/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bernaferrari.changedetection.error.ErrorEvent
import com.bernaferrari.changedetection.error.ErrorSeverity
import com.bernaferrari.changedetection.error.ErrorType
import com.bernaferrari.changedetection.shared.generated.resources.Res
import com.bernaferrari.changedetection.shared.generated.resources.a11y_close
import com.bernaferrari.changedetection.shared.generated.resources.retry
import org.jetbrains.compose.resources.stringResource

/**
 * Snackbar-style error banner that appears at the bottom of the screen.
 */
@Composable
fun ErrorBanner(
    error: ErrorEvent?,
    onDismiss: () -> Unit,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    error?.let { event ->
        ErrorBannerContent(
            event = event,
            onDismiss = onDismiss,
            onRetry = if (event.isRetryable && onRetry != null) onRetry else null,
            modifier = modifier,
        )
    }
}

@Composable
private fun ErrorBannerContent(
    event: ErrorEvent,
    onDismiss: () -> Unit,
    onRetry: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val colors = getErrorColors(event.severity)
    val icon = getErrorIcon(event.type)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = MaterialTheme.shapes.large,
        color = colors.containerColor,
        tonalElevation = 3.dp,
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(colors.iconBackgroundColor),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = colors.iconColor,
                    modifier = Modifier.size(20.dp),
                )
            }

            // Message
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.message,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = colors.textColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (event.description != null) {
                    Text(
                        text = event.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textColor.copy(alpha = 0.7f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            // Actions
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (onRetry != null) {
                    TextButton(
                        onClick = {
                            onDismiss()
                            onRetry()
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = colors.actionColor,
                        ),
                    ) {
                        Text(stringResource(Res.string.retry))
                    }
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(Res.string.a11y_close),
                        tint = colors.textColor.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun getErrorColors(severity: ErrorSeverity): ErrorColors = when (severity) {
    ErrorSeverity.INFO -> ErrorColors(
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        iconBackgroundColor = MaterialTheme.colorScheme.primary,
        iconColor = MaterialTheme.colorScheme.onPrimary,
        textColor = MaterialTheme.colorScheme.onPrimaryContainer,
        actionColor = MaterialTheme.colorScheme.primary,
    )

    ErrorSeverity.WARNING -> ErrorColors(
        containerColor = MaterialTheme.colorScheme.secondaryContainer,
        iconBackgroundColor = MaterialTheme.colorScheme.secondary,
        iconColor = MaterialTheme.colorScheme.onSecondary,
        textColor = MaterialTheme.colorScheme.onSecondaryContainer,
        actionColor = MaterialTheme.colorScheme.secondary,
    )

    ErrorSeverity.ERROR -> ErrorColors(
        containerColor = MaterialTheme.colorScheme.errorContainer,
        iconBackgroundColor = MaterialTheme.colorScheme.error,
        iconColor = MaterialTheme.colorScheme.onError,
        textColor = MaterialTheme.colorScheme.onErrorContainer,
        actionColor = MaterialTheme.colorScheme.error,
    )

    ErrorSeverity.CRITICAL -> ErrorColors(
        containerColor = MaterialTheme.colorScheme.errorContainer,
        iconBackgroundColor = MaterialTheme.colorScheme.error,
        iconColor = MaterialTheme.colorScheme.onError,
        textColor = MaterialTheme.colorScheme.onErrorContainer,
        actionColor = MaterialTheme.colorScheme.error,
    )
}

private fun getErrorIcon(type: ErrorType): ImageVector = when (type) {
    ErrorType.NETWORK -> Icons.Default.CloudOff
    ErrorType.CONNECTIVITY -> Icons.Default.WifiOff
    ErrorType.TIMEOUT -> Icons.Default.HourglassEmpty
    ErrorType.VALIDATION -> Icons.Default.Edit
    ErrorType.AUTHENTICATION -> Icons.Default.Lock
    ErrorType.DATABASE -> Icons.Default.Storage
    ErrorType.CONTENT -> Icons.Default.Description
    ErrorType.RATE_LIMITED -> Icons.Default.Speed
    ErrorType.SYNC -> Icons.Default.SyncProblem
    ErrorType.PERMISSIONS -> Icons.Default.Lock
    ErrorType.INFO -> Icons.Default.Info
    ErrorType.UNKNOWN -> Icons.Default.ErrorOutline
}

private data class ErrorColors(
    val containerColor: androidx.compose.ui.graphics.Color,
    val iconBackgroundColor: androidx.compose.ui.graphics.Color,
    val iconColor: androidx.compose.ui.graphics.Color,
    val textColor: androidx.compose.ui.graphics.Color,
    val actionColor: androidx.compose.ui.graphics.Color,
)
