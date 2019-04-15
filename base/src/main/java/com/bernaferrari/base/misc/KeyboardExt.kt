package com.bernaferrari.base.misc

import android.app.Activity
import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager

/** Show the keyboard and focus on the view. */
fun View.showKeyboardOnView() {
    this.requestFocus()
    val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
    imm?.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, 0)
}

/** Hide the keyboard. */
fun Activity.hideKeyboard() {
    val imm = this.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager

    // check if no view has focus:
    val currentFocusedView = this.currentFocus
    if (currentFocusedView != null) {
        imm?.hideSoftInputFromWindow(
            currentFocusedView.windowToken,
            InputMethodManager.HIDE_NOT_ALWAYS
        )
    }
}
