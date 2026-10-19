package com.bernaferrari.changedetection.util

import org.junit.Assert.assertEquals
import org.junit.Test

class FileSizeUtilsTest {
    @Test fun nonPositiveSizesStayZero() {
        for (bytes in listOf(Long.MIN_VALUE, -1L, 0L)) assertEquals("0 B", bytes.readableFileSize())
    }

    @Test fun byteAndUnitBoundariesKeepExistingFormatting() {
        assertEquals("1.0 B", 1L.readableFileSize())
        assertEquals("1023.0 B", 1023L.readableFileSize())
        for ((power, unit) in listOf("KB", "MB", "GB", "TB").withIndex()) {
            assertEquals("1.0 $unit", (1L shl ((power + 1) * 10)).readableFileSize())
        }
        assertEquals("1.9 KB", 2047L.readableFileSize())
    }

    @Test fun petabyteAndMaximumLongSizesNeverIndexPastTheUnits() {
        assertEquals("1.0 PB", (1L shl 50).readableFileSize())
        assertEquals("1.0 EB", (1L shl 60).readableFileSize())
        assertEquals("8.0 EB", Long.MAX_VALUE.readableFileSize())
    }
}
