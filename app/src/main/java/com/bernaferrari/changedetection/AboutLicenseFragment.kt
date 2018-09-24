package com.bernaferrari.changedetection

import android.content.Context
import com.danielstone.materialaboutlibrary.ConvenienceBuilder
import com.danielstone.materialaboutlibrary.MaterialAboutFragment
import com.danielstone.materialaboutlibrary.model.MaterialAboutCard
import com.danielstone.materialaboutlibrary.model.MaterialAboutList
import com.danielstone.materialaboutlibrary.util.OpenSourceLicense
import com.mikepenz.google_material_typeface_library.GoogleMaterial
import com.mikepenz.iconics.IconicsDrawable

/**
 * Fragment which will display all app licenses.
 */
class AboutLicenseFragment : MaterialAboutFragment() {

    override fun getMaterialAboutList(activityContext: Context): MaterialAboutList {
        return createMaterialAboutLicenseList(activityContext)
    }

    override fun getTheme(): Int = R.style.About

    private fun createLicenseCard(
        libraryTitle: String,
        year: String,
        name: String,
        license: OpenSourceLicense,
        context: Context
    ): MaterialAboutCard {
        return ConvenienceBuilder.createLicenseCard(
            context,
            IconicsDrawable(context)
                .icon(GoogleMaterial.Icon.gmd_book)
                .colorRes(R.color.md_indigo_500)
                .sizeDp(18),
            libraryTitle,
            year,
            name,
            license
        )
    }

    private fun createMaterialAboutLicenseList(c: Context): MaterialAboutList {

        val materialAboutLIbraryLicenseCard = createLicenseCard(
            "material-about-library",
            "2016",
            "Daniel Stone",
            OpenSourceLicense.APACHE_2,
            c
        )
        val javadiffutils = createLicenseCard(
            "java-diff-utils",
            "2018",
            "Tobias (wumpz)",
            OpenSourceLicense.APACHE_2,
            c
        )
        val androidIconicsLicenseCard = createLicenseCard(
            "Android Iconics",
            "2017",
            "Mike Penz",
            OpenSourceLicense.APACHE_2,
            c
        )
        val logger = createLicenseCard(
            "Logger",
            "2017",
            "Orhan Obut",
            OpenSourceLicense.APACHE_2,
            c
        )
        val materialdialogs = createLicenseCard(
            "Material Dialogs",
            "2018",
            "Aidan Michael Follestad",
            OpenSourceLicense.APACHE_2,
            c
        )
        val aosp = createLicenseCard(
            "The Android Open Source Project",
            "2018",
            "Google Inc.",
            OpenSourceLicense.APACHE_2,
            c
        )
        val rxjava = createLicenseCard(
            "RxJava: Reactive Extensions for the JVM",
            "2016-present",
            "RxJava Contributors",
            OpenSourceLicense.APACHE_2,
            c
        )
        val rxandroid = createLicenseCard(
            "RxAndroid: Reactive Extensions for Android",
            "2015",
            "The RxAndroid authors",
            OpenSourceLicense.APACHE_2,
            c
        )
        val groupie = createLicenseCard("Groupie", "2016", "", OpenSourceLicense.MIT, c)
        val okhttp = createLicenseCard("OkHttp", "", "", OpenSourceLicense.APACHE_2, c)
        val kotlin = createLicenseCard(
            "Kotlin",
            "2010-2017",
            "JetBrains",
            OpenSourceLicense.APACHE_2,
            c
        )
        val jsoup = createLicenseCard(
            "Jsoup",
            "2009-2018",
            "Jonathan Hedley <jonathan@hedley.net>",
            OpenSourceLicense.MIT,
            c
        )
        val dagger = createLicenseCard(
            "Dagger",
            "2012",
            "The Dagger Authors",
            OpenSourceLicense.APACHE_2,
            c
        )

        return MaterialAboutList(
            aosp,
            javadiffutils,
            dagger,
            androidIconicsLicenseCard,
            rxjava,
            rxandroid,
            groupie,
            materialAboutLIbraryLicenseCard,
            logger,
            jsoup,
            kotlin,
            okhttp,
            materialdialogs
        )
    }
}

