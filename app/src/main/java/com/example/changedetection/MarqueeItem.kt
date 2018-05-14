package com.example.changedetection

import android.os.Build
import android.support.v4.content.ContextCompat
import android.support.v7.widget.CardView
import android.view.View
import com.mikepenz.google_material_typeface_library.GoogleMaterial
import com.mikepenz.iconics.IconicsDrawable
import com.example.changedetection.R
import com.example.changedetection.data.Task
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.orhanobut.logger.Logger
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
        if (subtitle != null){
            viewHolder.subtitleView.visibility = View.VISIBLE
            viewHolder.subtitleView.text = subtitle
        } else {
            viewHolder.subtitleView.visibility = View.GONE
            val a = 1+1
            val b = a+1
            val c = a
        }
    }
}
