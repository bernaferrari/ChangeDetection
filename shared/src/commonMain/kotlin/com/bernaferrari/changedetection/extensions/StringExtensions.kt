package com.bernaferrari.changedetection.extensions

import com.bernaferrari.changedetection.util.HtmlSanitizer
import com.fleeksoft.ksoup.parser.Parser
import io.ktor.http.Url

fun String.cleanUpHtml(): String = HtmlSanitizer.sanitize(this)

fun String.normalizeUrl(defaultScheme: String = "https"): String? {
    val trimmed = this.trim()
    if (trimmed.isEmpty()) return null

    val candidate = if ("://" in trimmed) trimmed else "$defaultScheme://$trimmed"
    return if (candidate.isValidUrl()) candidate else null
}

/**
 * Stable identity for an HTTP(S) page address.
 *
 * Host and scheme are case-insensitive, fragments are retained for browser routes, default ports are redundant, and a trailing slash denotes the same
 * page in the app. Path and query casing remain untouched because servers may
 * treat them as significant.
 */
fun String.canonicalUrlKey(includeFragment: Boolean = false): String? {
    val normalized = normalizeUrl() ?: return null
    return runCatching {
        val parsed = Url(normalized)
        val scheme = parsed.protocol.name.lowercase()
        val host = parsed.host.trimEnd('.').lowercase()
        val port = parsed.port
        val portSuffix = when {
            scheme == "http" && port == 80 -> ""
            scheme == "https" && port == 443 -> ""
            else -> ":$port"
        }
        val path = parsed.encodedPath.trimEnd('/').ifEmpty { "/" }
        val query = if (parsed.encodedQuery.isNotEmpty() || parsed.trailingQuery) {
            "?${parsed.encodedQuery}"
        } else {
            ""
        }
        val fragment = if (includeFragment && parsed.fragment.isNotEmpty()) "#${parsed.encodedFragment}" else ""
        "$scheme://$host$portSuffix$path$query$fragment"
    }.getOrNull()
}

/** Returns true for a complete HTTP(S) URL with a usable web host. */
fun String.isValidUrl(): Boolean {
    if (isBlank() || any(Char::isWhitespace)) return false

    return runCatching {
        val parsed = Url(this)
        val isWebScheme = parsed.protocol.name == "http" || parsed.protocol.name == "https"
        val host = parsed.host.trimEnd('.')
        val isLocalAddress = host.equals("localhost", ignoreCase = true) || ':' in host
        val hasQualifiedHost = host.contains('.') && host.split('.').all { it.isNotBlank() }
        isWebScheme && host.isNotBlank() && (isLocalAddress || hasQualifiedHost)
    }.getOrDefault(false)
}

fun String.removeClutterAndBeautifyHtmlIfNecessary(type: String): String = if (type == "text/html") HtmlSanitizer.sanitize(this) else this

fun String.unescapeHtml(): String = Parser.unescapeEntities(this, true)

fun String.findCharset(): String = "charset=([^()<>@,;:\"/\\[\\]?.=\\s]*)"
    .toRegex()
    .find(this)
    ?.value
    ?.replace("charset=", "")
    ?: ""

// this will convert all relative paths into absolute
fun String.replaceRelativePathWithAbsolute(absolute: String): String = this.replace("=\\s*['\"][^'\"\\s]+\\.\\w{3,4}['\"]".toRegex()) {
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

internal fun regexEmail(): Regex = "^(([\\w-]+\\.)+[\\w-]+|([a-zA-Z]|[\\w-]{2,}))@((([0-1]?[0-9]{1,2}|25[0-5]|2[0-4][0-9])\\.([0-1]?[0-9]{1,2}|25[0-5]|2[0-4][0-9])\\.([0-1]?[0-9]{1,2}|25[0-5]|2[0-4][0-9])\\.([0-1]?[0-9]{1,2}|25[0-5]|2[0-4][0-9]))|([a-zA-Z]+[\\w-]+\\.)+[a-zA-Z]{2,4})$"
    .toRegex()

internal fun regexValidWebsite(): Regex = "(http|https)://.*".toRegex()

fun String.fixUrlIfNecessary(): String = this.normalizeUrl() ?: this.trim()
