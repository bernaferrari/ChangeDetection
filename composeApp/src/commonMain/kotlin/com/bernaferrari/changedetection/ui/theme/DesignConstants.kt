/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.ui.theme

import androidx.compose.ui.unit.dp

/**
 * Legacy design tokens that remain while complex screens are migrated.
 *
 * New product UI uses MaterialTheme plus [AppSpacing]. The spring and radius objects stay here
 * only for legacy custom-shape animations, which are intentionally being retired screen by screen.
 */
object Spacing {
    val xs = 2.dp
    val xxs = 4.dp

    val small = 8.dp
    val smallPlus = 12.dp

    val medium = 16.dp
    val mediumPlus = 20.dp

    val large = 24.dp
    val largePlus = 28.dp

    val xl = 32.dp
    val xxl = 48.dp
    val xxxl = 64.dp

    // Standard padding values (commonly used)
    val paddingXSmall = xxs
    val paddingSmall = small
    val paddingMedium = medium
    val paddingLarge = large

    // Standard margin values
    val marginSmall = small
    val marginMedium = medium
    val marginLarge = large
}

/**
 * Compatibility spring constants for pre-existing custom animations.
 *
 * Prefer MaterialTheme.motionScheme for new components.
 */
object ExpressiveSpring {
    const val INTERACTIVE_DAMPING = 0.7f
    const val INTERACTIVE_STIFFNESS = 400f

    const val BOUNCY_DAMPING = 0.6f
    const val BOUNCY_STIFFNESS = 300f

    const val GENTLE_DAMPING = 0.8f
    const val GENTLE_STIFFNESS = 200f

    const val QUICK_DAMPING = 0.75f
    const val QUICK_STIFFNESS = 600f
}

object Alpha {
    const val FULL = 1.0f
    const val HIGH = 0.87f
    const val MEDIUM = 0.60f
    const val DISABLED = 0.38f

    const val CARD_OVERLAY = 0.18f
    const val HOVER = 0.08f
    const val FOCUS = 0.12f
    const val SELECTED = 0.16f

    const val SUBTLE = 0.3f
    const val VERY_SUBTLE = 0.2f
    const val BARELY_VISIBLE = 0.1f
}

/**
 * Compatibility radii for older screens with custom per-corner animation.
 *
 * Prefer MaterialTheme.shapes for new components.
 */
object CornerRadius {
    val xs = 4.dp
    val small = 8.dp

    val medium = 12.dp
    val mediumPlus = 14.dp

    val large = 16.dp
    val largePlus = 20.dp

    val xl = 24.dp
    val xxl = 28.dp
    val xxxl = 32.dp

    val full = 50.dp

    val cardRadius = medium
    val buttonRadius = mediumPlus
    val fabRadius = xl
    val dialogRadius = large
    val bottomSheetRadius = largePlus
    val surfaceRadius = medium
}

object IconSize {
    val xs = 16.dp
    val small = 20.dp
    val medium = 24.dp
    val mediumPlus = 28.dp
    val large = 32.dp
    val xl = 40.dp
    val xxl = 48.dp

    // Semantic sizes
    val toolbar = medium
    val button = medium
    val fab = medium
    val appBarIcon = large
}
