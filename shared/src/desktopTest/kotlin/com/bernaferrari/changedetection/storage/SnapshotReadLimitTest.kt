package com.bernaferrari.changedetection.storage

import com.bernaferrari.changedetection.util.DesktopFileStorage
import com.bernaferrari.changedetection.util.MAX_SNAPSHOT_BYTES
import java.io.RandomAccessFile
import java.nio.file.Files
import org.junit.Assert.*
import org.junit.Test

class SnapshotReadLimitTest {
    @Test fun oversizedSparseFileIsRejectedWithoutTrustingItsMetadata() {
        val dir = Files.createTempDirectory("snapshot-limit").toFile()
        try {
            val file = dir.resolve("damaged");RandomAccessFile(file, "rw").use { it.setLength(MAX_SNAPSHOT_BYTES.toLong() + 1) }
            assertTrue(runCatching { DesktopFileStorage(dir).readBytes("damaged") }.exceptionOrNull() is java.io.IOException)
            assertEquals(MAX_SNAPSHOT_BYTES.toLong() + 1, file.length())
        } finally { dir.deleteRecursively() }
    }
}
