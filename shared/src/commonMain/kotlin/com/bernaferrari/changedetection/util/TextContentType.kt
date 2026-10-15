package com.bernaferrari.changedetection.util

/** Text formats must use the same decoding policy during fetching, comparison and saved-page display. */
internal fun String.isTextContentType(): Boolean {
    val type = substringBefore(';').trim().lowercase()
    if (type.startsWith("image/")) return false
    return type.startsWith("text/") ||
        "html" in type || "xml" in type || "json" in type ||
        "javascript" in type || "ecmascript" in type
}
