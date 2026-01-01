package com.bernaferrari.changedetection.util

import platform.Foundation.*
import kotlinx.cinterop.*

@OptIn(ExperimentalForeignApi::class)
class IosFileStorage : FileStorage {
    private val fileManager = NSFileManager.defaultManager

    private fun getDocumentsDirectory(): String {
        val paths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, true)
        return paths.first() as String
    }

    private fun getFilePath(fileName: String): String {
        return "${getDocumentsDirectory()}/$fileName"
    }

    override fun writeBytes(fileName: String, bytes: ByteArray) {
        val path = getFilePath(fileName)
        val data = bytes.toNSData()
        data.writeToFile(path, true)
    }

    override fun readBytes(fileName: String): ByteArray {
        val path = getFilePath(fileName)
        val data = NSData.dataWithContentsOfFile(path) ?: return ByteArray(0)
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
