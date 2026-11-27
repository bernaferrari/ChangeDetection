package com.bernaferrari.changedetection.util

import android.content.Context
import java.io.File
import java.io.FileOutputStream
import android.system.Os
import com.bernaferrari.changedetection.error.SnapshotStorageError

class AndroidFileStorage(
    private val context: Context,
    private val availableBytes: () -> Long = { context.filesDir.usableSpace },
) : com.bernaferrari.changedetection.backup.BackupSnapshotStorage {
    override fun orphanCandidates(olderThan: Long): List<String> = context.filesDir.listFiles().orEmpty()
        .filter { it.isFile && it.lastModified() < olderThan && it.name.matches(Regex("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}(\\.partial)?")) }
        .map { it.name }

    override fun writeBytes(
        fileName: String,
        bytes: ByteArray,
    ) {
        try {
            writeSnapshot(fileName, bytes.size) { output ->
                output.write(bytes)
                true
            }
        } catch (error: SnapshotStorageError) {
            throw error
        } catch (error: Exception) {
            throw SnapshotStorageError(lowSpace = availableBytes() < MIN_FREE_BYTES, cause = error)
        }
    }

    override fun openSnapshot(fileName: String): java.io.FileInputStream = context.openFileInput(fileName)

    override fun writeSnapshot(fileName: String, size: Int, write: (java.io.OutputStream) -> Boolean): Boolean {
        val destination = context.getFileStreamPath(fileName)
        val temporary = context.getFileStreamPath("$fileName.partial")
        if (availableBytes() < size.toLong() + MIN_FREE_BYTES) throw SnapshotStorageError(lowSpace = true)
        try {
            FileOutputStream(temporary).use { stream ->
                if (!write(stream)) return false
                stream.fd.sync()
            }
            Os.rename(temporary.absolutePath, destination.absolutePath)
            return true
        } finally {
            temporary.delete()
        }
    }

    override fun readBytes(fileName: String): ByteArray {
        if (context.getFileStreamPath(fileName).length() > MAX_SNAPSHOT_BYTES) throw java.io.IOException("Saved snapshot exceeds the supported size")
        return context.openFileInput(fileName).use {
            val output = java.io.ByteArrayOutputStream()
            val buffer = ByteArray(16_384)
            while (true) {
                val count = it.read(buffer)
                if (count < 0) break
                if (output.size().toLong() + count > MAX_SNAPSHOT_BYTES) throw java.io.IOException("Saved snapshot exceeds the supported size")
                output.write(buffer, 0, count)
            }
            output.toByteArray()
        }
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
