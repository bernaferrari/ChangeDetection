package com.bernaferrari.changedetection.util

import com.bernaferrari.changedetection.R

internal object VisibilityHelper {
    internal fun getStaticIcon(isOn: Boolean): Int {
        return if (isOn) {
            R.drawable.visibility_on
        } else {
            R.drawable.visibility_off
        }
    }

    internal fun getAnimatedIcon(isOn: Boolean): Int {
        return if (isOn) {
            R.drawable.visibility_off_to_on
        } else {
            R.drawable.visibility_on_to_off
        }
    }
}
