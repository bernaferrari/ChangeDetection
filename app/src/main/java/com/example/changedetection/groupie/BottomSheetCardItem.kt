package com.example.changedetection.groupie

import android.graphics.drawable.Animatable
import android.support.v4.content.ContextCompat
import android.view.View
import com.mikepenz.iconics.IconicsDrawable
import com.example.changedetection.R
import com.example.changedetection.data.Diff
import com.example.changedetection.data.Task
import com.github.marlonlom.utilities.timeago.TimeAgo
import com.github.marlonlom.utilities.timeago.TimeAgoMessages
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.xwray.groupie.kotlinandroidextensions.Item
import com.xwray.groupie.kotlinandroidextensions.ViewHolder
import kotlinx.android.synthetic.main.bottomsheet_item_card_list.*
import java.text.DecimalFormat
import java.util.*

class BottomSheetCardItem(
    val tas: Task,
    val lastDiff: Diff?
) : Item() {
    var status = 0

    val title = if (tas.title.isNullOrBlank()){
        tas.url.replaceFirst("^(http[s]?://www\\.|http[s]?://|www\\.)".toRegex(),"")
    } else {
        tas.title ?: ""
    }

    override fun getSpanSize(spanCount: Int, position: Int) = 1

    override fun getLayout() = R.layout.bottomsheet_item_card_list

    override fun bind(holder: ViewHolder, position: Int, payloads: List<Any>) {
        bind(holder, position)
        bindHeart(holder)
        bindDiff(holder, payloads.firstOrNull() as? Diff ?: lastDiff)
    }

    private fun bindHeart(holder: ViewHolder) {
        val context = holder.containerView.context

        when (status){
            1 -> {
                holder.progressBar.visibility = View.VISIBLE
                holder.imageView.visibility = View.GONE
            }
            0 -> {
                holder.imageView.setImageDrawable(IconicsDrawable(context).icon(CommunityMaterial.Icon.cmd_check).sizeDp(24).color(ContextCompat.getColor(context, R.color.md_green_500)))
            }
            2 -> {
                holder.imageView.setImageDrawable(IconicsDrawable(context).icon(CommunityMaterial.Icon.cmd_alert_circle).sizeDp(24).color(ContextCompat.getColor(context, R.color.md_red_500)))
            }
        }
    }

    private fun bindDiff(holder: ViewHolder, diff: Diff?) {
        if (diff == null){
            return
        }

        val messages = TimeAgoMessages.Builder().withLocale(Locale.getDefault()).build()
        val text = TimeAgo.using(diff.timestamp, messages)
        holder.subtitleTextView.text = "${readableFileSize(diff.value.count())} – ${text}"
    }

    override fun bind(viewHolder: ViewHolder, position: Int) {

        viewHolder.titleTextView.text = title

        viewHolder.progressBar.visibility = View.GONE
        viewHolder.imageView.visibility = View.VISIBLE

        val context = viewHolder.containerView.context

        viewHolder.imageView.setImageDrawable(IconicsDrawable(context).icon(CommunityMaterial.Icon.cmd_exclamation).sizeDp(24).color(ContextCompat.getColor(context, R.color.md_yellow_500)))
    }

    private fun readableFileSize(size: Int): String {
        if (size <= 0) return "0"
        val units = arrayOf("B", "kB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
        return DecimalFormat("#,##0.#").format(
            size / Math.pow(
                1024.0,
                digitGroups.toDouble()
            )
        ) + " " + units[digitGroups]
    }
}
