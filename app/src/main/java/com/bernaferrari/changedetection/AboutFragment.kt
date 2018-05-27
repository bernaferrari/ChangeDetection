package com.bernaferrari.changedetection

import android.content.Context
import android.net.Uri
import android.support.v4.content.ContextCompat
import androidx.core.net.toUri
import androidx.navigation.Navigation
import com.danielstone.materialaboutlibrary.ConvenienceBuilder
import com.danielstone.materialaboutlibrary.MaterialAboutFragment
import com.danielstone.materialaboutlibrary.items.MaterialAboutActionItem
import com.danielstone.materialaboutlibrary.items.MaterialAboutTitleItem
import com.danielstone.materialaboutlibrary.model.MaterialAboutCard
import com.danielstone.materialaboutlibrary.model.MaterialAboutList
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.google_material_typeface_library.GoogleMaterial
import com.mikepenz.iconics.IconicsDrawable


class AboutFragment : MaterialAboutFragment() {

    override fun getMaterialAboutList(activityContext: Context): MaterialAboutList {
        return createMaterialAboutList(activityContext) // This creates an empty screen, add cards with .addCard()
    }

    override fun getTheme(): Int = R.style.About

    private fun createMaterialAboutList(c: Context): MaterialAboutList {
        val grey = ContextCompat.getColor(c, R.color.md_grey_800)
        val iconsize = 18

        val appCardBuilder = MaterialAboutCard.Builder()

        appCardBuilder.addItem(
            MaterialAboutTitleItem.Builder()
                .text(R.string.app_name)
                .desc("© 2018 Bernardo Ferrari")
                .icon(R.mipmap.ic_launcher)
                .build()
        )

        appCardBuilder.addItem(
            ConvenienceBuilder.createVersionActionItem(
                c,
                IconicsDrawable(c)
                    .icon(GoogleMaterial.Icon.gmd_update)
                    .color(grey)
                    .sizeDp(iconsize),
                c.getText(R.string.version),
                false
            )
        )

        appCardBuilder.addItem(
            MaterialAboutActionItem.Builder()
                .text(R.string.licenses)
                .icon(
                    IconicsDrawable(c)
                        .icon(CommunityMaterial.Icon.cmd_github_circle)
                        .color(grey)
                        .sizeDp(iconsize)
                )
                .setOnClickAction {
                    Navigation.findNavController(view!!)
                        .navigate(R.id.action_aboutFragment_to_aboutLicenseFragment)
                }
                .build())

        appCardBuilder.addItem(
            ConvenienceBuilder.createRateActionItem(
                c,
                IconicsDrawable(c)
                    .icon(CommunityMaterial.Icon.cmd_star)
                    .color(ContextCompat.getColor(c, R.color.md_yellow_700))
                    .sizeDp(iconsize),
                c.getString(R.string.rate),
                null
            )
        )

        val author = MaterialAboutCard.Builder()

        author.title(R.string.author)

        author.addItem(
            MaterialAboutActionItem.Builder()
                .text("Bernardo Ferrari")
                .subText("bernaferrari")
                .icon(
                    IconicsDrawable(c)
                        .icon(CommunityMaterial.Icon.cmd_reddit)
                        .color(grey)
                        .sizeDp(iconsize)
                )
                .setOnClickAction(
                    ConvenienceBuilder.createWebsiteOnClickAction(
                        c,
                        "https://www.reddit.com/user/bernaferrari".toUri()
                    )
                )
                .build()
        )

        author.addItem(
            MaterialAboutActionItem.Builder()
                .text(R.string.github)
                .icon(
                    IconicsDrawable(c)
                        .icon(CommunityMaterial.Icon.cmd_github_circle)
                        .color(grey)
                        .sizeDp(iconsize)
                )
                .setOnClickAction(
                    ConvenienceBuilder.createWebsiteOnClickAction(
                        c,
                        "https://github.com/bernaferrari".toUri()
                    )
                )
                .build()
        )

        author.addItem(
            MaterialAboutActionItem.Builder()
                .text(R.string.email)
                .subText("bernaferrari2+cd@gmail.com")
                .icon(
                    IconicsDrawable(c)
                        .icon(CommunityMaterial.Icon.cmd_email)
                        .color(grey)
                        .sizeDp(iconsize)
                )
                .setOnClickAction(
                    ConvenienceBuilder.createEmailOnClickAction(
                        c,
                        "bernaferrari2+cd@gmail.com",
                        getString(R.string.email_subject)
                    )
                )
                .build()
        )

        val iconDesigner = MaterialAboutCard.Builder()

        iconDesigner.title("Icon Designer (Famil Qasimov)")

        iconDesigner.addItem(
            ConvenienceBuilder.createWebsiteActionItem(
                c,
                IconicsDrawable(c)
                    .icon(CommunityMaterial.Icon.cmd_github_circle)
                    .color(grey)
                    .sizeDp(iconsize),
                "GitHub",
                true,
                Uri.parse("https://github.com/familqasimov")
            )
        )

        val otherCardBuilder = MaterialAboutCard.Builder()
        otherCardBuilder.title(R.string.help)

        otherCardBuilder.addItem(
            MaterialAboutActionItem.Builder()
                .text(R.string.bugs)
                .subText("bernaferrari2+cd@gmail.com")
                .icon(
                    IconicsDrawable(c)
                        .icon(CommunityMaterial.Icon.cmd_bug)
                        .color(grey)
                        .sizeDp(iconsize)
                )
                .setOnClickAction(
                    ConvenienceBuilder.createEmailOnClickAction(
                        c,
                        "bernaferrari2+cd@gmail.com",
                        getString(R.string.email_subject)
                    )
                )
                .build()
        )

        return MaterialAboutList(
            appCardBuilder.build(),
            author.build(),
            iconDesigner.build(),
            otherCardBuilder.build()
        )
    }
}