package com.bernaferrari.changedetection.forms

import android.support.v4.content.ContextCompat
import android.text.Editable
import android.widget.ImageView
import com.bernaferrari.changedetection.R
import com.mikepenz.iconics.IconicsDrawable
import com.orhanobut.logger.Logger

object extensions {

    fun setImage(image: ImageView, kind: String) {
        Logger.d("icon kind: $kind")
        val icon = Forms.getIcon(kind)
        image.setImageDrawable(
            IconicsDrawable(
                image.context,
                icon
            ).color(
                ContextCompat.getColor(image.context, R.color.FontWeaker)
            ).sizeDp(20)
        )
    }

    fun setTextFromCacheOrFunction(primarytext: String, nome: String): Editable {
        Editable.Factory.getInstance().run {
            return if (primarytext != "") {
                newEditable(primarytext)
            } else {
                newEditable(nome)
            }
        }
    }
}