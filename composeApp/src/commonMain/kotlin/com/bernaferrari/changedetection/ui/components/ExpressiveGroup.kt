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

fun segmentShape(position: SegmentPosition): Shape = when (position) {
    SegmentPosition.Only -> RoundedCornerShape(24.dp)
    SegmentPosition.First -> RoundedCornerShape(24.dp, 24.dp, 7.dp, 7.dp)
    SegmentPosition.Middle -> RoundedCornerShape(7.dp)
    SegmentPosition.Last -> RoundedCornerShape(7.dp, 7.dp, 24.dp, 24.dp)
}
