package com.bernaferrari.changedetection.extensions

import com.bernaferrari.changedetection.util.HtmlSanitizer
import com.fleeksoft.ksoup.parser.Parser

fun String.cleanUpHtml(): String = HtmlSanitizer.sanitize(this)

fun String.normalizeUrl(defaultScheme: String = "https"): String? {
    val trimmed = this.trim()
    if (trimmed.isEmpty()) return null

    val candidate = if ("://" in trimmed) trimmed else "$defaultScheme://$trimmed"
    return if (candidate.isValidUrl()) candidate else null
}

// verify if a url is valid (accepts inputs without explicit scheme and normalizes internally)
fun String.isValidUrl(): Boolean {
    // Basic URL validation regex for KMP
    val regex = "^(https?://)?([\\w\\d\\-_]+\\.)+\\w{2,}(/.*)?$".toRegex()
    return regex.matches(this) || regexValidWebsite().matches(this)
}

fun String.removeClutterAndBeautifyHtmlIfNecessary(type: String): String =
    if (type == "text/html") HtmlSanitizer.sanitize(this) else this

fun String.unescapeHtml(): String = Parser.unescapeEntities(this, true)

fun String.findCharset(): String =
    "charset=([^()<>@,;:\"/\\[\\]?.=\\s]*)".toRegex().find(this)
        ?.value?.replace("charset=", "")
        ?: ""

// this will convert all relative paths into absolute
fun String.replaceRelativePathWithAbsolute(absolute: String): String =
    this.replace("=\\s*['\"][^'\"\\s]+\\.\\w{3,4}['\"]".toRegex()) {

        if (absolute.isBlank()) return@replace absolute

        val valueWithoutWhitespace = it.value.replace(" ", "")
        val cleanValue = valueWithoutWhitespace.substring(2, valueWithoutWhitespace.length - 1)

        if (cleanValue.matches(regexEmail()) || cleanValue.matches(regexValidWebsite())) {
            it.value
        } else {
            val website = if (absolute.last() != '/') "$absolute/" else absolute
            "=${it.value.last()}$website" + it.value.replace("=\\s*['\"]".toRegex(), "")
        }
    }

internal fun regexEmail(): Regex =
    "^(([\\w-]+\\.)+[\\w-]+|([a-zA-Z]|[\\w-]{2,}))@((([0-1]?[0-9]{1,2}|25[0-5]|2[0-4][0-9])\\.([0-1]?[0-9]{1,2}|25[0-5]|2[0-4][0-9])\\.([0-1]?[0-9]{1,2}|25[0-5]|2[0-4][0-9])\\.([0-1]?[0-9]{1,2}|25[0-5]|2[0-4][0-9]))|([a-zA-Z]+[\\w-]+\\.)+[a-zA-Z]{2,4})$".toRegex()

internal fun regexValidWebsite(): Regex = "(http|https)://.*".toRegex()

fun String.fixUrlIfNecessary(): String = this.normalizeUrl() ?: this.trim()
