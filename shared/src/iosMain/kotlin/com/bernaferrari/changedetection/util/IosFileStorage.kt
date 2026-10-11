/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
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
        val data = NSData.dataWithContentsOfFile(path)
            ?: throw DatabaseError(DatabaseError.Operation.READ, "Could not read saved snapshot")
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
