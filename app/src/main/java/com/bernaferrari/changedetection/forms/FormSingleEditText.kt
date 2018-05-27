package com.bernaferrari.changedetection.forms

import android.text.Editable
import android.view.View
import com.bernaferrari.changedetection.R
import com.xwray.groupie.kotlinandroidextensions.ViewHolder
import kotlinx.android.synthetic.main.dialog_single_edittext.*

internal class FormSingleEditText(
    var nome: String,
    val kind: String
) :
    com.xwray.groupie.kotlinandroidextensions.Item() {
    private var visibleHolder: ViewHolder? = null

    override fun getLayout(): Int = R.layout.dialog_single_edittext

    override fun bind(holder: ViewHolder, position: Int) {
        visibleHolder = holder

        extensions.setImage(holder.imageView, kind)

        holder.delete.visibility = View.GONE
        holder.nome.apply {
            this.text = Editable.Factory.getInstance().newEditable(nome)
            this.inputType = Forms.inputType(kind)
            this.hint = Forms.getHint(this.context, kind)
        }
    }

    override fun unbind(holder: ViewHolder) {
        nome = holder.nome.text.toString()
        super.unbind(holder)
    }

    internal fun retrieveText(): String? {
        val txtfrombind = visibleHolder?.let {
            val finalstring = it.nome.text.toString()
                .replace(Regex("\\s+"), " ")
                .trim()

            if (finalstring.isNotBlank()) {
                return@let finalstring
            } else {
                return@let null
            }
        }

        if (txtfrombind == null) {
            val finalstring = nome
                .replace(Regex("\\s+"), " ")
                .trim()

            if (finalstring.isNotBlank()) {
                return finalstring
            }
            return null
        }
        return txtfrombind
    }
}