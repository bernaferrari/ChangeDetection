package com.example.changedetection.forms

import android.text.Editable
import android.widget.ImageView
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.iconics.IconicsDrawable
import com.orhanobut.logger.Logger

object extensions {
    const val color = 0xFF9E9E9E.toInt()

    fun setImage(image: ImageView, kind: Int) {
        Logger.d("icon kind: $kind")
        val icon = FormConstants.iconArr2[kind]
        image.setImageDrawable(
            IconicsDrawable(
                image.context,
                icon
            ).color(color)
        )
    }

    fun setDelete(image: ImageView) {
        val icon = CommunityMaterial.Icon.cmd_close

        image.setImageDrawable(
            IconicsDrawable(
                image.context,
                icon
            ).color(color)
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