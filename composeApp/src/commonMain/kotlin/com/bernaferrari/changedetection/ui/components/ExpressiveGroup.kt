/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.ui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * Shared geometry for connected Material 3 Expressive groups.
 *
 * Keeping this in one place prevents Settings, History, Turbo, and monitor details from slowly
 * drifting into different corner systems.
 */
enum class SegmentPosition {
    Only,
    First,
    Middle,
    Last,
}

fun segmentPosition(first: Boolean, last: Boolean): SegmentPosition = when {
    first && last -> SegmentPosition.Only
    first -> SegmentPosition.First
    last -> SegmentPosition.Last
    else -> SegmentPosition.Middle
}

fun segmentShape(position: SegmentPosition): Shape = when (position) {
    SegmentPosition.Only -> RoundedCornerShape(24.dp)
    SegmentPosition.First -> RoundedCornerShape(24.dp, 24.dp, 8.dp, 8.dp)
    SegmentPosition.Middle -> RoundedCornerShape(8.dp)
    SegmentPosition.Last -> RoundedCornerShape(8.dp, 8.dp, 24.dp, 24.dp)
}

/**
 * The app's single hero shape.
 *
 * Used for the one oversized, attention-grabbing element on a screen (e.g. a featured card).
 * Everything else must use [segmentShape] or standard Material shapes so the corner system
 * stays coherent.
 */
val SignalHeroShape: Shape = RoundedCornerShape(28.dp, 28.dp, 28.dp, 12.dp)
