package com.bernaferrari.changedetection

import android.content.Context
import android.support.v4.content.ContextCompat
import com.danielstone.materialaboutlibrary.ConvenienceBuilder
import com.danielstone.materialaboutlibrary.MaterialAboutFragment
import com.danielstone.materialaboutlibrary.model.MaterialAboutCard
import com.danielstone.materialaboutlibrary.model.MaterialAboutList
import com.danielstone.materialaboutlibrary.util.OpenSourceLicense
import com.mikepenz.google_material_typeface_library.GoogleMaterial
import com.mikepenz.iconics.IconicsDrawable

class AboutLicenseFragment : MaterialAboutFragment() {

    override fun getMaterialAboutList(activityContext: Context): MaterialAboutList {
        return createMaterialAboutLicenseList(activityContext, R.color.md_indigo_500)
    }

    override fun getTheme(): Int = R.style.About

    private fun createLicenseCard(
        libraryTitle: String,
        year: String,
        name: String,
        license: OpenSourceLicense,
        context: Context,
        colorIcon: Int
    ): MaterialAboutCard {
        return ConvenienceBuilder.createLicenseCard(
            context,
            IconicsDrawable(context)
                .icon(GoogleMaterial.Icon.gmd_book)
                .color(ContextCompat.getColor(context, colorIcon))
                .sizeDp(18),
            libraryTitle,
            year,
            name,
            license
        )
    }

    private fun createMaterialAboutLicenseList(c: Context, colorIcon: Int): MaterialAboutList {

        val materialAboutLIbraryLicenseCard = createLicenseCard(
            "material-about-library",
            "2016",
            "Daniel Stone",
            OpenSourceLicense.APACHE_2,
            c,
            colorIcon
        )
        val androidIconicsLicenseCard = createLicenseCard(
            "Android Iconics",
            "2017",
            "Mike Penz",
            OpenSourceLicense.APACHE_2,
            c,
            colorIcon
        )
        val logger = createLicenseCard(
            "Logger",
            "2017",
            "Orhan Obut",
            OpenSourceLicense.APACHE_2,
            c,
            colorIcon
        )
        val materialdialogs = createLicenseCard(
            "Material Dialogs",
            "2014-2016",
            "Aidan Michael Follestad",
            OpenSourceLicense.MIT,
            c,
            colorIcon
        )
        val aosp = createLicenseCard(
            "The Android Open Source Project",
            "2018",
            "Google Inc.",
            OpenSourceLicense.APACHE_2,
            c,
            colorIcon
        )
        val rxjava = createLicenseCard(
            "RxJava: Reactive Extensions for the JVM",
            "2016-present",
            "RxJava Contributors",
            OpenSourceLicense.APACHE_2,
            c,
            colorIcon
        )
        val rxandroid = createLicenseCard(
            "RxAndroid: Reactive Extensions for Android",
            "2015",
            "The RxAndroid authors",
            OpenSourceLicense.APACHE_2,
            c,
            colorIcon
        )
        val groupie = createLicenseCard("Groupie", "2016", "", OpenSourceLicense.MIT, c, colorIcon)
        val okhttp = createLicenseCard("OkHttp", "", "", OpenSourceLicense.APACHE_2, c, colorIcon)
        val kotlin = createLicenseCard(
            "Kotlin",
            "2010-2017",
            "JetBrains",
            OpenSourceLicense.APACHE_2,
            c,
            colorIcon
        )

        return MaterialAboutList(
            aosp,
            androidIconicsLicenseCard,
            rxjava,
            rxandroid,
            groupie,
            materialAboutLIbraryLicenseCard,
            logger,
            kotlin,
            okhttp,
            materialdialogs
        )
    }
}

