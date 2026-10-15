package com.bernaferrari.changedetection.ui.comparison

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * The slider mode icon, drawn on Material's 24 dp grid with 2 dp rounded strokes: no stock icon shows a
 * before/after split with a handle.
 * Drawn in black and tinted by [androidx.compose.material3.Icon], like any Material icon.
 */
internal object ModeIcons {
    /** Before on one side, after on the other, and a handle on the divider to drag between them. */
    val Slider: ImageVector by lazy {
        icon("Slider") {
            stroke { roundRect(3f, 5f, 21f, 19f, 2.5f) }
            // The "before" half, tinted (not solid) so the handle on the divider stays distinct.
            fill(alpha = 0.3f) {
                moveTo(12f, 5f)
                lineTo(5.5f, 5f)
                arcTo(2.5f, 2.5f, 0f, false, false, 3f, 7.5f)
                lineTo(3f, 16.5f)
                arcTo(2.5f, 2.5f, 0f, false, false, 5.5f, 19f)
                lineTo(12f, 19f)
                close()
            }
            stroke { line(12f, 2.5f, 12f, 21.5f) }
            fill { roundRect(10f, 9f, 14f, 15f, 2f) }
        }
    }
}

private fun icon(name: String, block: ImageVector.Builder.() -> Unit): ImageVector = ImageVector.Builder(name = name, defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
    .apply(block)
    .build()

private fun ImageVector.Builder.stroke(pathBuilder: PathBuilder.() -> Unit) = path(
    stroke = SolidColor(Color.Black),
    strokeLineWidth = 2f,
    strokeLineCap = StrokeCap.Round,
    strokeLineJoin = StrokeJoin.Round,
    pathBuilder = pathBuilder,
)

private fun ImageVector.Builder.fill(alpha: Float = 1f, pathBuilder: PathBuilder.() -> Unit) = path(fill = SolidColor(Color.Black), fillAlpha = alpha, pathBuilder = pathBuilder)

private fun PathBuilder.line(x1: Float, y1: Float, x2: Float, y2: Float) {
    moveTo(x1, y1)
    lineTo(x2, y2)
}

private fun PathBuilder.roundRect(left: Float, top: Float, right: Float, bottom: Float, r: Float) {
    moveTo(left + r, top)
    lineTo(right - r, top)
    arcTo(r, r, 0f, false, true, right, top + r)
    lineTo(right, bottom - r)
    arcTo(r, r, 0f, false, true, right - r, bottom)
    lineTo(left + r, bottom)
    arcTo(r, r, 0f, false, true, left, bottom - r)
    lineTo(left, top + r)
    arcTo(r, r, 0f, false, true, left + r, top)
    close()
}
