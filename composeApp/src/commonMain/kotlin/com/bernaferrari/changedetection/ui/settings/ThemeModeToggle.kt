/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bernaferrari.changedetection.model.ThemeMode
import org.jetbrains.compose.resources.stringResource

/** A stable, labelled theme choice that keeps selection obvious without motion. */
@Composable
fun ThemeModeToggle(
    themeMode: ThemeMode,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val title = stringResource(themeMode.titleRes)
    val description = stringResource(themeMode.descriptionRes)
    val containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
    val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        OutlinedCard(
            onClick = onClick,
            modifier = Modifier.height(64.dp),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.outlinedCardColors(containerColor = containerColor),
            border = BorderStroke(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
            ),
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (isSelected) themeMode.selectedIcon else themeMode.icon,
                    contentDescription = title,
                    modifier = Modifier.size(24.dp),
                    tint = contentColor,
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
    }
}
