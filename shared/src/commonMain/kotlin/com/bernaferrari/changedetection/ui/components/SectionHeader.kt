package com.bernaferrari.changedetection.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.bernaferrari.changedetection.ui.theme.spacing

/**
 * Shared section header: `titleSmall`, primary color, SemiBold, with the app-wide
 * start=xl / top=lg / bottom=xs paddings (end=lg).
 *
 * Extracted from the Settings screen so every screen uses one identical header treatment.
 */
@Composable
fun SectionHeader(title: String) {
    val spacing = MaterialTheme.spacing
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(
            start = spacing.xl,
            end = spacing.lg,
            top = spacing.lg,
            bottom = spacing.xs,
        ),
    )
}
