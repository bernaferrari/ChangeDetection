package com.bernaferrari.changedetection.error

import com.bernaferrari.changedetection.repo.CaptureMode

/**
 * Whether checking the page's text is a different route past [failure], rather than the same attempt again.
 * Only routes that differ in kind are offered; a page that is down or gone fails by every route.
 */
internal fun textMayWork(mode: CaptureMode, failure: CheckFailure, repeated: Boolean): Boolean = when (failure) {
    // Text needs no steady picture, no images or fonts, and far less memory than a screenshot.
    CheckFailure.PAGE_UNSETTLED, CheckFailure.CAPTURE_INCOMPLETE, CheckFailure.CAPTURE_UNAVAILABLE, CheckFailure.CONTENT_TOO_LARGE -> !mode.isText

    // One timeout is usually the network, and anything would work a minute later. Only a repeat says something about the route.
    CheckFailure.TIMEOUT -> repeated && !mode.isText

    // Source is fetched without the app's browser. Text goes through it, with its sign-ins and a real browser's manners.
    CheckFailure.LOGIN, CheckFailure.ACCESS_DENIED, CheckFailure.CONTENT -> mode == CaptureMode.SOURCE || mode == CaptureMode.HTML

    else -> false
}
