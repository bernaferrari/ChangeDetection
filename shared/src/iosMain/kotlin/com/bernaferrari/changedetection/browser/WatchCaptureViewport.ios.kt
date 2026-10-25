package com.bernaferrari.changedetection.browser

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
internal actual fun watchCaptureViewportModifier(): Modifier {
    val density = LocalDensity.current
    val nativeWidth = with(density) { 390.dp.roundToPx() }
    val nativeHeight = with(density) { 844.dp.roundToPx() }
    return Modifier.layout { measurable, constraints ->
        val width = if (constraints.hasBoundedWidth) constraints.maxWidth else nativeWidth
        val height = if (constraints.hasBoundedHeight) constraints.maxHeight else nativeHeight
        val child = measurable.measure(Constraints.fixed(nativeWidth, nativeHeight))
        val scale = minOf(width.toFloat() / nativeWidth, height.toFloat() / nativeHeight).coerceAtLeast(.01f)
        layout(width, height) {
            child.placeWithLayer(((width - nativeWidth * scale) / 2).roundToInt(), 0) {
                scaleX = scale;scaleY = scale;transformOrigin = TransformOrigin(0f, 0f)
            }
        }
    }
}
