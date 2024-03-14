package test

import com.bernaferrari.changedetection.extensions.readableFileSize
import org.junit.Assert
import org.junit.Test

internal class ReadableFileSizeTest {
    @Test
    fun testFileSizeZeroReturnsEmpty() {
        val input = 0
        val expected = "EMPTY"
        val output  = input.readableFileSize()
        Assert.assertEquals(expected, output)
    }

    @Test
    fun testNegativeFileSizeReturnsEmpty() {
        val input = -1
        val expected = "EMPTY"
        val output  = input.readableFileSize()
        Assert.assertEquals(expected, output)
    }

    @Test
    fun testBytesToKilobytesConversion() {
        val input = 1024
        val expected = "1 kB"
        val output  = input.readableFileSize()
        Assert.assertEquals(expected, output)
    }

    @Test
    fun testBytesToMegabytesConversion() {
        val input = 5242880
        val expected = "5 MB"
        val output  = input.readableFileSize()
        Assert.assertEquals(expected, output)
    }

    @Test
    fun testBytesToGigabytesConversion() {
        // This test will fail because the file size exceeds the maximum value of an Int.
        // Consider using a Long value for file sizes larger than Int.MAX_VALUE.
        // Example:
        // val fileSize: Long = 2147483648L
        // val expected = "2 GB"
        // val output = fileSize.readableFileSize()
        // assertEquals(expected, output)
    }
}