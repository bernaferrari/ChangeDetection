package com.bernaferrari.changedetection.forms

import android.text.Editable
import android.view.View
import android.view.animation.AnimationUtils
import androidx.core.view.isVisible
import com.bernaferrari.changedetection.R
import com.bernaferrari.changedetection.extensions.onTextChanged
import com.xwray.groupie.kotlinandroidextensions.ViewHolder
import kotlinx.android.synthetic.main.item_input_text.*

internal class FormInputText(
    var textInput: String,
    val title: String,
    val kind: String
) :
    com.xwray.groupie.kotlinandroidextensions.Item() {
    private var visibleHolder: ViewHolder? = null

    override fun getLayout(): Int = R.layout.item_input_text

    override fun bind(holder: ViewHolder, position: Int) {
        visibleHolder = holder

        Forms.setImage(holder.kind_input, kind)

        val queryClear: View = holder.clear_input
        queryClear.isVisible = textInput.isNotEmpty()

//        TooltipCompat.setTooltipText(queryClear, queryClear.contentDescription)
        queryClear.setOnClickListener {
            holder.text_input.setText("")
        }

        holder.clear_input.apply {
            holder.text_input
        }

        holder.text_input.apply {
            this.text = Editable.Factory.getInstance().newEditable(textInput)
            this.inputType = Forms.inputType(kind)
            this.hint = Forms.getHint(this.context, kind)
            this.onTextChanged {
                queryClear.isVisible = it.isNotEmpty()
            }
        }
    }

    fun shakeIt() {
        // inspired by Hurry from Sam Ruston
        visibleHolder?.containerView?.let {
            it.startAnimation(AnimationUtils.loadAnimation(it.context, R.anim.shake))
        }
    }

    override fun unbind(holder: ViewHolder) {
        textInput = holder.text_input.text.toString()
        super.unbind(holder)
    }

    internal fun retrieveText(): String? {
        return visibleHolder?.text_input?.text.toString()
            .replace(Regex("\\s+"), " ")
            .trim()
            .takeIf { it.isNotBlank() }
                ?: textInput
                    .replace(Regex("\\s+"), " ")
                    .trim()
                    .takeIf { it.isNotBlank() }
    }
}