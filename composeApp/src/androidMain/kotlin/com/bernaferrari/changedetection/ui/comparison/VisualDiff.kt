package com.bernaferrari.changedetection.ui.comparison

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.bernaferrari.changedetection.R
import kotlin.math.roundToInt
// Custom Shape for clipping
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.geometry.Rect

@Composable
actual fun VisualDiffView(
    oldContent: String,
    newContent: String,
    modifier: Modifier
) {
    BoxWithConstraints(modifier = modifier) {
        val maxWidth = constraints.maxWidth.toFloat()
        var dragOffset by remember { mutableFloatStateOf(maxWidth / 2f) }

        val draggableState = rememberDraggableState { delta ->
            dragOffset = (dragOffset + delta).coerceIn(0f, maxWidth)
        }

        // We use a custom clipper to reveal the top view (New Content)
        // The bottom view (Old Content) is always fully visible (but obscured by top)

        // Bottom: Old Content
        WebContent(
            content = oldContent,
            modifier = Modifier.fillMaxSize()
        )

        // Top: New Content (Clipped)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(HorizontalSplitShape(dragOffset))
        ) {
            WebContent(
                content = newContent,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Slider Handle
        Box(
            modifier = Modifier
                .offset { IntOffset(dragOffset.roundToInt(), 0) }
                .fillMaxHeight()
                .width(4.dp) // Thickness of the divider
                .background(MaterialTheme.colorScheme.primary)
                .draggable(
                    state = draggableState,
                    orientation = Orientation.Horizontal
                )
        )

        // Handle Thumb (Vertical Pill)
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        dragOffset.roundToInt() - 12.dp.toPx().toInt(),
                        0
                    )
                } // Center horizontally on the line
                .fillMaxHeight()
                .width(24.dp) // Touch target width
                .draggable(
                    state = draggableState,
                    orientation = Orientation.Horizontal
                ),
            contentAlignment = Alignment.Center
        ) {
            // The visible Pill
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(48.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.White)
                    .border(1.dp, Color.Black.copy(alpha = 0.1f), RoundedCornerShape(50))
            )
            // Arrows icon for affordance (optional, or just the pill)
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                    .border(2.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_diff),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
private fun WebContent(
    content: String,
    modifier: Modifier = Modifier
) {
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
                webViewClient = WebViewClient()
            }
        },
        update = { webView ->
            webView.loadDataWithBaseURL(null, content, "text/html", "UTF-8", null)
        },
        modifier = modifier
    )
}

class HorizontalSplitShape(private val splitX: Float) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        // We want to show the RIGHT side (New Content) or LEFT side?
        // Let's say New Content is ON TOP.
        // If we want "Before | After" (Left | Right)
        // Then Top View (New) should be clipped to start at splitX
        // So we show Old (Bottom) from 0..splitX, and New (Top) from splitX..Width

        // Wait, "Reveal" usually means Top layer covers Bottom layer. 
        // If we slide Right, we reveal MORE of the Bottom layer? Or MORE of the Top layer?
        // Standard: Left is Old, Right is New.
        // So Bottom Layer = Old. Top Layer = New.
        // We clip Top Layer to only show from splitX to Width.

        return Outline.Rectangle(
            Rect(
                left = splitX,
                top = 0f,
                right = size.width,
                bottom = size.height
            )
        )
    }
}
