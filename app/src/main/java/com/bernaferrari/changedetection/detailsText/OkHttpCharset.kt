package com.bernaferrari.changedetection.detailsText

import java.nio.charset.Charset

object OkHttpCharset {

    private val UTF_8_BOM = decodeHex("efbbbf")
    private val UTF_16_BE_BOM = decodeHex("feff")
    private val UTF_16_LE_BOM = decodeHex("fffe")
    private val UTF_32_BE_BOM = decodeHex("0000ffff")
    private val UTF_32_LE_BOM = decodeHex("ffff0000")

    val UTF_8 = Charset.forName("UTF-8")
    val ISO_8859_1 = Charset.forName("ISO-8859-1")
    private val UTF_16_BE = Charset.forName("UTF-16BE")
    private val UTF_16_LE = Charset.forName("UTF-16LE")
    private val UTF_32_BE = Charset.forName("UTF-32BE")
    private val UTF_32_LE = Charset.forName("UTF-32LE")

    fun bomAwareCharset(source: ByteArray, charset: Charset): Charset {
        if (rangeEquals(source, UTF_8_BOM)) {
            return UTF_8
        }
        if (rangeEquals(source, UTF_16_BE_BOM)) {
            return UTF_16_BE
        }
        if (rangeEquals(source, UTF_16_LE_BOM)) {
            return UTF_16_LE
        }
        if (rangeEquals(source, UTF_32_BE_BOM)) {
            return UTF_32_BE
        }
        if (rangeEquals(source, UTF_32_LE_BOM)) {
            return UTF_32_LE
        }
        return charset
    }

    private fun rangeEquals(source: ByteArray, bytestring: ByteArray): Boolean {
        if (bytestring.size < source.size) {
            return false
        }

        bytestring.forEachIndexed { index, byte ->
            if (byte != source[index]) {
                return false
            }
        }
        return true
    }

    /** Decodes the hex-encoded bytes and returns their value a byte string.  */
    private fun decodeHex(hex: String): ByteArray {
        if (hex.length % 2 != 0) throw IllegalArgumentException("Unexpected hex string: $hex")

        val result = ByteArray(hex.length / 2)
        for (i in result.indices) {
            val d1 = decodeHexDigit(hex[i * 2]) shl 4
            val d2 = decodeHexDigit(hex[i * 2 + 1])
            result[i] = (d1 + d2).toByte()
        }
        return result.clone()
    }

    private fun decodeHexDigit(c: Char): Int {
        if (c in '0'..'9') return c - '0'
        if (c in 'a'..'f') return c - 'a' + 10
        if (c in 'A'..'F') return c - 'A' + 10
        throw IllegalArgumentException("Unexpected hex digit: $c")
    }
}