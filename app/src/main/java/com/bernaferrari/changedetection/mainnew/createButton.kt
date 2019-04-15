package com.bernaferrari.changedetection.mainnew

import android.view.View
import android.widget.FrameLayout
import com.bernaferrari.base.view.inflate
import com.bernaferrari.changedetection.R

fun FrameLayout.inflateAddButton(): View {
    return this.inflate(R.layout.add_button, false).also {
        this.addView(it)
    }
}