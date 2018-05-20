package com.example.changedetection

fun String.cleanUpHtml(): String{
    return this.replaceScriptTag().replaceLinkTag().replaceStyleTag()
}

// Avoid some pages from changing the script on every reload (like Google Analytics)
// From https://stackoverflow.com/a/6660315/4418073
fun String.replaceScriptTag(): String{
    return this.replace("<script\\b[^<]*(?:(?!</script>)<[^<]*)*</script>".toRegex(), "")
}

// Avoid some pages from changing the link on every reload
// From https://stackoverflow.com/a/7542023/4418073
fun String.replaceLinkTag(): String{
    return this.replace("</?link(?:(?= )[^>]*)?>".toRegex(), "")
}

// Avoid some pages from changing the style on every reload
// From https://stackoverflow.com/a/29888314/4418073
fun String.replaceStyleTag(): String{
    return this.replace("<style([\\s\\S]+?)</style>".toRegex(), "");
}