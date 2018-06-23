package com.bernaferrari.changedetection.extensions

import android.util.Patterns
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.parser.Parser
import org.jsoup.safety.Whitelist

fun String.cleanUpHtml(): String {
    return Jsoup.clean(this, "", Whitelist.relaxed(), Document.OutputSettings().prettyPrint(false))
}

// verify if a url is valid
fun String.isValidUrl(): Boolean {
    // first one will not catch links without http:// before them.
    return Patterns.WEB_URL.matcher(this).matches() && this.toLowerCase().matches(
        "^\\w+://.*".toRegex()
    )
}

fun String.removeClutterAndBeautifyHtmlIfNecessary(type: String): String {
    return if (type == "text/html") {
        Jsoup.clean(this, Whitelist.relaxed())
    } else {
        this
    }
}

fun String.unescapeHtml(): String {
    return Parser.unescapeEntities(this, true)
}