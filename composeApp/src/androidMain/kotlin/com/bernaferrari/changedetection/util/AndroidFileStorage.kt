package com.bernaferrari.changedetection.util

import android.content.Context
import java.io.File

class AndroidFileStorage(private val context: Context) : FileStorage {
    override fun writeBytes(fileName: String, bytes: ByteArray) {
        context.openFileOutput(fileName, Context.MODE_PRIVATE).use {
            it.write(bytes)
        }
    }

    override fun readBytes(fileName: String): ByteArray {
        return context.openFileInput(fileName).use {
            it.readBytes()
        }
    }

    override fun deleteFile(fileName: String): Boolean {
        return context.deleteFile(fileName)
    }

    override fun fileExists(fileName: String): Boolean {
        val file = context.getFileStreamPath(fileName)
        return file != null && file.exists()
    }
}
