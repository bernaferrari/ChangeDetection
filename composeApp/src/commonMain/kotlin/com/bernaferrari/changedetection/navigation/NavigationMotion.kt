/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.bernaferrari.changedetection.navigation

import androidx.compose.animation.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.navigation3.ui.NavDisplay

/** Push and pop share the logical trailing edge, including when reading direction changes. */
@Composable
internal fun rememberForwardTransitionMetadata(): Map<String, Any> {
    val motionScheme = MaterialTheme.motionScheme
    val direction = LocalLayoutDirection.current
    return remember(motionScheme, direction) {
        val sign = if (direction == LayoutDirection.Rtl) -1 else 1
        NavDisplay.transitionSpec {
            ContentTransform(
                targetContentEnter = slideInHorizontally(
                    animationSpec = motionScheme.defaultSpatialSpec(),
                    initialOffsetX = { fullWidth -> sign * fullWidth },
                ) + fadeIn(animationSpec = motionScheme.fastEffectsSpec()),
                initialContentExit = ExitTransition.None,
            )
        } + NavDisplay.popTransitionSpec {
            ContentTransform(
                targetContentEnter = EnterTransition.None,
                initialContentExit = slideOutHorizontally(
                    animationSpec = motionScheme.defaultSpatialSpec(),
                    targetOffsetX = { fullWidth -> sign * fullWidth },
                ) + fadeOut(animationSpec = motionScheme.fastEffectsSpec()),
            )
        }
    }
}
