/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.ui.theme

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Change Detection's “change pulse” mark.
 *
 * A nearly complete scan ring wraps a small pulse. The open ring reads as an ongoing watch while
 * the pulse is the moment something changes. It stays legible at launcher, splash, and about sizes.
 */
@Composable
fun ChangeDetectionBrandMark(
    modifier: Modifier = Modifier.size(48.dp),
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    signalColor: Color = MaterialTheme.colorScheme.secondary,
    animated: Boolean = false,
) {
    val scannerAngle = if (animated) {
        val scanner = rememberInfiniteTransition(label = "brandScanner")
        val angle by scanner.animateFloat(
            initialValue = 42f,
            targetValue = 402f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 2400, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "brandScannerAngle",
        )
        angle
    } else {
        318f
    }
    Surface(
        modifier = modifier,
        shape = BrandMarkShape,
        color = containerColor,
        contentColor = contentColor,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val edge = size.minDimension
                val origin = Offset((size.width - edge) / 2f, (size.height - edge) / 2f)
                val ringInset = edge * 0.21f
                val ringSize = edge - ringInset * 2f

                drawArc(
                    color = contentColor,
                    startAngle = 42f,
                    sweepAngle = 276f,
                    useCenter = false,
                    topLeft = origin + Offset(ringInset, ringInset),
                    size = Size(ringSize, ringSize),
                    style = Stroke(width = edge * 0.075f, cap = StrokeCap.Round),
                )

                val pulse = Path().apply {
                    moveTo(origin.x + edge * 0.27f, origin.y + edge * 0.52f)
                    lineTo(origin.x + edge * 0.41f, origin.y + edge * 0.52f)
                    lineTo(origin.x + edge * 0.48f, origin.y + edge * 0.38f)
                    lineTo(origin.x + edge * 0.57f, origin.y + edge * 0.64f)
                    lineTo(origin.x + edge * 0.65f, origin.y + edge * 0.48f)
                    lineTo(origin.x + edge * 0.73f, origin.y + edge * 0.48f)
                }
                drawPath(
                    path = pulse,
                    color = contentColor,
                    style = Stroke(
                        width = edge * 0.065f,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round,
                    ),
                )
                val angle = scannerAngle * PI / 180.0
                val ringRadius = ringSize / 2f
                val ringCenter = origin + Offset(edge / 2f, edge / 2f)
                drawCircle(
                    color = signalColor,
                    radius = edge * 0.07f,
                    center = ringCenter + Offset(
                        x = cos(angle).toFloat() * ringRadius,
                        y = sin(angle).toFloat() * ringRadius,
                    ),
                )
            }
        }
    }
}

val BrandMarkShape = RoundedCornerShape(28)
