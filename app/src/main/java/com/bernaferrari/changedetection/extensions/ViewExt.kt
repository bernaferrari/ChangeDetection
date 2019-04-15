package com.bernaferrari.changedetection.extensions

import android.text.Editable
import android.view.View
import android.view.animation.AnimationUtils
import com.bernaferrari.changedetection.R

fun View.shakeIt() {
    // inspired by Hurry from Sam Ruston
    this.startAnimation(AnimationUtils.loadAnimation(this.context, R.anim.shake))
}

fun String.toEditText(): Editable = Editable.Factory.getInstance().newEditable(this)
