package com.bernaferrari.changedetection.ui.interactions

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp

/**
 * Award-winning micro-interactions and haptic feedback system
 */
object MicroInteractions {
    
    /**
     * Bouncy press animation with haptic feedback
     */
    @Composable
    fun Modifier.bouncyPress(
        enabled: Boolean = true,
        onPress: (() -> Unit)? = null,
        hapticFeedback: Boolean = true
    ): Modifier = composed {
        val haptic = LocalHapticFeedback.current
        val interactionSource = remember { MutableInteractionSource() }
        val isPressed by interactionSource.collectIsPressedAsState()
        
        val scale by animateFloatAsState(
            targetValue = if (isPressed && enabled) 0.95f else 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            ),
            label = "bouncy_press_scale"
        )
        
        LaunchedEffect(isPressed) {
            if (isPressed && enabled) {
                if (hapticFeedback) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }
                onPress?.invoke()
            }
        }
        
        this.scale(scale)
    }
    
    /**
     * Gentle float animation
     */
    @Composable
    fun Modifier.gentleFloat(
        enabled: Boolean = true,
        amplitude: Float = 4f,
        duration: Int = 3000
    ): Modifier = composed {
        val infiniteTransition = rememberInfiniteTransition(label = "gentle_float")
        
        val offsetY by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = amplitude,
            animationSpec = infiniteRepeatable(
                animation = tween(duration, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "float_offset"
        )
        
        if (enabled) {
            this.graphicsLayer {
                translationY = offsetY
            }
        } else {
            this
        }
    }
    
    /**
     * Pulse animation for attention
     */
    @Composable
    fun Modifier.pulse(
        enabled: Boolean = true,
        minScale: Float = 0.95f,
        maxScale: Float = 1.05f,
        duration: Int = 1000
    ): Modifier = composed {
        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
        
        val scale by infiniteTransition.animateFloat(
            initialValue = minScale,
            targetValue = maxScale,
            animationSpec = infiniteRepeatable(
                animation = tween(duration, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse_scale"
        )
        
        if (enabled) {
            this.scale(scale)
        } else {
            this
        }
    }
    
    /**
     * Shake animation for errors
     */
    @Composable
    fun Modifier.shake(
        enabled: Boolean = false,
        amplitude: Float = 10f,
        duration: Int = 300
    ): Modifier = composed {
        var shakeState by remember { mutableStateOf(enabled) }
        
        val offsetX by animateFloatAsState(
            targetValue = if (shakeState) amplitude else 0f,
            animationSpec = tween(duration / 4, easing = LinearEasing),
            finishedListener = {
                if (shakeState) {
                    shakeState = false
                }
            },
            label = "shake_offset"
        )
        
        LaunchedEffect(enabled) {
            if (enabled) {
                shakeState = true
            }
        }
        
        this.graphicsLayer {
            translationX = if (shakeState) {
                // Create shake effect by alternating direction
                offsetX * kotlin.math.sin(System.currentTimeMillis() / 20.0).toFloat()
            } else {
                0f
            }
        }
    }
    
    /**
     * Shimmer loading effect
     */
    @Composable
    fun Modifier.shimmer(
        enabled: Boolean = true,
        duration: Int = 1500
    ): Modifier = composed {
        val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
        
        val shimmerTranslate by infiniteTransition.animateFloat(
            initialValue = -200f,
            targetValue = 200f,
            animationSpec = infiniteRepeatable(
                animation = tween(duration, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "shimmer_translate"
        )
        
        if (enabled) {
            this.graphicsLayer {
                translationX = shimmerTranslate
                alpha = 0.7f
            }
        } else {
            this
        }
    }
    
    /**
     * Swipe to reveal animation
     */
    @Composable
    fun Modifier.swipeReveal(
        onSwipe: () -> Unit,
        threshold: Float = 100f,
        hapticFeedback: Boolean = true
    ): Modifier = composed {
        val haptic = LocalHapticFeedback.current
        var offsetX by remember { mutableStateOf(0f) }
        var hasTriggered by remember { mutableStateOf(false) }
        
        val animatedOffsetX by animateFloatAsState(
            targetValue = offsetX,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            ),
            label = "swipe_reveal_offset"
        )
        
        this
            .graphicsLayer {
                translationX = animatedOffsetX
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { /* Handle press */ },
                    onTap = { /* Handle tap */ }
                )
            }
    }
    
    /**
     * Success celebration animation
     */
    @Composable
    fun Modifier.celebrate(
        trigger: Boolean = false,
        onComplete: () -> Unit = {}
    ): Modifier = composed {
        val haptic = LocalHapticFeedback.current
        var isAnimating by remember { mutableStateOf(false) }
        
        val scale by animateFloatAsState(
            targetValue = if (isAnimating) 1.2f else 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessHigh
            ),
            finishedListener = {
                if (isAnimating) {
                    isAnimating = false
                    onComplete()
                }
            },
            label = "celebrate_scale"
        )
        
        LaunchedEffect(trigger) {
            if (trigger) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                isAnimating = true
            }
        }
        
        this.scale(scale)
    }
    
    /**
     * Loading breathe animation
     */
    @Composable
    fun Modifier.breathe(
        enabled: Boolean = true,
        minAlpha: Float = 0.5f,
        maxAlpha: Float = 1f,
        duration: Int = 2000
    ): Modifier = composed {
        val infiniteTransition = rememberInfiniteTransition(label = "breathe")
        
        val alpha by infiniteTransition.animateFloat(
            initialValue = minAlpha,
            targetValue = maxAlpha,
            animationSpec = infiniteRepeatable(
                animation = tween(duration, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "breathe_alpha"
        )
        
        if (enabled) {
            this.graphicsLayer {
                this.alpha = alpha
            }
        } else {
            this
        }
    }
    
    /**
     * Magnetic snap effect
     */
    @Composable
    fun Modifier.magneticSnap(
        enabled: Boolean = true,
        attraction: Float = 1.1f,
        onSnap: () -> Unit = {}
    ): Modifier = composed {
        val haptic = LocalHapticFeedback.current
        val interactionSource = remember { MutableInteractionSource() }
        val isPressed by interactionSource.collectIsPressedAsState()
        
        val scale by animateFloatAsState(
            targetValue = if (isPressed && enabled) attraction else 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessHigh
            ),
            finishedListener = {
                if (it == attraction && enabled) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onSnap()
                }
            },
            label = "magnetic_snap_scale"
        )
        
        this.scale(scale)
    }
    
    /**
     * Parallax scroll effect
     */
    @Composable
    fun Modifier.parallax(
        scrollOffset: Float,
        rate: Float = 0.5f
    ): Modifier = composed {
        this.graphicsLayer {
            translationY = scrollOffset * rate
        }
    }
    
    /**
     * Typewriter animation for text
     */
    @Composable
    fun typewriterEffect(
        text: String,
        duration: Int = 50,
        onComplete: () -> Unit = {}
    ): String {
        var displayedText by remember { mutableStateOf("") }
        
        LaunchedEffect(text) {
            displayedText = ""
            text.forEachIndexed { index, _ ->
                kotlinx.coroutines.delay(duration.toLong())
                displayedText = text.take(index + 1)
            }
            onComplete()
        }
        
        return displayedText
    }
}

/**
 * Haptic feedback patterns for different interactions
 */
object HapticPatterns {
    @Composable
    fun success() {
        val haptic = LocalHapticFeedback.current
        LaunchedEffect(Unit) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            kotlinx.coroutines.delay(100)
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }
    
    @Composable
    fun error() {
        val haptic = LocalHapticFeedback.current
        LaunchedEffect(Unit) {
            repeat(3) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                kotlinx.coroutines.delay(50)
            }
        }
    }
    
    @Composable
    fun notification() {
        val haptic = LocalHapticFeedback.current
        LaunchedEffect(Unit) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }
    
    @Composable
    fun selection() {
        val haptic = LocalHapticFeedback.current
        LaunchedEffect(Unit) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }
}