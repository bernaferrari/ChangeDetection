/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.interaction.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

/** Apply motion to the icon, keeping its surrounding pointer/touch target still. */
internal class PageIconMotion(val modifier: Modifier, val outlineProgress: Float, val focused: Boolean)

@Composable
internal fun rememberPageIconMotion(interactions: MutableInteractionSource, selected: Boolean = false): PageIconMotion {
    val hovered by interactions.collectIsHoveredAsState()
    val pressed by interactions.collectIsPressedAsState()
    val focused by interactions.collectIsFocusedAsState()
    val ringTarget = when {
        selected || focused -> 1f
        pressed -> 0.65f
        hovered -> 0.35f
        else -> 0f
    }
    val ring by animateFloatAsState(ringTarget, if (focused) snap() else tween(160, easing = LinearOutSlowInEasing), label = "shape-ring-growth")
    val scaleTarget = when {
        focused -> 1f
        pressed -> 0.96f
        selected -> 1.045f
        hovered -> 1.035f
        else -> 1f
    }
    val scale by animateFloatAsState(scaleTarget, if (focused) snap() else spring(dampingRatio = 0.65f, stiffness = 900f), label = "shape-lift")
    val tilt by animateFloatAsState(if (hovered && !selected && !focused) -5f else 0f, if (focused) snap() else spring(dampingRatio = 0.7f, stiffness = 900f), label = "shape-hover-tilt")
    val wobble = remember { Animatable(0f) }
    var previouslySelected by remember { mutableStateOf(selected) }
    LaunchedEffect(selected) {
        val newlySelected = selected && !previouslySelected
        previouslySelected = selected
        // Native Compose animations honor the platform's motion duration scale, including zero.
        // Opening the picker and keyboard navigation leave the icons at rest.
        if (newlySelected && !focused) {
            wobble.animateTo(-8f, tween(50, easing = LinearOutSlowInEasing))
            wobble.animateTo(0f, spring(dampingRatio = 0.5f, stiffness = 1600f))
        } else {
            wobble.snapTo(0f)
        }
    }
    return PageIconMotion(
        Modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
            rotationZ = tilt + wobble.value
        },
        ring,
        focused,
    )
}
