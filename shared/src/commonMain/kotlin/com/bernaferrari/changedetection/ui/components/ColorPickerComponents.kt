/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import com.bernaferrari.changedetection.ui.theme.marks
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.LinearGradientShader
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * A gradient color swatch with expressive motion: unselected it is a disc turned 45°; chosen, it springs
 * upright into a rounded square, lifts, and gains a ring. The gradient counter-rotates, so the colors hold
 * still while the shape turns. The change of shape (not only color) shows which one is selected.
 */
@Composable
fun PaletteSwatch(
    colors: List<Color>,
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    icon: ImageVector? = null,
) {
    val haptics = LocalHapticFeedback.current
    val interactions = remember { MutableInteractionSource() }
    val focused by interactions.collectIsFocusedAsState()
    val bouncy = spring<Dp>(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
    val corner by animateDpAsState(if (selected) size * 0.3f else size / 2, bouncy, label = "corner")
    val elevation by animateDpAsState(if (selected) 6.dp else 1.dp, bouncy, label = "elevation")
    val ringAnimated by animateDpAsState(if (selected) 3.dp else 0.dp, bouncy, label = "ring")
    // The bounce overshoots; widths and elevation never go below zero.
    val ring = ringAnimated.coerceAtLeast(0.dp)
    val rotation by animateFloatAsState(
        if (selected) 0f else 45f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "rotation",
    )
    val ringColor by animateColorAsState(if (selected) MaterialTheme.colorScheme.surfaceContainerLow else Color.Transparent, label = "ring-color")

    // Counter-rotate the gradient so it stays put on screen while the swatch turns.
    val stops = if (colors.size == 1) colors + colors else colors
    val angle = -rotation * PI.toFloat() / 180f
    val brush = object : ShaderBrush() {
        override fun createShader(size: Size): Shader {
            // The diagonal, turned by the counter-rotation, centred on the swatch.
            val half = Offset(size.width * cos(angle) - size.height * sin(angle), size.width * sin(angle) + size.height * cos(angle)) / 2f
            val center = Offset(size.width / 2, size.height / 2)
            return LinearGradientShader(from = center - half, to = center + half, colors = stops)
        }
    }
    val outer = RoundedCornerShape(corner.coerceAtLeast(0.dp))
    val inner = RoundedCornerShape((corner - ring).coerceAtLeast(0.dp))
    val core = RoundedCornerShape((corner - ring - 2.dp).coerceAtLeast(0.dp))
    Box(
        modifier
            .size(size)
            .semantics { contentDescription = label }
            .selectable(selected = selected, role = Role.RadioButton, interactionSource = interactions, indication = null) {
                if (!selected) {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onClick()
                }
            }
            .rotate(rotation)
            .shadow(elevation.coerceAtLeast(0.dp), outer, clip = false)
            .clip(outer)
            .background(brush)
            .then(if (focused) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, outer) else Modifier)
            .padding(ring)
            .clip(inner)
            .background(ringColor)
            .padding(if (selected) 2.dp else 0.dp)
            .clip(core)
            .background(brush),
        contentAlignment = Alignment.Center,
    ) {
        if (icon != null) {
            Icon(icon, null, Modifier.size(size * 0.45f).rotate(-rotation), tint = Color.White)
        }
    }
}

/** Shared preview colors for Appearance and individual page icons. */
@Composable
internal fun paletteSwatchColors(palette: com.bernaferrari.changedetection.model.AppPalette): List<Color> = when (palette) {
    com.bernaferrari.changedetection.model.AppPalette.DEFAULT -> listOf(com.bernaferrari.changedetection.ui.theme.BrandSeed, MaterialTheme.marks.highlight)
    com.bernaferrari.changedetection.model.AppPalette.DYNAMIC -> listOf(Color(0xFF7C4DFF), Color(0xFF00BFA5))
    else -> listOf(palette.seedColor, androidx.compose.ui.graphics.lerp(palette.seedColor, Color.White, 0.45f))
}
