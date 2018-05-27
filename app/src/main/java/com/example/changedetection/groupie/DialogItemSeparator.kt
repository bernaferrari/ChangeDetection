package com.example.changedetection.groupie

import android.graphics.drawable.Drawable
import android.view.View
import com.example.changedetection.R
import com.xwray.groupie.kotlinandroidextensions.Item
import com.xwray.groupie.kotlinandroidextensions.ViewHolder

class DialogItemSeparator(

) : Item() {

    override fun getLayout(): Int {
        return R.layout.dialog_item_separator
    }

    override fun bind(viewHolder: ViewHolder, position: Int) {

    }
}
