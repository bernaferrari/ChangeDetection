package com.bernaferrari.changedetection.extensions

import android.widget.ImageView
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat

fun ImageView.setImageDrawableIfNull(drawableId: Int) {
    val animDrawable = AnimatedVectorDrawableCompat.create(context, drawableId)

    if (this.drawable == null) {
        this.setImageDrawable(animDrawable)
    }
}