package com.example.changedetection.forms

import android.arch.lifecycle.MutableLiveData
import android.support.v7.widget.RecyclerView
import android.view.View
import com.example.changedetection.R
import kotlinx.android.synthetic.main.eureka_single_yes_or_no.view.*

internal class FormDecIsMember(
    override val kind: Int,
    val ismemberinit: Boolean,
    val live: MutableLiveData<Boolean>
) :
    EmptyAdapter() {
    var ismemberinternal = ismemberinit
    var visibleHolder: RecyclerView.ViewHolder? = null

    override fun getType(): Int = kind

    override fun getViewHolder(v: View) = ViewHolderYesOrNo(v)

    override fun getLayoutRes(): Int = R.layout.eureka_single_yes_or_no

    override fun detachFromWindow(holder: RecyclerView.ViewHolder) {
        if (holder is ViewHolderYesOrNo) {
            ismemberinternal = holder.yes.isChecked
        }
        super.detachFromWindow(holder)
    }

    override fun bindView(holder: RecyclerView.ViewHolder, payloads: List<Any>) {
        super.bindView(holder, payloads)
        val holder = holder as ViewHolderYesOrNo
        visibleHolder = holder
        extensions.setImage(holder.imageview, kind)
        if (ismemberinternal) {
            holder.yes.isChecked = true
            holder.no.isChecked = false
        } else {
            holder.yes.isChecked = false
            holder.no.isChecked = true
        }

        holder.radiogroup.setOnCheckedChangeListener { group, checkedId ->
            live.value = holder.yes.isChecked
        }

//        extensions.setImage(holder.whatsappimage, kind)

//        holder.delete.visibility = View.GONE
//        holder.rua.apply {
//            text = extensions.setTextFromCacheOrFunction(primarytext, rua)
//            primarytext = this.text.toString()
//            inputType = InputType.TYPE_CLASS_TEXT
//            hint = "Nome completo"
//        }
    }

    fun isYesSelected(): Boolean {
        return (visibleHolder as? ViewHolderYesOrNo)?.let {
            return@let it.yes.isChecked
        } ?: false
    }

    internal class ViewHolderYesOrNo constructor(v: View) : RecyclerView.ViewHolder(v) {
        //        internal val rua: AppCompatEditText = v.rua
        internal val imageview = v.imageView
        internal val radiogroup = v.radiogroup
        internal val yes = v.radiogroup.yes
        internal val no = v.radiogroup.no
    }
}