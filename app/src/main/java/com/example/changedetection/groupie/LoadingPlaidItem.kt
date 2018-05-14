package com.example.changedetection.groupie

import android.annotation.TargetApi
import android.os.Build
import com.example.changedetection.R
import com.xwray.groupie.kotlinandroidextensions.Item
import com.xwray.groupie.kotlinandroidextensions.ViewHolder

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
class LoadingPlaidItem : Item() {
    override fun getLayout() = R.layout.bottomsheet_loading_plaid
    override fun bind(viewHolder: ViewHolder, position: Int) = Unit
}
