package com.example.changedetection.forms

import android.support.v7.widget.RecyclerView
import android.text.Editable
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import com.example.changedetection.R

internal class FormDecAddress(
    val map2: MutableMap<String, Editable>,
    override val kind: Int = FormConstants.iaddress
) :
    EmptyAdapter() {
    var visibleHolder: RecyclerView.ViewHolder? = null
    var cache = map2

    override fun getType(): Int = kind

    override fun getViewHolder(v: View): RecyclerView.ViewHolder = ViewHolderAddress(v)

    override fun getLayoutRes(): Int = R.layout.eureka_endereco_completo

    override fun detachFromWindow(holder: RecyclerView.ViewHolder) {
        if (holder is ViewHolderAddress) {
            mutableMapOf<String, Editable>().apply {
                this["bairro"] = holder.bairro.text
                this["rua"] = holder.rua.text
                this["cep"] = holder.cep.text
                this["cidade"] = holder.cidade.text
                this["complemento"] = holder.complemento.text
                this["numero"] = holder.numero.text
                cache = this
            }
        }
        super.detachFromWindow(holder)
    }

    override fun bindView(holder: RecyclerView.ViewHolder, payloads: List<Any>) {
        super.bindView(holder, payloads)

        val holder = holder as ViewHolderAddress
        cache.let {
            holder.rua.text = it["rua"]
            holder.bairro.text = it["bairro"]
            holder.cep.text = it["cep"]
            holder.cidade.text = it["cidade"]
            holder.numero.text = it["numero"]
            holder.complemento.text = it["complemento"]
        }
        visibleHolder = holder
        extensions.setImage(holder.whatsappimage, kind)
    }

    internal class ViewHolderAddress constructor(v: View) : RecyclerView.ViewHolder(v) {
        internal var whatsappimage: ImageView = v.findViewById(R.id.imageView)
        internal val rua: EditText = v.findViewById(R.id.nome)
        internal val cep: EditText = v.findViewById(R.id.cep)
        internal val bairro: EditText = v.findViewById(R.id.bairro)
        internal val cidade: EditText = v.findViewById(R.id.cidade)
        internal val complemento: EditText = v.findViewById(R.id.complemento)
        internal val numero: EditText = v.findViewById(R.id.numero)
    }
}
