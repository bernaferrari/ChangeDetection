package com.example.changedetection

import android.graphics.drawable.Drawable
import android.support.graphics.drawable.Animatable2Compat

fun Animatable2Compat.onAnimationStart(action: (seekBar: Drawable) -> Unit): Animatable2Compat.AnimationCallback =
    setOnAnimationChangeListener(onAnimationStart = action)

fun Animatable2Compat.onAnimationEnd(action: (seekBar: Drawable) -> Unit): Animatable2Compat.AnimationCallback =
    setOnAnimationChangeListener(onAnimationEnd = action)

fun Animatable2Compat.setOnAnimationChangeListener(
    onAnimationStart: ((action: Drawable) -> Unit)? = null,
    onAnimationEnd: ((action: Drawable) -> Unit)? = null
): Animatable2Compat.AnimationCallback {
    val listener = object : Animatable2Compat.AnimationCallback() {

        override fun onAnimationEnd(drawable: Drawable) {
            onAnimationEnd?.invoke(drawable)
        }

        override fun onAnimationStart(drawable: Drawable) {
            onAnimationStart?.invoke(drawable)
        }
    }

    registerAnimationCallback(listener)
    return listener
}