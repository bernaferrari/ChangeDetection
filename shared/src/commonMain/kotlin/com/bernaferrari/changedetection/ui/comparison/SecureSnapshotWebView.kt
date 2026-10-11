/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.ui.comparison

import com.bernaferrari.changedetection.browser.BrowserRuntimeHost
import com.bernaferrari.changedetection.browser.PlatformBrowserView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.fleeksoft.ksoup.Ksoup
import com.multiplatform.webview.web.WebViewState
import com.multiplatform.webview.web.rememberWebViewStateWithHTMLData

/**
 * Renders captured HTML as inert historical content, never as a live webpage.
 *
 * Stored pages are untrusted input. The document is stripped of executable and navigable content,
 * receives a deny-by-default CSP, and is loaded with the strongest cross-platform WebView settings
 * exposed by the library.
 */
@Composable
internal fun SecureSnapshotWebView(
    html: String,
    modifier: Modifier = Modifier,
) {
    val document = remember(html) { secureSnapshotDocument(html) }
    val state = rememberWebViewStateWithHTMLData(data = document)
    state.applySecureSnapshotSettings()

    BrowserRuntimeHost(modifier) {
        PlatformBrowserView(state = state, modifier = modifier)
    }
}

private fun WebViewState.applySecureSnapshotSettings() {
    webSettings.apply {
        isJavaScriptEnabled = false
        allowFileAccessFromFileURLs = false
        allowUniversalAccessFromFileURLs = false

        androidWebSettings.apply {
            allowFileAccess = false
            domStorageEnabled = false
            safeBrowsingEnabled = true
            mediaPlaybackRequiresUserGesture = true
            allowProtectedMedia = false
            allowMidiSysexMessages = false
        }
        iOSWebSettings.apply {
            mediaPlaybackRequiresUserGesture = true
            isInspectable = false
        }
        desktopWebSettings.disablePopupWindows = true
        wasmJSWebSettings.apply {
            enableSandbox = true
            sandboxPermissions = ""
            allowFullscreen = false
        }
    }
}

internal fun secureSnapshotDocument(html: String): String = runCatching {
    val document = Ksoup.parse(html)
    // Keep self-contained typography/layout. Network-dependent or escaped CSS is
    // omitted conservatively; the deny-by-default CSP remains the security boundary.
    var styleBudget = MAX_SNAPSHOT_STYLE_CHARS
    val styles = document.select("style").take(MAX_SNAPSHOT_STYLES).mapNotNull { style ->
        val css = style.data()
        if (style.attr("type").let { it.isNotBlank() && !it.equals("text/css", ignoreCase = true) } || css.length > styleBudget || UNSAFE_CSS.containsMatchIn(css) || '\\' in css) {
            null
        } else {
            styleBudget -= css.length
            css to style.attr("media")
        }
    }

    document.select(UNSAFE_ELEMENTS).remove()
    document.select("*").forEach { element ->
        element.attributes().asList().map { it.key }.forEach { attributeName ->
            val normalizedName = attributeName.lowercase()
            val value = element.attr(attributeName)
            val keepInlineImage =
                normalizedName == "src" &&
                    element.normalName() == "img" &&
                    value.isSafeInlineImage()
            val unsafeStyle = normalizedName == "style" && UNSAFE_CSS.containsMatchIn(value)

            if (
                normalizedName.startsWith("on") ||
                (normalizedName in NAVIGABLE_ATTRIBUTES && !keepInlineImage) ||
                unsafeStyle
            ) {
                element.removeAttr(attributeName)
            }
        }
    }

    val rendered = Ksoup.parse(snapshotDocument(document.body().html()))
    rendered.outputSettings().prettyPrint(false)
    // Classes, inline layout and direction on the original body affect its descendants.
    document.body().attributes().asList().forEach { rendered.body().attr(it.key, it.value) }
    listOf("lang", "dir", "class", "id", "style").forEach { name ->
        document.selectFirst("html")?.takeIf { it.hasAttr(name) }?.let { rendered.selectFirst("html")?.attr(name, it.attr(name)) }
    }
    styles.forEach { (css, media) ->
        rendered.head().appendElement("style").apply {
            if (media.isNotBlank()) attr("media", media)
            appendChild(com.fleeksoft.ksoup.nodes.DataNode(css))
        }
    }
    rendered.outerHtml()
}.getOrElse { snapshotDocument(html.escapeForSnapshot()) }

private fun snapshotDocument(body: String): String = """
    <!doctype html>
    <html>
      <head>
        <meta charset="utf-8" />
        <meta name="viewport" content="width=device-width, initial-scale=1" />
        <meta http-equiv="Content-Security-Policy" content="default-src 'none'; img-src data:; style-src 'unsafe-inline'; font-src data:; media-src 'none'; connect-src 'none'; frame-src 'none'; object-src 'none'; base-uri 'none'; form-action 'none'; navigate-to 'none'" />
        <style>
          :root { color-scheme: light; }
          html, body { min-height: 100%; margin: 0; background: #ffffff; color: #1d1d1f; }
          body {
            min-height: 100vh;
            box-sizing: border-box;
            padding: 20px;
            font: 16px/1.5 -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
            overflow-wrap: anywhere;
          }
          img, canvas, svg { max-width: 100%; height: auto; }
          table { max-width: 100%; border-collapse: collapse; }
        </style>
      </head>
      <body>$body</body>
    </html>
""".trimIndent()

private fun String.isSafeInlineImage(): Boolean = trim().lowercase().matches(INLINE_IMAGE_DATA)

private fun String.escapeForSnapshot(): String = replace("&", "&amp;")
    .replace("<", "&lt;")
    .replace(">", "&gt;")
    .replace("\"", "&quot;")
    .replace("'", "&#39;")

private const val MAX_SNAPSHOT_STYLE_CHARS = 256_000
private const val MAX_SNAPSHOT_STYLES = 32

private const val UNSAFE_ELEMENTS =
    "script, style, iframe, frame, frameset, object, embed, link, base, meta, form, input, " +
        "button, textarea, select, option, video, audio, source, track, portal, foreignObject, " +
        "animate, animateMotion, animateTransform, set"

private val NAVIGABLE_ATTRIBUTES = setOf(
    "href",
    "src",
    "srcset",
    "action",
    "formaction",
    "data",
    "poster",
    "background",
    "cite",
    "longdesc",
    "usemap",
    "ping",
    "profile",
    "manifest",
    "xlink:href",
)

private val UNSAFE_CSS = Regex(
    pattern = "url\\s*\\(|@import|expression\\s*\\(|-moz-binding",
    option = RegexOption.IGNORE_CASE,
)

private val INLINE_IMAGE_DATA = Regex(
    pattern = "data:image/(?:png|jpe?g|gif|webp);base64,[a-z0-9+/=\\s]+",
    option = RegexOption.IGNORE_CASE,
)
