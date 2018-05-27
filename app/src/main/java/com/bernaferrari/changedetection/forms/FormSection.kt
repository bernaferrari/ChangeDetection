package com.bernaferrari.changedetection.forms

import android.support.v7.widget.RecyclerView
import com.bernaferrari.changedetection.R
import com.xwray.groupie.kotlinandroidextensions.Item
import com.xwray.groupie.kotlinandroidextensions.ViewHolder
import kotlinx.android.synthetic.main.item_section.*

internal class FormSection(
    val nome: String,
    val isCompact: Boolean = false
) : Item() {

    var visibleHolder: RecyclerView.ViewHolder? = null

    override fun getLayout(): Int = R.layout.item_section

    override fun bind(holder: ViewHolder, position: Int) {
        visibleHolder = holder

        holder.title.text = nome

        if (isCompact) {
            holder.itemView.minimumHeight = 0
        }
    }
}