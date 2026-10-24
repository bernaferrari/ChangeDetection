package com.bernaferrari.changedetection.browser

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.unit.Constraints
import kotlin.math.roundToInt

@Composable
internal actual fun watchCaptureViewportModifier(): Modifier = Modifier.layout { measurable, constraints ->
    val width = if (constraints.hasBoundedWidth) constraints.maxWidth else WatchCaptureViewport.WIDTH
    val height = if (constraints.hasBoundedHeight) constraints.maxHeight else WatchCaptureViewport.HEIGHT
    val child = measurable.measure(Constraints.fixed(WatchCaptureViewport.WIDTH, WatchCaptureViewport.HEIGHT))
    val scale = minOf(width.toFloat() / child.width, height.toFloat() / child.height).coerceAtLeast(.01f)
    layout(width, height) {
        child.placeWithLayer(((width - child.width * scale) / 2).roundToInt(), 0) {
            scaleX = scale
            scaleY = scale
            transformOrigin = TransformOrigin(0f, 0f)
        }
    }
}
