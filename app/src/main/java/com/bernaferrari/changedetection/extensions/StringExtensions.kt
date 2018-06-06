package com.bernaferrari.changedetection.extensions

import android.util.Patterns

fun String.cleanUpHtml(): String {
    return this.replaceScriptTag().replaceStyleTag().replaceLinkTag().replaceMetaTag()
}

// Avoid some pages from changing the script on every fetch
// From https://stackoverflow.com/a/6660315/4418073
fun String.replaceScriptTag(): String {
    return this.replace("<script\\b[^<]*(?:(?!</script>)<[^<]*)*</script>".toRegex(), "")
}

// Avoid some pages from changing the style on every fetch
// From https://stackoverflow.com/a/29888314/4418073
fun String.replaceStyleTag(): String {
    return this.replace("<style([\\s\\S]+?)</style>".toRegex(), "")
}

// Avoid some pages from changing the link on every fetch
// From https://stackoverflow.com/a/7542023/4418073
fun String.replaceLinkTag(): String {
    return this.replace("</?link(?:(?= )[^>]*)?>".toRegex(), "")
}

// Avoid some pages from changing the meta on every fetch
// From https://stackoverflow.com/a/29888314/4418073
fun String.replaceMetaTag(): String {
    return this.replace("</?meta(?:(?= )[^>]*)?>".toRegex(), "")
}

// verify if a url is valid
fun String.isValidUrl(): Boolean {
    // first one will not catch links without http:// before them.
    return Patterns.WEB_URL.matcher(this).matches() && this.toLowerCase().matches(
        "^\\w+://.*".toRegex()
    )
}