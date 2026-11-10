package com.bernaferrari.changedetection.backup

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ZipBackupTest {
    private fun zipOf(vararg entries: Pair<String, ByteArray>): ByteArray = ByteArrayOutputStream().also { out ->
        ZipBackupSink(out).use { sink -> entries.forEach { (name, bytes) -> sink.write(name, bytes) } }
    }.toByteArray()

    @Test fun `entries come back in order with their exact bytes`() {
        val picture = ByteArray(70_000) { (it % 251).toByte() }
        val source = ZipBackupSource(ByteArrayInputStream(zipOf("manifest.json" to "{}".toByteArray(), "snapshots/a" to picture, "snapshots/b" to ByteArray(0))))

        assertEquals("manifest.json", source.nextName())
        assertArrayEquals("{}".toByteArray(), source.readCurrent(10))
        assertEquals("snapshots/a", source.nextName())
        assertArrayEquals(picture, source.readCurrent(picture.size))
        assertEquals("snapshots/b", source.nextName())
        assertArrayEquals(ByteArray(0), source.readCurrent(0))
        assertNull(source.nextName())
    }

    @Test fun `an entry larger than the limit is refused instead of read`() {
        // 4 MB of zeros compresses to a few kilobytes: the classic way to exhaust memory.
        val source = ZipBackupSource(ByteArrayInputStream(zipOf("manifest.json" to ByteArray(4 * 1024 * 1024))))

        assertEquals("manifest.json", source.nextName())
        assertNull(source.readCurrent(1024))
    }

    @Test fun `an entry can be skipped without being read`() {
        val source = ZipBackupSource(ByteArrayInputStream(zipOf("a" to "one".toByteArray(), "b" to "two".toByteArray())))

        assertEquals("a", source.nextName())
        assertEquals("b", source.nextName())
        assertArrayEquals("two".toByteArray(), source.readCurrent(3))
    }

    @Test fun `a file that is not a zip has no entries`() {
        assertNull(ZipBackupSource(ByteArrayInputStream("{\"format\":\"x\"}".toByteArray())).nextName())
    }

    @Test fun `zip files are recognised by their first bytes`() {
        assertTrue(looksLikeZip(zipOf("a" to "one".toByteArray())))
        assertFalse(looksLikeZip("{\"format\":\"change-detection-backup\"}".toByteArray()))
        assertFalse(looksLikeZip(ByteArray(0)))
    }

    @Test fun `a picked zip is read as a full backup`() {
        val source = openBackup(ByteArrayInputStream(zipOf("manifest.json" to "{}".toByteArray(), "snapshots/a" to "one".toByteArray())))

        assertEquals("manifest.json", source.nextName())
        assertArrayEquals("{}".toByteArray(), source.readCurrent(10))
        assertEquals("snapshots/a", source.nextName())
    }

    @Test fun `a picked json file is read as a pages backup`() {
        val json = "{\"format\":\"change-detection-backup\"}".toByteArray()
        val source = openBackup(ByteArrayInputStream(json))

        assertEquals("manifest.json", source.nextName())
        assertArrayEquals(json, source.readCurrent(json.size))
        assertNull(source.nextName())
    }

    @Test fun `a huge file that is not a zip is not loaded whole`() {
        val source = openBackup(ByteArrayInputStream(ByteArray(MAX_MANIFEST_BYTES + 100) { 'x'.code.toByte() }))

        assertEquals("manifest.json", source.nextName())
        assertNull("more than a manifest can hold must be refused by the reader", source.readCurrent(MAX_MANIFEST_BYTES))
    }
}
