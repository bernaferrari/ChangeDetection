package com.bernaferrari.base.misc

import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat


internal operator fun Boolean.inc() = !this

internal fun ImageView.setAndStartAnimation(res: Int) {
    this.setImageDrawable(AnimatedVectorDrawableCompat.create(this.context, res))
    (this.drawable as AnimatedVectorDrawableCompat).start()
}

/**
 * For Fragments, allows declarations like
 * ```
 * val myViewModel = viewModelProvider(myViewModelFactory)
 * ```
 */
inline fun <reified VM : ViewModel> Fragment.viewModelProvider(
    provider: ViewModelProvider.Factory
) = ViewModelProviders.of(this, provider).get(VM::class.java)

internal fun Fragment.getStringFromArguments(
    key: String,
    default: String = ""
): String = arguments?.getString(key) ?: default

/** Convenience for callbacks/listeners whose return value indicates an event was consumed. */
inline fun consume(f: () -> Unit): Boolean {
    f()
    return true
}

/** Convenience for try/catch where the exception is ignored. */
inline fun trySilently(f: () -> Unit) {
    try {
        f()
    } catch (e: Exception) {

    }
}
