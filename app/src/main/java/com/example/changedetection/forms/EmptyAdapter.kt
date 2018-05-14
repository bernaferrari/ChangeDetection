package com.example.changedetection.forms

import android.support.v7.widget.RecyclerView
import android.view.View
import com.example.changedetection.R
import com.mikepenz.fastadapter.items.AbstractItem

internal open class EmptyAdapter : AbstractItem<EmptyAdapter, RecyclerView.ViewHolder>() {
    open val kind = 0
    open val isSection = false

    override fun getType(): Int = kind

    override fun getViewHolder(v: View): RecyclerView.ViewHolder = ViewHolderSeparator(v)

    override fun getLayoutRes(): Int = R.layout.rowdrigo_section

    internal class ViewHolderSeparator constructor(v: View) : RecyclerView.ViewHolder(v)
}