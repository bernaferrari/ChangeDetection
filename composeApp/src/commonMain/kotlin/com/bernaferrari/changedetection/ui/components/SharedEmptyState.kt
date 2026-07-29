/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bernaferrari.changedetection.ui.theme.spacing

@Composable
fun SharedEmptyState(
    icon: ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    primaryActionLabel: String? = null,
    onPrimaryAction: (() -> Unit)? = null,
    secondaryActionLabel: String? = null,
    onSecondaryAction: (() -> Unit)? = null,
) {
    val spacing = MaterialTheme.spacing

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(spacing.xl),
        ) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(MaterialTheme.shapes.extraLarge)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(spacing.touchTarget),
                )
            }

            Spacer(modifier = Modifier.height(spacing.lg))

            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(spacing.xs))

            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            if (primaryActionLabel != null && onPrimaryAction != null) {
                Spacer(modifier = Modifier.height(spacing.lg))
                Button(
                    onClick = onPrimaryAction,
                    modifier = Modifier.heightIn(min = spacing.touchTarget),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                ) {
                    Text(primaryActionLabel)
                }
            }

            if (secondaryActionLabel != null && onSecondaryAction != null) {
                if (primaryActionLabel == null) {
                    Spacer(modifier = Modifier.height(spacing.lg))
                } else {
                    Spacer(
                        modifier = Modifier.height(spacing.xs),
                    )
                }
                TextButton(
                    onClick = onSecondaryAction,
                    modifier = Modifier.heightIn(min = spacing.touchTarget),
                ) {
                    Text(secondaryActionLabel)
                }
            }
        }
    }
}
