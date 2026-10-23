package com.bernaferrari.changedetection.platform

import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

class AndroidShareManager(
    private val context: Context,
) : ShareManager {
    override fun shareText(
        text: String,
        title: String,
    ) {
        try {
            val intent =
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, text)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            val chooser =
                Intent.createChooser(intent, title).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            context.startActivity(chooser)
        } catch (e: Exception) {
            appLog.e("Failed to share text", e)
        }
    }

    override fun shareFile(
        fileName: String,
        text: String,
        title: String,
    ) {
        try {
            val cacheDir = File(context.cacheDir, "shares")
            check(cacheDir.exists() || cacheDir.mkdirs()) { "Unable to prepare the share cache" }
            val safeFileName = File(fileName).name
            require(safeFileName == fileName && safeFileName.isNotBlank()) { "Invalid share file name" }
            val file = File(cacheDir, safeFileName)
            file.writeText(text)

            val uri =
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.files",
                    file,
                )

            val intent =
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    clipData = ClipData.newRawUri(safeFileName, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }

            val chooser =
                Intent.createChooser(intent, title).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            context.startActivity(chooser)
        } catch (e: Exception) {
            appLog.e("Failed to share file", e)
        }
    }
}

private val appLog = com.bernaferrari.changedetection.logging.AppLog.tagged("AndroidShareManager")
