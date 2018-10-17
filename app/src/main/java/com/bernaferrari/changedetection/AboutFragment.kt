package com.bernaferrari.changedetection

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.navigation.findNavController
import com.danielstone.materialaboutlibrary.ConvenienceBuilder
import com.danielstone.materialaboutlibrary.MaterialAboutFragment
import com.danielstone.materialaboutlibrary.items.MaterialAboutActionItem
import com.danielstone.materialaboutlibrary.items.MaterialAboutTitleItem
import com.danielstone.materialaboutlibrary.model.MaterialAboutCard
import com.danielstone.materialaboutlibrary.model.MaterialAboutList
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.google_material_typeface_library.GoogleMaterial
import com.mikepenz.iconics.IconicsDrawable
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader


class AboutFragment : MaterialAboutFragment() {

    private val email = "bernaferrari2@gmail.com"

    private val isDarkModeOn =
        Injector.get().sharedPrefs().getBoolean(MainActivity.DARKMODE, false)

    override fun getMaterialAboutList(activityContext: Context): MaterialAboutList =
        createMaterialAboutList(activityContext)

    override fun getTheme() = if (isDarkModeOn) R.style.AboutDark else R.style.AboutLight

    private fun createMaterialAboutList(c: Context): MaterialAboutList {
        val standardIconColor = if (isDarkModeOn) R.color.md_grey_200 else R.color.md_grey_800
        val iconSize = 18

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
                    .colorRes(standardIconColor)
                    .sizeDp(iconSize),
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
                        .colorRes(standardIconColor)
                        .sizeDp(iconSize)
                )
                .setOnClickAction {
                    view?.findNavController()
                        ?.navigate(R.id.action_aboutFragment_to_aboutLicenseFragment)
                }
                .build())

        appCardBuilder.addItem(
            ConvenienceBuilder.createRateActionItem(
                c,
                IconicsDrawable(c)
                    .icon(CommunityMaterial.Icon.cmd_star)
                    .colorRes(R.color.md_yellow_700)
                    .sizeDp(iconSize),
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
                        .colorRes(standardIconColor)
                        .sizeDp(iconSize)
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
                        .colorRes(standardIconColor)
                        .sizeDp(iconSize)
                )
                .setOnClickAction(
                    ConvenienceBuilder.createWebsiteOnClickAction(
                        c,
                        "https://github.com/bernaferrari/ChangeDetection".toUri()
                    )
                )
                .build()
        )

        author.addItem(
            MaterialAboutActionItem.Builder()
                .text(R.string.email)
                .subText(email)
                .icon(
                    IconicsDrawable(c)
                        .icon(CommunityMaterial.Icon.cmd_email)
                        .colorRes(standardIconColor)
                        .sizeDp(iconSize)
                )
                .setOnClickAction(
                    ConvenienceBuilder.createEmailOnClickAction(
                        c,
                        email,
                        getString(R.string.email_subject)
                    )
                )
                .build()
        )

        val iconDesigner = MaterialAboutCard.Builder()

        iconDesigner.title(R.string.icon_designer)

        iconDesigner.addItem(
            ConvenienceBuilder.createWebsiteActionItem(
                c,
                IconicsDrawable(c)
                    .icon(CommunityMaterial.Icon.cmd_github_circle)
                    .colorRes(standardIconColor)
                    .sizeDp(iconSize),
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
                .subText(email)
                .icon(
                    IconicsDrawable(c)
                        .icon(CommunityMaterial.Icon.cmd_bug)
                        .colorRes(standardIconColor)
                        .sizeDp(iconSize)
                )
                .setOnClickAction { sendEmailWithLogs() }
                .build()
        )

        return MaterialAboutList(
            appCardBuilder.build(),
            author.build(),
            iconDesigner.build(),
            otherCardBuilder.build()
        )
    }

    private fun sendEmailWithLogs() {
        val log = StringBuilder()

        val process = Runtime.getRuntime().exec("logcat -d")
        val br = BufferedReader(InputStreamReader(process.inputStream))

        var line: String? = br.readLine()
        while (line != null) {
            log.append(line + "\n")
            line = br.readLine()
        }
        br.close()

        val file = File(requireContext().cacheDir, "logs.txt")
        file.createNewFile()
        file.writeBytes(log.toString().toByteArray())

        val contentUri = FileProvider.getUriForFile(
            requireContext(),
            "com.bernaferrari.changedetection.files",
            file
        )

        val emailIntent = Intent(Intent.ACTION_SEND)
        emailIntent.type = "message/rfc822"
        emailIntent.putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
        emailIntent.putExtra(Intent.EXTRA_STREAM, contentUri)
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.email_subject))
        emailIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        startActivity(Intent.createChooser(emailIntent, "Send email..."))
    }
}
