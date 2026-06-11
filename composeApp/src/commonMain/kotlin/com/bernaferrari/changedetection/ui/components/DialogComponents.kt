/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bernaferrari.changedetection.shared.generated.resources.Res
import com.bernaferrari.changedetection.shared.generated.resources.hour
import com.bernaferrari.changedetection.shared.generated.resources.hours
import com.bernaferrari.changedetection.shared.generated.resources.min
import com.bernaferrari.changedetection.ui.theme.ExpressiveSpring
import org.jetbrains.compose.resources.stringResource

/**
 * Reusable confirmation dialog for destructive actions.
 * Features an icon, title, message, and cancel/confirm buttons.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfirmDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    title: String,
    message: String,
    icon: ImageVector? = null,
    confirmText: String = "Delete",
    isDestructive: Boolean = true,
    modifier: Modifier = Modifier,
) {
    if (!visible) return

    val iconColor by animateColorAsState(
        targetValue = if (isDestructive) {
            MaterialTheme.colorScheme.error
        } else {
            MaterialTheme.colorScheme.primary
        },
        animationSpec = spring(
            dampingRatio = ExpressiveSpring.GENTLE_DAMPING,
            stiffness = ExpressiveSpring.GENTLE_STIFFNESS,
        ),
        label = "iconColor",
    )

    val iconBackgroundColor by animateColorAsState(
        targetValue = if (isDestructive) {
            MaterialTheme.colorScheme.errorContainer
        } else {
            MaterialTheme.colorScheme.primaryContainer
        },
        animationSpec = spring(
            dampingRatio = ExpressiveSpring.GENTLE_DAMPING,
            stiffness = ExpressiveSpring.GENTLE_STIFFNESS,
        ),
        label = "iconBackgroundColor",
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Icon
                if (icon != null) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(iconBackgroundColor),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconColor,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Title
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Message
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            text = "Cancel",
                            fontWeight = FontWeight.Medium,
                        )
                    }

                    Button(
                        onClick = {
                            onConfirm()
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDestructive) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.primary
                            },
                        ),
                    ) {
                        Text(
                            text = confirmText,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }
        }
    }
}

/**
 * State holder for confirmation dialog to make usage easier.
 */
class ConfirmDialogState {
    var isVisible by mutableStateOf(false)
        private set
    var pendingAction by mutableStateOf<(() -> Unit)?>(null)
        private set
    var title by mutableStateOf("")
        private set
    var message by mutableStateOf("")
        private set
    var icon by mutableStateOf<ImageVector?>(null)
        private set
    var confirmText by mutableStateOf("Delete")
        private set

    fun show(
        title: String,
        message: String,
        icon: ImageVector? = null,
        confirmText: String = "Delete",
        onConfirm: () -> Unit,
    ) {
        this.title = title
        this.message = message
        this.icon = icon
        this.confirmText = confirmText
        this.pendingAction = onConfirm
        this.isVisible = true
    }

    fun confirm() {
        pendingAction?.invoke()
        dismiss()
    }

    fun dismiss() {
        isVisible = false
        pendingAction = null
    }
}

@Composable
fun rememberConfirmDialogState(): ConfirmDialogState = remember { ConfirmDialogState() }

@Composable
fun DialogHeader(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun DialogSwitchItem(
    title: String,
    icon: ImageVector,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primary,
        )

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )

        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
        )
    }
}

@Composable
fun DialogSeparator(
    title: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
    ) {
        HorizontalDivider()
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
fun DialogActions(
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
    cancelText: String = "Cancel",
    confirmText: String = "OK",
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalArrangement = Arrangement.End,
    ) {
        TextButton(onClick = onCancel) {
            Text(cancelText)
        }

        Spacer(modifier = Modifier.width(8.dp))

        TextButton(onClick = onConfirm) {
            Text(confirmText)
        }
    }
}

@Composable
fun DialogIntervalItem(
    title: String,
    initialDelay: Int,
    onValueChange: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val minutes = arrayOf(15, 30, 45, 60, 120, 240, 360, 720, 1440, 2880)
    var sliderValue by remember {
        mutableFloatStateOf(minutes.indexOfFirst { it == initialDelay }.coerceAtLeast(0).toFloat())
    }

    val minLabel = stringResource(Res.string.min)
    val hourLabel = stringResource(Res.string.hour)
    val hoursLabel = stringResource(Res.string.hours)

    val getTimeString = { value: Int ->
        when {
            value < 60 -> "$value $minLabel"
            value == 60 -> hourLabel
            else -> "${value / 60} $hoursLabel"
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Text(
                text = getTimeString(minutes[sliderValue.toInt()]),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Slider(
            value = sliderValue,
            onValueChange = { newValue ->
                sliderValue = newValue
                onValueChange(minutes[newValue.toInt()].toLong())
            },
            valueRange = 0f..(minutes.size - 1).toFloat(),
            steps = minutes.size - 2,
        )
    }
}
