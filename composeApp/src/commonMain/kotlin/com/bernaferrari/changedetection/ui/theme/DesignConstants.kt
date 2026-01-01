package com.bernaferrari.changedetection.ui.theme

import androidx.compose.ui.unit.dp

/**
 * Centralized design system constants for spacing, animations, and other UI values.
 * This prevents magic numbers throughout the codebase and ensures consistency.
 */

// ==================== SPACING CONSTANTS ====================
object Spacing {
    // Extra small
    val xs = 2.dp
    val xxs = 4.dp

    // Small
    val small = 8.dp
    val smallPlus = 12.dp

    // Medium
    val medium = 16.dp
    val mediumPlus = 20.dp

    // Large
    val large = 24.dp
    val largePlus = 28.dp

    // Extra large
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

// ==================== ANIMATION/TIMING CONSTANTS ====================
object AnimationDuration {
    // Animation durations in milliseconds
    const val EXTRA_FAST = 100
    const val VERY_FAST = 150
    const val FAST = 200
    const val NORMAL = 300
    const val SLOW = 400
    const val VERY_SLOW = 500

    // Specialized durations
    const val SHORT_TRANSITION = 200
    const val STANDARD_TRANSITION = 300
    const val LONG_TRANSITION = 500

    // Component-specific durations
    const val BUTTON_PRESS = 150
    const val RIPPLE = 300
    const val FAB_ANIMATION = 300
    const val DRAWER_ANIMATION = 300

    // Loading animations
    const val PULSE = 1200
    const val SHIMMER = 1500
    const val BOUNCE = 600
    const val SHAKE = 300

    // Special cases
    const val SNACKBAR_DURATION = 4000
    const val TOAST_DURATION = 2000
    const val SUCCESS_ANIMATION = 1500
    const val ERROR_ANIMATION = 400
}

// ==================== M3 EXPRESSIVE SPRING SPECS ====================
/**
 * Material 3 Expressive spring animation specifications.
 * These provide natural, bouncy feel recommended by M3 Expressive guidelines.
 */
object ExpressiveSpring {
    // Standard interactive spring - for button presses, card touches
    const val INTERACTIVE_DAMPING = 0.7f  // Low bounce, responsive
    const val INTERACTIVE_STIFFNESS = 400f

    // Bouncy spring - for emphasized transitions, FAB, chips
    const val BOUNCY_DAMPING = 0.6f
    const val BOUNCY_STIFFNESS = 300f

    // Gentle spring - for subtle state changes, color transitions
    const val GENTLE_DAMPING = 0.8f
    const val GENTLE_STIFFNESS = 200f

    // Quick spring - for snappy micro-interactions
    const val QUICK_DAMPING = 0.75f
    const val QUICK_STIFFNESS = 600f
}

// ==================== OPACITY/ALPHA CONSTANTS ====================
object Alpha {
    const val FULL = 1.0f              // 100%
    const val HIGH = 0.87f             // Material Design high emphasis text
    const val MEDIUM = 0.60f           // Material Design medium emphasis
    const val DISABLED = 0.38f         // Material Design disabled

    // Component-specific alphas
    const val CARD_OVERLAY = 0.18f
    const val HOVER = 0.08f
    const val FOCUS = 0.12f
    const val SELECTED = 0.16f

    // Subtle feedback
    const val SUBTLE = 0.3f
    const val VERY_SUBTLE = 0.2f
    const val BARELY_VISIBLE = 0.1f
}

// ==================== CORNER RADIUS CONSTANTS ====================
object CornerRadius {
    // Extra small
    val xs = 4.dp
    val small = 8.dp

    // Small to medium
    val medium = 12.dp
    val mediumPlus = 14.dp

    // Large
    val large = 16.dp
    val largePlus = 20.dp

    // Extra large
    val xl = 24.dp
    val xxl = 28.dp
    val xxxl = 32.dp

    // Full circle (for FAB, avatars, etc.)
    val full = 50.dp

    // Predefined shape styles
    val cardRadius = medium
    val buttonRadius = mediumPlus
    val fabRadius = xl
    val dialogRadius = large
    val bottomSheetRadius = largePlus
    val surfaceRadius = medium
}

// ==================== ELEVATION CONSTANTS ====================
object Elevation {
    val none = 0.dp
    val xs = 1.dp
    val small = 2.dp
    val medium = 4.dp
    val mediumPlus = 6.dp
    val large = 8.dp
    val xl = 12.dp
    val xxl = 16.dp
    val xxxl = 24.dp

    // Semantic elevation levels
    val surface = small
    val card = medium
    val button = mediumPlus
    val fab = large
    val appBar = mediumPlus
    val drawer = large
    val dialog = xxl
    val modal = xxxl
}

// ==================== STROKE WIDTH CONSTANTS ====================
object StrokeWidth {
    val hairline = 0.5.dp
    val thin = 1.dp
    val normal = 1.5.dp
    val medium = 2.dp
    val bold = 3.dp
}

// ==================== ICON SIZE CONSTANTS ====================
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

// ==================== TEXT SIZE CONSTANTS ====================
// Note: Typography sizes should use the Material Design Type Scale
// These are for reference and special cases only
object TextSize {
    val xs = 10f
    val small = 12f
    val medium = 14f
    val large = 16f
    val xl = 18f
    val xxl = 20f
    val xxxl = 24f
    val huge = 32f
    val massive = 48f
}

// ==================== MOTION/EASING CONSTANTS ====================
object EasingConstants {
    // Standard easing functions - use with Compose's built-in easing
    // Example: animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)

    // Note: Easing is handled via androidx.compose.animation.core.* imports
    // This object serves as documentation of which easing to use where

    // USAGE GUIDE:
    // - FastOutSlowInEasing: Standard material motion (most UI transitions)
    // - LinearEasing: Continuous rotations, progress indicators
    // - FastOutLinearInEasing: Attention-drawing animations
    // - LinearOutSlowInEasing: Entrances (material convention)
    // - CubicBezierEasing: Custom curves for special effects
}

// ==================== Z-INDEX/LAYERING ====================
object ZIndex {
    const val BACKGROUND = 0
    const val DEFAULT = 1
    const val ELEVATED = 5
    const val FLOATING = 10
    const val MODAL = 50
    const val SNACKBAR = 60
    const val DIALOG = 70
    const val MENU = 80
    const val POPOVER = 100
    const val TOOLTIP = 110
}
