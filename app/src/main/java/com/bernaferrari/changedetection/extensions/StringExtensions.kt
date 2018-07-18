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

fun String.findCharset(): String {
    return "charset=([^()<>@,;:\"/\\[\\]?.=\\s]*)".toRegex().find(this)
        ?.value?.replace("charset=", "") ?: ""
}

// this will convert all relative paths into absolute, i.e. for Google:
// <meta content="/images/branding/googleg/1x/googleg_standard_color_128dp.png" ...
// will become:
// <meta content="https://google.com//images/branding/googleg/1x/googleg_standard_color_128dp.png" ...
// this means it will get the content from the network, so text will be versioned, but images and javascript will be not.
fun String.replaceRelativePathWithAbsolute(absolute: String): String {
    return this.replace("=\\s*['\"][^'\"\\s]+\\.\\w{3,4}['\"]".toRegex()) {

        val valueWithoutWhitespace = it.value.replace(" ", "")
        val cleanValue = valueWithoutWhitespace.substring(2, valueWithoutWhitespace.length - 1)

        if (cleanValue.matches(regexEmail()) || cleanValue.matches(regexValidWebsite())) {
            return@replace it.value
        }

        return@replace "=${it.value.last()}$absolute" + it.value.replace("=\\s*['\"]".toRegex(), "")
    }
}

fun regexEmail(): Regex {
    return "^(([\\w-]+\\.)+[\\w-]+|([a-zA-Z]|[\\w-]{2,}))@((([0-1]?[0-9]{1,2}|25[0-5]|2[0-4][0-9])\\.([0-1]?[0-9]{1,2}|25[0-5]|2[0-4][0-9])\\.([0-1]?[0-9]{1,2}|25[0-5]|2[0-4][0-9])\\.([0-1]?[0-9]{1,2}|25[0-5]|2[0-4][0-9]))|([a-zA-Z]+[\\w-]+\\.)+[a-zA-Z]{2,4})$".toRegex()
}

fun regexValidWebsite(): Regex {
    return "(http|https)://.*".toRegex()
}
