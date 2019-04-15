package com.bernaferrari.changedetection.core

import android.view.View
import android.widget.ImageView
import androidx.core.view.isVisible
import androidx.databinding.BindingAdapter
import com.bernaferrari.changedetection.extensions.toDp
import com.bernaferrari.changedetection.util.GradientColors

@BindingAdapter("isItemVisible")
fun isItemVisible(view: View, isVisible: Boolean) {
    view.isVisible = isVisible
}

@BindingAdapter("srcRes")
fun imageViewSrcRes(view: ImageView, drawableRes: Int) {
    if (drawableRes != 0) {
        view.setImageResource(drawableRes)
    } else {
        view.setImageDrawable(null)
    }
}

@BindingAdapter("colorFirst", "colorSecond")
fun viewBgGradient(view: View, colorFirst: Int, colorSecond: Int) {
    view.background = GradientColors.getGradientDrawable(Pair(colorFirst, colorSecond)).apply {
        cornerRadius = 8.toDp(view.resources).toFloat()
    }
}
