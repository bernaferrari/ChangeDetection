/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.ui.animations

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.unit.dp

/**
 * Smooth bounce animation for interactive elements
 */
@Composable
fun Modifier.bounceClick(
    enabled: Boolean = true,
    scaleDown: Float = 0.95f,
    onClick: () -> Unit = {},
): Modifier = composed {
    val haptic = LocalHapticFeedback.current
    var isPressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) scaleDown else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "bounce_scale",
    )

    this
        .scale(scale)
        .pointerInput(enabled) {
            if (!enabled) return@pointerInput

            detectTapGestures(
                onPress = {
                    isPressed = true
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    tryAwaitRelease()
                    isPressed = false
                },
                onTap = {
                    onClick()
                },
            )
        }
}

/**
 * Pulse animation for drawing attention to elements
 */
@Composable
fun rememberPulseAnimation(
    minScale: Float = 0.97f,
    maxScale: Float = 1.03f,
    durationMillis: Int = 1200,
): Float {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    return infiniteTransition.animateFloat(
        initialValue = minScale,
        targetValue = maxScale,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse_scale",
    ).value
}

/**
 * Shimmer animation for loading states
 */
@Composable
fun rememberShimmerAnimation(): Float {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    return infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmer_progress",
    ).value
}

/**
 * Smooth slide-in/out transitions for lists
 */
fun <T> slideInOutTransitionSpec(): AnimatedContentTransitionScope<T>.() -> ContentTransform = {
    val enterAnim = slideInVertically(
        initialOffsetY = { it / 2 },
        animationSpec = tween(300, easing = FastOutSlowInEasing),
    ) + fadeIn(animationSpec = tween(300))

    val exitAnim = slideOutVertically(
        targetOffsetY = { -it / 2 },
        animationSpec = tween(200, easing = FastOutLinearInEasing),
    ) + fadeOut(animationSpec = tween(200))

    enterAnim togetherWith exitAnim
}

/**
 * Stagger animation for list items
 */
fun staggeredDelay(index: Int, delayMillis: Int = 50): Int = (index * delayMillis).coerceAtMost(500)

/**
 * Animated visibility with custom specs
 */
@Composable
fun EnhancedAnimatedVisibility(
    visible: Boolean,
    modifier: Modifier = Modifier,
    enter: EnterTransition = slideInVertically(
        initialOffsetY = { it / 4 },
        animationSpec = tween(300, easing = FastOutSlowInEasing),
    ) + fadeIn(animationSpec = tween(300)),
    exit: ExitTransition = slideOutVertically(
        targetOffsetY = { -it / 4 },
        animationSpec = tween(200),
    ) + fadeOut(animationSpec = tween(200)),
    content: @Composable AnimatedVisibilityScope.() -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = enter,
        exit = exit,
        content = content,
    )
}

/**
 * Rotation animation for icons
 */
@Composable
fun rememberRotationAnimation(
    durationMillis: Int = 1000,
    enabled: Boolean = true,
): Float {
    val infiniteTransition = rememberInfiniteTransition(label = "rotation")
    return if (enabled) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "rotation_degrees",
        ).value
    } else {
        0f
    }
}

/**
 * Shake animation for error states
 */
@Composable
fun Modifier.shake(trigger: Int): Modifier = composed {
    var shake by remember { mutableStateOf(0f) }

    LaunchedEffect(trigger) {
        if (trigger > 0) {
            for (i in 0..8) {
                when (i % 3) {
                    0 -> shake = 5f
                    1 -> shake = -5f
                    else -> shake = 0f
                }
                kotlinx.coroutines.delay(50)
            }
            shake = 0f
        }
    }

    this.graphicsLayer {
        translationX = shake
    }
}

/**
 * Press and hold scale animation
 */
@Composable
fun Modifier.pressAndHoldScale(
    pressedScale: Float = 0.96f,
    unpressedScale: Float = 1f,
): Modifier = composed {
    var isPressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) pressedScale else unpressedScale,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh,
        ),
        label = "press_scale",
    )

    this
        .scale(scale)
        .pointerInput(Unit) {
            detectTapGestures(
                onPress = {
                    isPressed = true
                    tryAwaitRelease()
                    isPressed = false
                },
            )
        }
}

/**
 * Breathe animation for attention-drawing elements
 */
@Composable
fun rememberBreatheAnimation(
    minAlpha: Float = 0.6f,
    maxAlpha: Float = 1f,
    durationMillis: Int = 2000,
): Float {
    val infiniteTransition = rememberInfiniteTransition(label = "breathe")
    return infiniteTransition.animateFloat(
        initialValue = minAlpha,
        targetValue = maxAlpha,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "breathe_alpha",
    ).value
}

/**
 * Elevation animation on hover/press
 */
@Composable
fun animateElevationAsState(
    targetElevation: Float,
): State<Float> = animateFloatAsState(
    targetValue = targetElevation,
    animationSpec = tween(
        durationMillis = 150,
        easing = FastOutSlowInEasing,
    ),
    label = "elevation",
)
