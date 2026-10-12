package com.bernaferrari.changedetection.util

import android.content.Context
import java.io.File
import java.io.FileOutputStream
import android.system.Os
import com.bernaferrari.changedetection.error.SnapshotStorageError

class AndroidFileStorage(
    private val context: Context,
    private val availableBytes: () -> Long = { context.filesDir.usableSpace },
) : FileStorage {
    override fun orphanCandidates(olderThan: Long): List<String> = context.filesDir.listFiles().orEmpty()
        .filter { it.isFile && it.lastModified() < olderThan && it.name.matches(Regex("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}(\\.partial)?")) }
        .map { it.name }

    override fun writeBytes(
        fileName: String,
        bytes: ByteArray,
    ) {
        val destination = context.getFileStreamPath(fileName)
        val temporary = context.getFileStreamPath("$fileName.partial")
        if (availableBytes() < bytes.size.toLong() + MIN_FREE_BYTES) {
            throw SnapshotStorageError(lowSpace = true)
        }
        try {
            FileOutputStream(temporary).use { stream ->
                stream.write(bytes)
                stream.fd.sync()
            }
            // Same-directory POSIX rename publishes all bytes together and throws on failure.
            Os.rename(temporary.absolutePath, destination.absolutePath)
        } catch (error: Exception) {
            throw SnapshotStorageError(lowSpace = availableBytes() < MIN_FREE_BYTES, cause = error)
        } finally {
            temporary.delete()
        }
    }

    override fun readBytes(fileName: String): ByteArray = context.openFileInput(fileName).use {
        it.readBytes()
    }

    override fun deleteFile(fileName: String): Boolean = context.deleteFile(fileName)

    override fun fileExists(fileName: String): Boolean {
        val file = context.getFileStreamPath(fileName)
        return file != null && file.exists()
    }
    private companion object {
        const val MIN_FREE_BYTES = 32L * 1024 * 1024
    }
}
