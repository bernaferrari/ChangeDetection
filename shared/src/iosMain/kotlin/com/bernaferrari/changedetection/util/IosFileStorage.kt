package com.bernaferrari.changedetection.util

import platform.Foundation.*
import kotlinx.cinterop.*
import com.bernaferrari.changedetection.error.DatabaseError
import com.bernaferrari.changedetection.error.SnapshotStorageError

@OptIn(ExperimentalForeignApi::class)
class IosFileStorage : FileStorage {
    private val fileManager = NSFileManager.defaultManager

    private fun getDocumentsDirectory(): String {
        val paths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, true)
        return paths.first() as String
    }

    private fun getFilePath(fileName: String): String = "${getDocumentsDirectory()}/$fileName"

    override fun writeBytes(fileName: String, bytes: ByteArray) {
        val path = getFilePath(fileName)
        val data = bytes.toNSData()
        if (!data.writeToFile(path, true)) throw SnapshotStorageError(lowSpace = false)
    }

    override fun readBytes(fileName: String): ByteArray {
        val path = getFilePath(fileName)
        val size = (fileManager.attributesOfItemAtPath(path, null)?.get(NSFileSize) as? NSNumber)?.longLongValue
        if (size != null && size > MAX_SNAPSHOT_BYTES) throw DatabaseError(DatabaseError.Operation.READ, "Saved snapshot exceeds the supported size")
        val data = NSData.dataWithContentsOfFile(path)
            ?: throw DatabaseError(DatabaseError.Operation.READ, "Could not read saved snapshot")
        if (data.length > MAX_SNAPSHOT_BYTES.toULong()) throw DatabaseError(DatabaseError.Operation.READ, "Saved snapshot exceeds the supported size")
        return data.toKByteArray()
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

    private fun ByteArray.toNSData(): NSData = memScoped {
        if (isEmpty()) return NSData.data()
        val data = NSMutableData.dataWithCapacity(size.toULong())!!
        data.appendBytes(allocArrayOf(this@toNSData), size.toULong())
        data
    }

    private fun NSData.toKByteArray(): ByteArray {
        val size = length.toInt()
        val bytes = ByteArray(size)
        if (size > 0) {
            val ptr = this.bytes!!.reinterpret<ByteVar>()
            for (i in 0 until size) {
                bytes[i] = ptr[i]
            }
        }
        return bytes
    }
}
