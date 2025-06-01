package com.biblialibras.android.common.extensions

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
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.RecyclerView
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat
import com.biblialibras.android.common.misc.toUri

/**
 * provides a String representation of the given time
 * @return `millis` in hh:mm:ss format
 */
fun Long.formatTime(): String {
    return when {
        this % 86400 / 3600 > 0 -> String.format(
            "%02d:%02d:%02d",
            this % 86400 / 3600,
            this % 3600 / 60,
            this % 60
        )
        this % 3600 / 60 > 0 -> String.format("%d:%02d", this % 3600 / 60, this % 60)
        else -> String.format("0:%02d", this % 60)
    }
}

internal typealias ColorGroup = Pair<Int, Int>

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

internal fun Context.getColorFromAttr(
    @AttrRes attrColor: Int,
    typedValue: TypedValue = TypedValue(),
    resolveRefs: Boolean = true
): Int {
    theme.resolveAttribute(attrColor, typedValue, resolveRefs)
    return typedValue.data
}


/**
 * Allows calls like
 *
 * `viewGroup.inflate(R.layout.foo)`
 */
fun ViewGroup.inflate(@LayoutRes layout: Int, attachToRoot: Boolean = false): View {
    return LayoutInflater.from(context).inflate(layout, this, attachToRoot)
}

fun generateTitle(appendText: String, rangeStart: Int, rangeEnd: Int): String {

    val verseStart = rangeStart % 1000
    val chapterStart = (rangeStart / 1000) % 1000

    val verseEnd = rangeEnd % 1000
    val chapterEnd = (rangeEnd / 1000) % 1000

    return if (chapterStart == chapterEnd) {
        "$appendText $chapterStart:$verseStart-$verseEnd"
    } else {
        "$appendText $chapterStart:$verseStart-$chapterEnd:$verseEnd"
    }
}

inline fun RecyclerView.onScroll(crossinline body: (dx: Int, dy: Int) -> Unit) {
    addOnScrollListener(object : RecyclerView.OnScrollListener() {
        override fun onScrolled(
            recyclerView: RecyclerView,
            dx: Int,
            dy: Int
        ) {
            body(dx, dy)
        }
    })
}