package com.bernaferrari.changedetection.extensions

import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.support.annotation.StringRes
import android.support.graphics.drawable.AnimatedVectorDrawableCompat
import android.support.v4.app.Fragment
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.SimpleItemAnimator
import android.view.View
import android.widget.ImageView
import androidx.core.net.toUri
import com.github.marlonlom.utilities.timeago.TimeAgo

internal typealias ColorGroup = Pair<Int, Int>

internal fun Long.convertTimestampToDate(): String = TimeAgo.using(this)

internal fun View.getText(@StringRes res: Int) = this.resources.getText(res)

internal operator fun Boolean.inc() = !this

internal fun ImageView.setAndStartAnimation(res: Int, context: Context) {
    this.setImageDrawable(AnimatedVectorDrawableCompat.create(context, res))
    (this.drawable as AnimatedVectorDrawableCompat).start()
}

// this will scroll to wanted index + or - one, giving a margin of one and allowing user to
// keep tapping in the same place and RecyclerView keep scrolling.
internal fun RecyclerView.scrollToIndexWithMargins(
    previousIndex: Int,
    index: Int,
    size: Int
) {
    if (previousIndex == -1) {
        // This will run on first iteration
        when (index) {
            in 1 until (size - 1) -> this.scrollToPosition(index - 1)
            (size - 1) -> this.scrollToPosition(index)
            else -> this.scrollToPosition(0)
        }
    } else {
        when (index) {
            in 1 until previousIndex -> this.scrollToPosition(index - 1)
            in previousIndex until (size - 1) -> this.scrollToPosition(index + 1)
            else -> this.scrollToPosition(index)
        }
    }
}

internal fun Int.toDp(resources: Resources): Int {
    return (resources.displayMetrics.density * this).toInt()
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

internal fun Context.openInBrowser(url: String?) {
    if (url != null) {
        this.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
    }
}

internal fun Fragment.getStringFromArguments(key: String, default: String = ""): String =
    arguments?.getString(key) ?: default

/** Convenience for callbacks/listeners whose return value indicates an event was consumed. */
inline fun consume(f: () -> Unit): Boolean {
    f()
    return true
}

internal fun RecyclerView.itemAnimatorWithoutChangeAnimations() =
    this.itemAnimator.apply {
        // From https://stackoverflow.com/a/33302517/4418073
        if (this is SimpleItemAnimator) {
            this.supportsChangeAnimations = false
        }
    }
