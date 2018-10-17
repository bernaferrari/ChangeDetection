package com.bernaferrari.changedetection.ui

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import androidx.annotation.StringRes
import com.bernaferrari.changedetection.R
import kotlinx.android.synthetic.main.empty_layout.view.*

class EmptyLayout @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null,
    defStyleAttr: Int = 0, defStyleRes: Int = 0
) : FrameLayout(context, attrs, defStyleAttr, defStyleRes) {

    fun showLoading() {
        visibility = View.VISIBLE
        cardView.visibility = View.GONE
        loading_indicator.visibility = View.VISIBLE
    }

    fun setEmptyText(@StringRes resId: Int) {
        setEmptyText(resources.getString(resId))
    }

    fun setEmptyText(text: String) {
        textWhenEmpty.text = text
    }

    fun showEmptyState() {
        if (loading_indicator.visibility != View.VISIBLE) {
            return
        }

        visibility = View.VISIBLE
        cardView.visibility = View.VISIBLE
        textWhenEmpty.visibility = View.VISIBLE
        loading_indicator.visibility = View.GONE
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        View.inflate(context, R.layout.empty_layout, this)
    }
}
