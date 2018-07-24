package com.bernaferrari.changedetection.ui

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator

class ColorPickerItem @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null,
    defStyleAttr: Int = 0, defStyleRes: Int = 0
) : View(context, attrs, defStyleAttr, defStyleRes) {

    var progress: Float = 0f
    var circlePaint: Paint? = null
    var outlinePaint: Paint? = null
    var colors = Pair(Color.WHITE, Color.WHITE)

    fun updateColor() {
        // If the value is set here, it risks getting a solid color if width is blue.
        // This way, it will be refreshed on onDraw.
        circlePaint = null
        outlinePaint = null
        invalidate()
    }

    fun areColorsSet(): Boolean = circlePaint != null && outlinePaint != null

    init {
        setLayerType(1, null)
    }

    fun setOutlineColor(i: Int) {
        outlinePaint?.color = i
        invalidate()
    }

    private fun dpToPixels(i: Int): Float {
        return TypedValue.applyDimension(
            1,
            i.toFloat(),
            context.resources.displayMetrics
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (circlePaint == null) {
            circlePaint = createPaintInside()
        }

        if (outlinePaint == null) {
            outlinePaint = createPaintOutside()
        }

        val min = Math.min(
            width,
            height
        ).toFloat() / 2.0f - dpToPixels(6) * this.progress
        canvas.drawCircle(
            width.toFloat() / 2.0f,
            height.toFloat() / 2.0f,
            (Math.min(
                width,
                height
            ).toFloat() / 2.0f - dpToPixels(2)) * this.progress + 0.0f * (1.toFloat() - this.progress),
            outlinePaint
        )

        canvas.drawCircle(
            width.toFloat() / 2.0f,
            height.toFloat() / 2.0f,
            min,
            circlePaint
        )
    }

    fun reverseSelection() {
        if (this.isSelected) {
            deselectIfSelected(true)
        } else {
            selectIfDeselected(true)
        }
    }

    fun selectIfDeselected(animated: Boolean) {
        if (!this.isSelected) {
            startAnimation(0.0f, 1.0f, animated)
            this.isSelected = true
        }
    }

    fun deselectIfSelected(animated: Boolean) {
        if (this.isSelected) {
            startAnimation(1.0f, 0.0f, animated)
            this.isSelected = false
        }
    }

    private fun startAnimation(f: Float, f2: Float, z: Boolean) {
        if (z) {
            val ofFloat = ObjectAnimator.ofFloat(f, f2)
            ofFloat.duration = 250
            ofFloat.interpolator = AccelerateDecelerateInterpolator()
            ofFloat.addUpdateListener(updateListener())
            ofFloat.start()
            return
        }
        this.progress = f2
        invalidate()
    }

    private fun updateListener(): ValueAnimator.AnimatorUpdateListener =
        ValueAnimator.AnimatorUpdateListener { valueAnimator2 ->
            val valueAnimator = valueAnimator2?.animatedValue ?: throw NullPointerException()
            this.progress = (valueAnimator as Float).toFloat()
            this.invalidate()
        }

    private fun createPaintInside(): Paint {
        val paint = Paint()
        paint.isAntiAlias = true
        paint.style = Paint.Style.FILL
        paint.shader = LinearGradient(
            width.toFloat(),
            0f,
            0f,
            height.toFloat(),
            colors.first,
            colors.second,
            Shader.TileMode.MIRROR
        )
        return paint
    }

    private fun createPaintOutside(): Paint {
        val paint = Paint()
        paint.isAntiAlias = true
        paint.color = Color.WHITE
        paint.shader = LinearGradient(
            width.toFloat(), 0f, 0f, height.toFloat(),
            colors.first,
            colors.second,
            Shader.TileMode.CLAMP
        )
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = dpToPixels(3)

        return paint
    }
}