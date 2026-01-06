package com.bernaferrari.changedetection.ui.comparison

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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.bernaferrari.changedetection.shared.generated.resources.Res
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CompareArrows

import org.jetbrains.compose.resources.painterResource
import kotlin.math.roundToInt
// Custom Shape for clipping
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.geometry.Rect
import com.multiplatform.webview.web.WebView
import com.multiplatform.webview.web.rememberWebViewStateWithHTMLData

@Composable
fun VisualDiffView(
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
                    imageVector = Icons.Default.CompareArrows,
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
    val state = rememberWebViewStateWithHTMLData(
        data = content
    )
    
    WebView(
        state = state,
        modifier = modifier
    )
}

class HorizontalSplitShape(private val splitX: Float) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
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
