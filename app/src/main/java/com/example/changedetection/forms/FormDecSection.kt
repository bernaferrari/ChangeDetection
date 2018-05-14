package com.example.changedetection.forms

import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.TextView
import com.example.changedetection.R
import com.example.changedetection.forms.FormConstants.subtitlefalse
import com.example.changedetection.forms.FormConstants.subtitletrue
import kotlinx.android.synthetic.main.rowdrigo_section.view.*

internal class FormDecSection(
    val nome: String,
    override val kind: Int,
    val isCompact: Boolean = false,
    override val isSection: Boolean = true
) :
    EmptyAdapter() {

    var ismember: Boolean? = null

    override fun getType(): Int = if (isCompact) {
        FormConstants.isection + 50
    } else {
        FormConstants.isection
    }

    override fun getViewHolder(v: View) = ViewHolderSeparator(v)

    override fun getLayoutRes(): Int = R.layout.rowdrigo_section

    override fun unbindView(holder: RecyclerView.ViewHolder) {
        val holder = holder as ViewHolderSeparator
        holder.headerView.text = null
        holder.subtitle.text = null
        super.unbindView(holder)
    }

    override fun bindView(holder: RecyclerView.ViewHolder, payloads: List<Any>) {
        super.bindView(holder, payloads)

        val holder = holder as ViewHolderSeparator
        holder.headerView.text = nome

        if (isCompact) {
            holder.itemView.minimumHeight = 0
//            holder.itemView.setBackgroundColor(ContextCompat.getColor(holder.itemView.context, R.color.CARD))
        }

        val subtitle = when (ismember) {
            true -> subtitletrue[kind]
            false -> subtitlefalse[kind]
            null -> null
        }

        if (subtitle != null) {
            holder.subtitle.visibility = View.VISIBLE
            when (ismember) {
                true -> holder.subtitle.text = subtitle
                false -> holder.subtitle.text = subtitle
            }
        } else {
            holder.subtitle.visibility = View.GONE
        }
    }

    internal class ViewHolderSeparator constructor(v: View) : RecyclerView.ViewHolder(v) {
        internal var headerView: TextView = v.title
        internal var subtitle: TextView = v.subtitle
    }
}