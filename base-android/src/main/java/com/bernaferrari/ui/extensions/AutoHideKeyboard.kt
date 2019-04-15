package com.bernaferrari.ui.extensions

import android.content.Context
import android.os.Build
import android.view.KeyEvent
import android.view.ViewConfiguration
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.core.content.getSystemService
import androidx.recyclerview.widget.RecyclerView
import com.bernaferrari.base.misc.onEditorAction
import com.bernaferrari.base.view.onKey
import com.bernaferrari.base.view.onScroll

/**
 * Automatically hide KeyBoard when app is being scrolled down.
 */
fun hideKeyboardWhenNecessary(recyclerView: RecyclerView?, editText: EditText) {

    val inputMethodManager by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            editText.context?.getSystemService<InputMethodManager>()
        } else {
            editText.context?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        } ?: throw Exception("null activity. Can't bind inputMethodManager")
    }

    // hide keyboard when user scrolls
    val touchSlop = ViewConfiguration.get(editText.context).scaledTouchSlop
    var totalDy = 0

    recyclerView?.onScroll { _, dy ->
        if (dy > 0) {
            totalDy += dy
            if (totalDy >= touchSlop) {
                totalDy = 0
                inputMethodManager.hideKeyboard(editText)
            }
        }
    }

    // hide keyboard when user taps enter
    editText.onKey {
        if (it.keyCode == KeyEvent.KEYCODE_ENTER) {
            inputMethodManager.hideKeyboard(editText)
            true
        } else {
            false
        }
    }

    // hide keyboard when user taps to go
    editText.onEditorAction {
        if (it == EditorInfo.IME_ACTION_GO) {
            inputMethodManager.hideKeyboard(editText)
            true
        } else {
            false
        }
    }
}

private fun InputMethodManager.hideKeyboard(editText: EditText) {
    this.hideSoftInputFromWindow(editText.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)

    editText.isFocusable = false
    editText.isFocusableInTouchMode = true

    if (editText.text.isEmpty()) {
        // loose the focus when scrolling and the text is empty, this way the
        // cursor will be hidden.
        editText.isFocusable = false
        editText.isFocusableInTouchMode = true
    }
}