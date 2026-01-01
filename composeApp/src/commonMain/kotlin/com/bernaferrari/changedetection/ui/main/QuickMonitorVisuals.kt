package com.bernaferrari.changedetection.ui.main

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

@Immutable
data class QuickMonitorVisuals(
    val gradient: Brush,
    val onGradient: Color
)

@Composable
fun rememberQuickMonitorVisuals(): QuickMonitorVisuals {
    val colorScheme = MaterialTheme.colorScheme
    val gradient = remember(colorScheme.primary, colorScheme.primaryContainer, colorScheme.secondary) {
        Brush.linearGradient(
            colors = listOf(
                colorScheme.primary,
                colorScheme.primaryContainer,
                colorScheme.secondary
            )
        )
    }

    val onGradient = remember(colorScheme.onPrimary, colorScheme.onSecondary) {
        val primaryContrast = colorScheme.onPrimary.luminance()
        val secondaryContrast = colorScheme.onSecondary.luminance()
        if (primaryContrast >= secondaryContrast) colorScheme.onPrimary else colorScheme.onSecondary
    }

    return remember(gradient, onGradient) { QuickMonitorVisuals(gradient = gradient, onGradient = onGradient) }
}
