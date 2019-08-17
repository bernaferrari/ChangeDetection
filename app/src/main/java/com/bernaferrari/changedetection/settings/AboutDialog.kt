package com.bernaferrari.changedetection.settings

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import androidx.core.content.FileProvider
import com.afollestad.materialdialogs.MaterialDialog
import com.bernaferrari.changedetection.BuildConfig.VERSION_NAME
import com.bernaferrari.changedetection.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

// inspired from mnml
class AboutDialog : BottomSheetDialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val context = activity ?: throw IllegalStateException("Oh no!")

        return MaterialDialog(context)
            .title(text = getString(R.string.about_title, VERSION_NAME))
            .message(res = R.string.about_body) {
                html()
                lineSpacing(1.4f)
            }
            .positiveButton(R.string.contact) { sendEmailWithLogs() }
            .negativeButton(R.string.dismiss)
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
        emailIntent.putExtra(Intent.EXTRA_EMAIL, arrayOf("bernaferrari2+cd@gmail.com"))
        emailIntent.putExtra(Intent.EXTRA_STREAM, contentUri)
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.email_subject))
        emailIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        startActivity(Intent.createChooser(emailIntent, "Send email..."))
    }
}
