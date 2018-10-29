package com.bernaferrari.changedetection.extensions

import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.annotation.AttrRes
import androidx.annotation.LayoutRes
import androidx.annotation.StringRes
import androidx.core.graphics.drawable.toDrawable
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat
import com.bernaferrari.changedetection.R
import com.github.marlonlom.utilities.timeago.TimeAgo
import com.google.android.material.bottomsheet.BottomSheetDialog

internal typealias ColorGroup = Pair<Int, Int>

internal fun Long.convertTimestampToDate(): String = TimeAgo.using(this)

internal fun View.getText(@StringRes res: Int) = this.resources.getText(res)

internal operator fun Boolean.inc() = !this

internal fun ImageView.setAndStartAnimation(res: Int) {
    this.setImageDrawable(AnimatedVectorDrawableCompat.create(this.context, res))
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

internal fun Fragment.getStringFromArguments(
    key: String,
    default: String = ""
): String =
    arguments?.getString(key) ?: default

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

internal fun RecyclerView.itemAnimatorWithoutChangeAnimations() =
    this.itemAnimator.apply {
        // From https://stackoverflow.com/a/33302517/4418073
        if (this is SimpleItemAnimator) {
            this.supportsChangeAnimations = false
        }
    }

internal fun Context.getColorFromAttr(
    @AttrRes attrColor: Int,
    typedValue: TypedValue = TypedValue(),
    resolveRefs: Boolean = true
): Int {
    theme.resolveAttribute(attrColor, typedValue, resolveRefs)
    return typedValue.data
}

internal fun View.createBottomSheet() =
    BottomSheetDialog(this.context).also {
        this.background = context.getColorFromAttr(R.attr.windowBackground).toDrawable()
        it.setContentView(this)
    }

internal fun View.createAndShowBottomSheet() =
    BottomSheetDialog(this.context).also {
        this.background = context.getColorFromAttr(R.attr.windowBackground).toDrawable()
        it.setContentView(this)
        it.show()
    }


/**
 * Allows calls like
 *
 * `viewGroup.inflate(R.layout.foo)`
 */
fun ViewGroup.inflate(@LayoutRes layout: Int, attachToRoot: Boolean = false): View {
    return LayoutInflater.from(context).inflate(layout, this, attachToRoot)
}
