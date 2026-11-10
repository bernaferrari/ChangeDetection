package com.bernaferrari.changedetection.sync

import org.junit.Assert.assertEquals
import org.junit.Test

class ResponseDecodingTest {
    private fun ascii(text: String) = text.toByteArray(Charsets.US_ASCII)

    @Test fun headerCharsetIsFoundWhateverItsSpelling() {
        for (header in listOf(
            "text/html; charset=ISO-8859-1",
            "text/html; Charset=ISO-8859-1",
            "text/html;CHARSET=ISO-8859-1",
            "text/html; charset=\"ISO-8859-1\"",
            "text/html; charset='ISO-8859-1'",
            "text/html; charset = ISO-8859-1 ; boundary=x",
        )) {
            assertEquals(header, "ISO-8859-1", responseCharset(header, ascii("<p>x</p>")))
        }
    }

    @Test fun headerCharsetWinsOverTheDocument() {
        assertEquals("ISO-8859-1", responseCharset("text/html; charset=ISO-8859-1", ascii("<meta charset=\"Shift_JIS\">")))
    }

    @Test fun htmlWithoutAHeaderCharsetUsesItsMetaDeclaration() {
        for (html in listOf(
            "<!doctype html><html><head><meta charset=\"windows-1252\"><title>x</title>",
            "<html><head><META CHARSET=windows-1252>",
            "<html><head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=windows-1252\">",
            "<html><head><meta content='text/html;charset=windows-1252' http-equiv='content-type'>",
        )) {
            assertEquals(html, "windows-1252", responseCharset("text/html", ascii(html)))
        }
    }

    @Test fun aDeclarationOutsideAMetaTagOrBeyondTheScanLimitIsIgnored() {
        assertEquals("UTF-8", responseCharset("text/html", ascii("<p>charset=windows-1252</p>")))
        assertEquals("UTF-8", responseCharset("text/html", ascii("<!--" + "x".repeat(2000) + "--><meta charset=\"windows-1252\">")))
    }

    @Test fun onlyHtmlIsScannedForMetaDeclarations() {
        assertEquals("UTF-8", responseCharset("application/json", ascii("{\"a\":\"<meta charset=windows-1252>\"}")))
        assertEquals("UTF-8", responseCharset("text/plain", ascii("<meta charset=windows-1252>")))
    }

    @Test fun textWithNoDeclarationIsUtf8() {
        assertEquals("UTF-8", responseCharset("text/html", ascii("<p>plain</p>")))
        assertEquals("UTF-8", responseCharset("text/html; charset=", ascii("<p>plain</p>")))
        assertEquals("UTF-8", responseCharset("text/html", ByteArray(0)))
    }

    @Test fun untypedResponsesAreRecognisedByTheirFirstBytes() {
        fun bytes(vararg values: Int) = ByteArray(values.size) { values[it].toByte() }
        assertEquals("image/png", sniffContentType(bytes(0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0, 0)))
        assertEquals("image/jpeg", sniffContentType(bytes(0xFF, 0xD8, 0xFF, 0xE0, 0, 0)))
        assertEquals("image/gif", sniffContentType(ascii("GIF89a") + bytes(1, 0)))
        assertEquals("image/gif", sniffContentType(ascii("GIF87a") + bytes(1, 0)))
        assertEquals("image/webp", sniffContentType(ascii("RIFF") + bytes(1, 2, 3, 4) + ascii("WEBPVP8 ")))
        assertEquals("application/pdf", sniffContentType(ascii("%PDF-1.7\n")))
        assertEquals("text/html", sniffContentType(ascii("<!doctype html><p>x</p>")))
        assertEquals("text/html", sniffContentType(ascii("RIFF") + bytes(1, 2, 3, 4) + ascii("WAVEfmt ")))
        assertEquals("text/html", sniffContentType(ByteArray(0)))
        assertEquals("text/html", sniffContentType(bytes(0x89, 0x50)))
    }
}
