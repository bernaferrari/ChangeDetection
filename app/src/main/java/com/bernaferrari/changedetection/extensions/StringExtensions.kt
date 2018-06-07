package com.bernaferrari.changedetection.extensions

import android.util.Patterns
import com.bernaferrari.changedetection.util.jsoup.Jsoup
import com.bernaferrari.changedetection.util.jsoup.nodes.Document
import com.bernaferrari.changedetection.util.jsoup.parser.Parser
import com.bernaferrari.changedetection.util.jsoup.safety.Whitelist

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

fun String.removeClutterAndBeautifyHtml(): String {
    return Jsoup.clean(this, Whitelist.relaxed())
}

fun String.unescapeHtml(): String {
    return Parser.unescapeEntities(this, true)
}