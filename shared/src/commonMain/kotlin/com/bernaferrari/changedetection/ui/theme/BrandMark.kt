package com.bernaferrari.changedetection.ui.theme

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** The same scanned-page and highlighted-change mark used by the platform icons. */
@Composable
fun ChangeDetectionBrandMark(
    modifier: Modifier = Modifier.size(48.dp),
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    signalColor: Color = MaterialTheme.colorScheme.secondary,
    animated: Boolean = false,
) {
    val signalAlpha = if (animated) {
        val transition = rememberInfiniteTransition(label = "changeSignal")
        val alpha by transition.animateFloat(
            initialValue = 0.45f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse),
            label = "changeSignalAlpha",
        )
        alpha
    } else {
        1f
    }
    Surface(modifier = modifier, shape = BrandMarkShape, color = containerColor) {
        Box {
            val artwork = Modifier.fillMaxSize().padding(6.dp)
            Icon(BrandArt.Mark, null, artwork, tint = contentColor)
            Icon(BrandArt.Change, null, artwork, tint = signalColor.copy(alpha = signalAlpha))
        }
    }
}

val BrandMarkShape = RoundedCornerShape(28)
