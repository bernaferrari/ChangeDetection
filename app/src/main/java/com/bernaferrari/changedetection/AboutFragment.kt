package com.bernaferrari.changedetection

import android.content.Context
import com.danielstone.materialaboutlibrary.model.MaterialAboutList
import com.danielstone.materialaboutlibrary.MaterialAboutFragment


class AboutFragment : MaterialAboutFragment() {

    override fun getMaterialAboutList(activityContext: Context): MaterialAboutList {
        return MaterialAboutList.Builder()
            .build() // This creates an empty screen, add cards with .addCard()
    }

    override fun getTheme(): Int {
        return R.style.AppTheme_MaterialAboutActivity_Fragment
    }

}