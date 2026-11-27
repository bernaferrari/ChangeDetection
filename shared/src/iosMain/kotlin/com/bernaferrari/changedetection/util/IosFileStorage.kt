package com.bernaferrari.changedetection.util

import platform.Foundation.*
import kotlinx.cinterop.*
import platform.posix.memcpy
import com.bernaferrari.changedetection.error.DatabaseError
import com.bernaferrari.changedetection.error.SnapshotStorageError

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
class IosFileStorage : FileStorage {
    private val fileManager = NSFileManager.defaultManager

    private fun getDocumentsDirectory(): String {
        val paths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, true)
        return paths.first() as String
    }

    private fun getFilePath(fileName: String): String {
        require(fileName.isNotBlank() && fileName != "." && fileName != ".." && fileName.none { it == '/' || it == '\\' || it == '\u0000' }) {
            "Invalid snapshot file name"
        }
        return "${getDocumentsDirectory()}/$fileName"
    }

    override fun writeBytes(fileName: String, bytes: ByteArray) = autoreleasepool {
        val path = getFilePath(fileName)
        val data = bytes.toNSData()
        if (!data.writeToFile(path, true)) throw SnapshotStorageError(lowSpace = false)
    }

    override fun readBytes(fileName: String): ByteArray = autoreleasepool {
        val path = getFilePath(fileName)
        val size = (fileManager.attributesOfItemAtPath(path, null)?.get(NSFileSize) as? NSNumber)?.longLongValue
        if (size != null && size > MAX_SNAPSHOT_BYTES) throw DatabaseError(DatabaseError.Operation.READ, "Saved snapshot exceeds the supported size")
        val data = NSData.dataWithContentsOfFile(path)
            ?: throw DatabaseError(DatabaseError.Operation.READ, "Could not read saved snapshot")
        if (data.length > MAX_SNAPSHOT_BYTES.toULong()) throw DatabaseError(DatabaseError.Operation.READ, "Saved snapshot exceeds the supported size")
        data.toKByteArray()
    }

    override fun deleteFile(fileName: String): Boolean {
        val path = getFilePath(fileName)
        return fileManager.removeItemAtPath(path, null)
    }

    override fun fileExists(fileName: String): Boolean {
        val path = getFilePath(fileName)
        return fileManager.fileExistsAtPath(path)
    }

    override fun orphanCandidates(olderThan: Long): List<String> {
        val directory = getDocumentsDirectory()
        return fileManager.contentsOfDirectoryAtPath(directory, null).orEmpty().mapNotNull { entry ->
            val name = entry as? String ?: return@mapNotNull null
            if (!name.matches(Regex("[0-9a-fA-F-]{36}"))) return@mapNotNull null
            val attributes = fileManager.attributesOfItemAtPath("$directory/$name", null) ?: return@mapNotNull null
            val modified = attributes[NSFileModificationDate] as? NSDate ?: return@mapNotNull null
            name.takeIf { attributes[NSFileType] == NSFileTypeRegular && modified.timeIntervalSince1970 * 1000 < olderThan }
        }
    }

    private fun ByteArray.toNSData(): NSData = if (isEmpty()) {
        NSData.data()
    } else {
        usePinned { NSData.create(bytes = it.addressOf(0), length = size.toULong()) }
    }

    private fun NSData.toKByteArray(): ByteArray = ByteArray(length.toInt()).also { result ->
        if (result.isNotEmpty()) {
            result.usePinned { memcpy(it.addressOf(0), bytes, length) }
        }
    }
}
