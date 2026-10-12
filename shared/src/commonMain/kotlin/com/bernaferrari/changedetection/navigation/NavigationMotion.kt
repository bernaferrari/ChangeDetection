@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.bernaferrari.changedetection.navigation

import androidx.compose.animation.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
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
