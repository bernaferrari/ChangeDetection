package com.bernaferrari.changedetection.util

/** Bounded reads include old/damaged bodies; capture input is rejected before writing this limit. */
const val MAX_SNAPSHOT_BYTES: Int = 32 * 1024 * 1024

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
