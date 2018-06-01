package com.bernaferrari.changedetection.ui

import android.content.Context
import android.support.annotation.StringRes
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import com.bernaferrari.changedetection.R
import kotlinx.android.synthetic.main.empty_layout.view.*

/**
Created by bernardoferrari on 09/11/17.
Inspired on FastHub implementation.
TODO need to change some variable names and update some methods
 */
class StateLayout : FrameLayout {
    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

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
