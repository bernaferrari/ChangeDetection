@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.bernaferrari.changedetection.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.navigation3.scene.Scene
import androidx.navigation3.ui.NavDisplay

/**
 * Push and pop share the logical trailing edge, including when reading direction changes. The screen
 * underneath drifts a little (parallax) so the motion reads as depth, and the predictive back gesture
 * drives the same pop, so swiping back previews exactly what releasing will do.
 */
@Composable
internal fun rememberForwardTransitionMetadata(): Map<String, Any> {
    val motionScheme = MaterialTheme.motionScheme
    val direction = LocalLayoutDirection.current
    return remember(motionScheme, direction) {
        val sign = if (direction == LayoutDirection.Rtl) -1 else 1
        val pop = ContentTransform(
            targetContentEnter = slideInHorizontally(
                animationSpec = motionScheme.defaultSpatialSpec(),
                initialOffsetX = { fullWidth -> -sign * fullWidth / 5 },
            ),
            initialContentExit = slideOutHorizontally(
                animationSpec = motionScheme.defaultSpatialSpec(),
                targetOffsetX = { fullWidth -> sign * fullWidth },
            ),
        )
        NavDisplay.transitionSpec {
            ContentTransform(
                targetContentEnter = slideInHorizontally(
                    animationSpec = motionScheme.defaultSpatialSpec(),
                    initialOffsetX = { fullWidth -> sign * fullWidth },
                ),
                initialContentExit = slideOutHorizontally(
                    animationSpec = motionScheme.defaultSpatialSpec(),
                    targetOffsetX = { fullWidth -> -sign * fullWidth / 5 },
                ),
            )
        } + NavDisplay.popTransitionSpec { pop } + NavDisplay.predictivePopTransitionSpec { _ -> pop }
    }
}

private val EmphasizedAccelerate = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)
private val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)

/**
 * Material's fade through, for destinations with no spatial relationship such as navigation bar tabs:
 * the old screen clears quickly, then the new one settles in from a slight scale. Short and
 * sequential, so the two screens never sit half-blended on top of each other.
 */
internal fun <T : Any> fadeThrough(): AnimatedContentTransitionScope<Scene<T>>.() -> ContentTransform = {
    ContentTransform(
        targetContentEnter = fadeIn(tween(durationMillis = 210, delayMillis = 90, easing = EmphasizedDecelerate)) +
            scaleIn(tween(durationMillis = 210, delayMillis = 90, easing = EmphasizedDecelerate), initialScale = 0.92f),
        initialContentExit = fadeOut(tween(durationMillis = 90, easing = EmphasizedAccelerate)),
    )
}
