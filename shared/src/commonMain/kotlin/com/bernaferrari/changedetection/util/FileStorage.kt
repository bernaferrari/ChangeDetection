package com.bernaferrari.changedetection.util

interface FileStorage {
    /** Snapshot-shaped files old enough to be safely checked against the database. */
    fun orphanCandidates(olderThan: Long): List<String> = emptyList()

    fun writeBytes(
        fileName: String,
        bytes: ByteArray,
    )

    fun readBytes(fileName: String): ByteArray

    fun deleteFile(fileName: String): Boolean

    fun fileExists(fileName: String): Boolean
}
