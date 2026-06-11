/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Shared typography tokens for consistent styling.
 */
object SharedTypography {
    val cardTitle: TextStyle
        @Composable
        get() = MaterialTheme.typography.titleMedium

    val cardSubtitle: TextStyle
        @Composable
        get() = MaterialTheme.typography.bodyMedium

    val captionText: TextStyle
        @Composable
        get() = MaterialTheme.typography.bodySmall

    val sectionHeader: TextStyle
        @Composable
        get() = MaterialTheme.typography.titleSmall
}
