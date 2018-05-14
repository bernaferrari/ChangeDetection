package com.example.changedetection.forms

import android.support.v7.widget.RecyclerView
import android.text.Editable
import android.text.InputType
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import com.example.changedetection.R

internal class FormDecExpands(
    val nome: String,
    override val kind: Int
) :
    EmptyAdapter() {
    var primarytext = ""
    var visibleHolder: RecyclerView.ViewHolder? = null

    override fun getType(): Int = kind

    override fun getViewHolder(v: View): RecyclerView.ViewHolder =
        ViewHolderSimpleExpands(v)

    override fun getLayoutRes(): Int = R.layout.eureka_single_edittext

    override fun detachFromWindow(holder: RecyclerView.ViewHolder) {
        if (holder is ViewHolderSimpleExpands) {
            primarytext = holder.nome.text.toString()
        }
        super.detachFromWindow(holder)
    }

    override fun bindView(holder: RecyclerView.ViewHolder, payloads: List<Any>) {
        super.bindView(holder, payloads)
        val holder = holder as ViewHolderSimpleExpands
        visibleHolder = holder
        extensions.setImage(holder.whatsappimage, kind)
        extensions.setDelete(holder.delete)

        when (kind) {
            FormConstants.iphone -> { // Phone
                holder.nome.apply {
                    text = Editable.Factory.getInstance().newEditable(primarytext)
                    inputType = InputType.TYPE_CLASS_PHONE
                    hint = "telefone"
                }
            }
            FormConstants.iemail -> { // Email
                holder.nome.apply {
                    text = Editable.Factory.getInstance().newEditable(primarytext)
                    inputType = InputType.TYPE_CLASS_TEXT
                    hint = "email"
                }
            }
            FormConstants.isons -> { // Filhos
                holder.nome.apply {
                    text = Editable.Factory.getInstance().newEditable(primarytext)
                    inputType = InputType.TYPE_CLASS_TEXT
                    hint = "filhos"
                }
            }
            else -> {
            }
        }
    }
//
//    internal fun retrieveText(): String? {
//        return (visibleHolder as? ViewHolderSimpleExpands)?.let {
//            val finalstring = it.nome.text.toString()
//                .replace(Regex("\\s+"), " ")
//                .trim()
//
//            if (finalstring != "" && finalstring != "null" && finalstring != " ") {
//                return@let finalstring
//            } else {
//                return@let null
//            }
//        }
//    }

    internal fun retrieveText(): String? {
        val txtfrombind = (visibleHolder as? ViewHolderSimpleExpands)?.let {
            val finalstring = it.nome.text.toString()
                .replace(Regex("\\s+"), " ")
                .trim()

            if (finalstring != "" && finalstring != "null" && finalstring != " ") {
                return@let finalstring
            } else {
                return@let null
            }
        }

        if (txtfrombind == null) {
            val finalstring = nome
                .replace(Regex("\\s+"), " ")
                .trim()

            if (finalstring != "" && finalstring != "null" && finalstring != " ") {
                return finalstring
            } else {
                return null
            }
        }
        return txtfrombind
    }

    internal class ViewHolderSimpleExpands constructor(v: View) : RecyclerView.ViewHolder(v) {
        internal var whatsappimage: ImageView = v.findViewById(R.id.imageView)
        internal val nome: EditText = v.findViewById(R.id.nome)
        internal val delete: ImageView = v.findViewById(R.id.delete)
    }
}