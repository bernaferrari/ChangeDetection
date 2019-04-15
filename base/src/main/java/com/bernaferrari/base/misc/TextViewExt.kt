package com.bernaferrari.base.misc

import android.text.Editable
import android.text.TextWatcher
import android.widget.TextView

/** Detect when text has changed. */
inline fun TextView.onTextChanged(crossinline body: (text: CharSequence) -> Unit): TextWatcher {
    val watcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) = body(s)
        override fun afterTextChanged(s: Editable?) = Unit
    }
    addTextChangedListener(watcher)
    return watcher
}


/** Detect the text input when user is typing. */
inline fun TextView.onEditorAction(crossinline body: (actionId: Int) -> Boolean) {
    setOnEditorActionListener { _, actionId, _ -> body(actionId) }
}
