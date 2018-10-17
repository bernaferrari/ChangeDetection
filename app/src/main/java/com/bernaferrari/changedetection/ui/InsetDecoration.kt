package com.bernaferrari.changedetection.ui

import android.graphics.Canvas
import android.graphics.Paint
import androidx.annotation.ColorInt
import androidx.annotation.Dimension
import androidx.recyclerview.widget.RecyclerView


/**
 * A decoration which draws a horizontal divider between [RecyclerView.ViewHolder]s of a given
 * type; with a left inset.
 * this class was adapted from Plaid
 */
class InsetDecoration(
    @param:Dimension private val height: Int,
    @param:Dimension private val inset: Int,
    @ColorInt dividerColor: Int
) : androidx.recyclerview.widget.RecyclerView.ItemDecoration() {

    private val paint: Paint = Paint()

    init {
        paint.color = dividerColor
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = height.toFloat()
    }

    override fun onDrawOver(
        canvas: Canvas,
        parent: androidx.recyclerview.widget.RecyclerView,
        state: androidx.recyclerview.widget.RecyclerView.State
    ) {
        val childCount = parent.childCount
        if (childCount < 2) return

        val lm = parent.layoutManager ?: return
        val lines = FloatArray(childCount * 4)

        for (i in 0 until childCount) {
            val child = parent.getChildAt(i)
            parent.getChildViewHolder(child)

            if (child.isActivated || i + 1 < childCount && parent.getChildAt(i + 1).isActivated) {
                continue
            }

            lines[i * 4] = (inset + lm.getDecoratedLeft(child)).toFloat()
            lines[i * 4 + 2] = lm.getDecoratedRight(child).toFloat()
            val y = lm.getDecoratedBottom(child) + child.translationY.toInt() + height
            lines[i * 4 + 1] = y.toFloat()
            lines[i * 4 + 3] = y.toFloat()
        }

        canvas.drawLines(lines, paint)
    }
}
