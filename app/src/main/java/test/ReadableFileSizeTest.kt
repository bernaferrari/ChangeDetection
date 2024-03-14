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
}