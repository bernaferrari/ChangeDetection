package com.bernaferrari.changedetection.platform

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.orhanobut.logger.Logger
import java.io.File

class AndroidShareManager(private val context: Context) : ShareManager {

    override fun shareText(text: String, title: String) {
        try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(intent, title).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            Logger.e("Failed to share text: ${e.message}")
        }
    }

    override fun shareFile(fileName: String, text: String, title: String) {
        try {
            val cacheDir = File(context.cacheDir, "shares")
            cacheDir.mkdirs()
            val file = File(cacheDir, fileName)
            file.writeText(text)

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val chooser = Intent.createChooser(intent, title).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            Logger.e("Failed to share file: ${e.message}")
        }
    }
}
