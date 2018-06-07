package com.bernaferrari.changedetection.groupie

import android.support.v4.content.ContextCompat
import android.view.View
import com.bernaferrari.changedetection.R
import com.bernaferrari.changedetection.data.MinimalSnap
import com.bernaferrari.changedetection.extensions.convertTimestampToDate
import com.bernaferrari.changedetection.extensions.readableFileSize
import com.xwray.groupie.kotlinandroidextensions.Item
import com.xwray.groupie.kotlinandroidextensions.ViewHolder
import kotlinx.android.synthetic.main.item_row.*

class RowItem(
    val minimalSnap: MinimalSnap,
    var isSelected: Boolean = false
) : Item() {

    override fun getLayout() = R.layout.item_row

    override fun unbind(holder: ViewHolder) {
        holder.counter.visibility = View.GONE
        super.unbind(holder)
    }

    override fun bind(viewHolder: ViewHolder, position: Int) {

        viewHolder.run {
            title.text = minimalSnap.timestamp.convertTimestampToDate()
            subtitle.text = minimalSnap.contentSize.readableFileSize()

            if (isSelected) {
                subtitle.setTextColor(
                    ContextCompat.getColor(
                        viewHolder.containerView.context,
                        R.color.md_blue_A200
                    )
                )

                title.setTextColor(
                    ContextCompat.getColor(
                        viewHolder.containerView.context,
                        R.color.md_blue_A200
                    )
                )

                counter.visibility = View.VISIBLE
            } else {
                subtitle.setTextColor(
                    ContextCompat.getColor(
                        viewHolder.containerView.context,
                        R.color.FontWeak
                    )
                )

                title.setTextColor(
                    ContextCompat.getColor(
                        viewHolder.containerView.context,
                        R.color.FontStrong
                    )
                )
            }
        }
    }
}
