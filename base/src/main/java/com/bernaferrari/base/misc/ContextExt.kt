package com.bernaferrari.base.misc

import android.content.Context
import android.content.Intent
import android.util.TypedValue
import android.widget.Toast
import android.widget.Toast.LENGTH_LONG
import androidx.annotation.AttrRes
import androidx.core.content.ContextCompat

var toast: Toast? = null

/** Calls [Context.getSystemService] and casts the return value to [T]. */
inline fun <reified T> Context.systemService(name: String): T {
    return getSystemService(name) as T
}

/** Shows a toast in the receiving context, cancelling any previous. */
fun Context.toast(message: Int) {
    toast?.cancel()
    toast = Toast.makeText(this, message, LENGTH_LONG)
        .apply {
            show()
        }
}

/** Shows a toast in the receiving context, cancelling any previous. */
fun Context.toast(message: String) {
    toast?.cancel()
    toast = Toast.makeText(this, message, LENGTH_LONG)
        .apply {
            show()
        }
}

/** Get color from attributes */
fun Context.getColorFromAttr(
    @AttrRes attrColor: Int,
    typedValue: TypedValue = TypedValue(),
    resolveRefs: Boolean = true
): Int {
    theme.resolveAttribute(attrColor, typedValue, resolveRefs)
    return typedValue.data
}

/** Open url in standard browser */
fun Context.openInBrowser(url: String?) {
    if (url != null) {
        this.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
    }
}

fun Context.getColorCompat(color: Int): Int = ContextCompat.getColor(this, color)
