package com.bernaferrari.changedetection.repo

import kotlinx.serialization.Serializable

/** Existing monitors retain their configured global behavior until explicitly changed. */
@Serializable
enum class CaptureMode {
    TEXT,
    SOURCE,
    DEFAULT,
    HTML,
    SCREENSHOT,
    DIRECT_TEXT,
    ;

    internal val isText: Boolean get() = this == TEXT || this == DIRECT_TEXT

    fun resolve(defaultMethod: ComparisonMethod): ComparisonMethod = when (this) {
        DEFAULT -> defaultMethod
        HTML, TEXT, DIRECT_TEXT, SOURCE -> ComparisonMethod.HTML
        SCREENSHOT -> ComparisonMethod.SCREENSHOT
    }
}
