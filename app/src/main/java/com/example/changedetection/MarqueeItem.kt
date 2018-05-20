package com.example.changedetection

import android.view.View
import com.xwray.groupie.kotlinandroidextensions.Item
import com.xwray.groupie.kotlinandroidextensions.ViewHolder
import kotlinx.android.synthetic.main.marquee.*

class MarqueeItem(
    val title: String,
    val subtitle: String? = null
) : Item() {

    override fun getSpanSize(spanCount: Int, position: Int): Int = spanCount

    override fun getLayout() = R.layout.marquee

    override fun bind(viewHolder: ViewHolder, position: Int) {
        viewHolder.titleView.text = title
        if (subtitle != null) {
            viewHolder.subtitleView.visibility = View.VISIBLE
            viewHolder.subtitleView.text = subtitle
        } else {
            viewHolder.subtitleView.visibility = View.GONE
            val a = 1 + 1
            val b = a + 1
            val c = a
        }
    }
}
