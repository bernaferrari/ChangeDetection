/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Reusable skeleton list matching the main monitor list's loading visual language:
 * static [MaterialTheme.colorScheme.surfaceContainerLow] placeholders, no shimmer.
 *
 * Unlike the previous embedded version, this carries no spinner row — callers that want a
 * progress affordance must layer it themselves.
 *
 * @param itemCount number of placeholder rows to draw.
 * @param showHeaderChips whether to render the filter-chip row placeholder.
 */
@Composable
fun ListSkeleton(
    modifier: Modifier = Modifier,
    itemCount: Int = 5,
    showHeaderChips: Boolean = false,
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopStart) {
        Column(
            modifier = Modifier
                .widthIn(max = 720.dp)
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (showHeaderChips) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    repeat(4) {
                        Surface(
                            modifier = Modifier.widthIn(min = 78.dp).height(44.dp),
                            shape = MaterialTheme.shapes.extraLarge,
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                        ) {}
                    }
                }
            }
            repeat(itemCount) {
                Surface(
                    modifier = Modifier.fillMaxWidth().height(68.dp),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                ) {}
            }
        }
    }
}
