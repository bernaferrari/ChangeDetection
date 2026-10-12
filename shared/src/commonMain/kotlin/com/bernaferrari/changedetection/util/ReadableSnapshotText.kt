package com.bernaferrari.changedetection.util

import com.fleeksoft.ksoup.Ksoup

private val textWhitespace = Regex("[\\s\u00a0]+")

/** Reading text only. Source remains intact in storage and in the Source comparison mode.
 * This does not infer visibility from external CSS or execute page scripts.
 */
internal fun readableSnapshotText(content: String, isHtml: Boolean): String {
    if (!isHtml) return content
    val document = Ksoup.parse(content)
    document.select("script,style,template,head,[hidden],[aria-hidden=true]").remove()
    document.select("p,div,section,article,header,footer,nav,li,h1,h2,h3,h4,h5,h6,tr,blockquote,pre,br,hr").forEach {
        it.prependText("\n")
        it.appendText("\n")
    }
    document.select("td,th").forEach { it.appendText(" ") }
    return document.body().wholeText()
        .lines()
        .map { it.replace(textWhitespace, " ").trim() }
        .filter(String::isNotEmpty)
        .joinToString("\n")
}
