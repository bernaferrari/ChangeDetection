/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.bernaferrari.changedetection.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Decorative mark for empty states; never represents check activity or change magnitude. */
@Composable
internal fun ChangeRibbon(
    colors: List<Int>,
    isListening: Boolean,
    modifier: Modifier = Modifier,
) {
    val fallback = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.primaryContainer,
        MaterialTheme.colorScheme.tertiaryContainer,
        MaterialTheme.colorScheme.secondaryContainer,
    )
    val beforeHeights = listOf(16.dp, 24.dp, 12.dp, 20.dp, 28.dp, 15.dp)
    val afterHeights = listOf(22.dp, 14.dp, 30.dp, 18.dp, 13.dp, 25.dp)
    val weights = listOf(0.82f, 1.08f, 0.92f, 1.2f, 0.88f, 1.02f)

    Box(modifier = modifier.fillMaxWidth().height(72.dp), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier.fillMaxWidth().height(2.dp)
                .background(MaterialTheme.colorScheme.outlineVariant),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            repeat(6) { index ->
                val color = colors.getOrNull(index)?.let(::Color) ?: fallback[index]
                Column(
                    modifier = Modifier.weight(weights[index]).padding(vertical = 2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentAlignment = Alignment.BottomCenter,
                    ) {
                        Surface(
                            modifier = Modifier.fillMaxWidth().height(beforeHeights[index]),
                            shape = RoundedCornerShape(
                                topStart = if (index % 2 == 0) 14.dp else 5.dp,
                                topEnd = if (index % 2 == 0) 5.dp else 14.dp,
                                bottomStart = 2.dp,
                                bottomEnd = 2.dp,
                            ),
                            color = color.copy(alpha = if (isListening) 0.46f else 0.28f),
                            content = {},
                        )
                    }
                    Box(Modifier.height(4.dp))
                    Box(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentAlignment = Alignment.TopCenter,
                    ) {
                        Surface(
                            modifier = Modifier.fillMaxWidth().height(afterHeights[index]),
                            shape = RoundedCornerShape(
                                topStart = 2.dp,
                                topEnd = 2.dp,
                                bottomStart = if (index % 2 == 0) 5.dp else 14.dp,
                                bottomEnd = if (index % 2 == 0) 14.dp else 5.dp,
                            ),
                            color = color.copy(alpha = if (isListening && index % 2 == 0) 0.58f else 1f),
                            content = {},
                        )
                    }
                }
            }
        }
    }
}

/** Compact form of the before/after signature for instructional and editor surfaces. */
@Composable
internal fun ChangeMark(
    modifier: Modifier = Modifier.size(width = 76.dp, height = 48.dp),
) {
    val before = listOf(10.dp, 17.dp, 8.dp, 14.dp)
    val after = listOf(16.dp, 9.dp, 20.dp, 12.dp)
    val colors = listOf(
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.primary,
    )
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Box(Modifier.fillMaxWidth().height(2.dp).background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.22f)))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            repeat(4) { index ->
                Column(modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentAlignment = Alignment.BottomCenter,
                    ) {
                        Surface(
                            modifier = Modifier.fillMaxWidth().height(before[index]),
                            shape = RoundedCornerShape(8.dp, 3.dp, 2.dp, 2.dp),
                            color = colors[index].copy(alpha = 0.28f),
                            content = {},
                        )
                    }
                    Box(Modifier.height(4.dp))
                    Box(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentAlignment = Alignment.TopCenter,
                    ) {
                        Surface(
                            modifier = Modifier.fillMaxWidth().height(after[index]),
                            shape = RoundedCornerShape(2.dp, 2.dp, 8.dp, 3.dp),
                            color = colors[index],
                            content = {},
                        )
                    }
                }
            }
        }
    }
}
