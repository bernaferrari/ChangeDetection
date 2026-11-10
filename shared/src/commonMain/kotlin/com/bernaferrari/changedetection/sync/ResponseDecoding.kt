package com.bernaferrari.changedetection.sync

/** How far into a document a charset declaration is looked for; the HTML standard uses the same limit. */
private const val META_SCAN_BYTES = 1024

private const val CHARSET_LABEL = "([A-Za-z0-9._:+-]+)"
private val HEADER_CHARSET = Regex("charset\\s*=\\s*[\"']?$CHARSET_LABEL", RegexOption.IGNORE_CASE)
private val META_CHARSET = Regex("<meta\\b[^>]*?charset\\s*=\\s*[\"']?$CHARSET_LABEL", RegexOption.IGNORE_CASE)

/**
 * The charset a text response should be decoded with: the Content-Type header's label when it has one,
 * otherwise (HTML only) the document's own meta declaration, otherwise UTF-8.
 */
internal fun responseCharset(contentType: String, body: ByteArray): String {
    HEADER_CHARSET.find(contentType.substringAfter(';', ""))?.let { return it.groupValues[1] }
    if ("html" !in contentType.substringBefore(';').lowercase()) return "UTF-8"
    // Charset declarations are ASCII in every ASCII-compatible encoding, so the prefix can be read byte for byte.
    val prefix = buildString(minOf(body.size, META_SCAN_BYTES)) {
        for (index in 0 until minOf(body.size, META_SCAN_BYTES)) append((body[index].toInt() and 0xFF).toChar())
    }
    return META_CHARSET.find(prefix)?.groupValues?.get(1) ?: "UTF-8"
}

/** The type of a response that arrived without a Content-Type header. Unrecognised content is treated as a web page. */
internal fun sniffContentType(body: ByteArray): String = when {
    body.startsWith(0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A) -> "image/png"
    body.startsWith(0xFF, 0xD8, 0xFF) -> "image/jpeg"
    body.startsWith(0x47, 0x49, 0x46, 0x38, 0x37, 0x61) || body.startsWith(0x47, 0x49, 0x46, 0x38, 0x39, 0x61) -> "image/gif"
    body.startsWith(0x52, 0x49, 0x46, 0x46) && body.startsWith(0x57, 0x45, 0x42, 0x50, offset = 8) -> "image/webp"
    body.startsWith(0x25, 0x50, 0x44, 0x46, 0x2D) -> "application/pdf"
    else -> "text/html"
}

private fun ByteArray.startsWith(vararg signature: Int, offset: Int = 0): Boolean = size >= offset + signature.size && signature.indices.all { this[offset + it] == signature[it].toByte() }
