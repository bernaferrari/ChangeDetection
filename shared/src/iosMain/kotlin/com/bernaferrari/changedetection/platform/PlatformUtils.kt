/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.platform

import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIDevice

/**
 * Opens a URL in the default browser.
 */
fun openUrl(url: String) {
    val nsUrl = NSURL.URLWithString(url)
    if (nsUrl != null) {
        UIApplication.sharedApplication.openURL(nsUrl)
    }
}

/**
 * Opens the email client with a pre-filled email.
 */
fun openEmail(
    email: String,
    subject: String,
) {
    val urlString = "mailto:$email?subject=${subject.encodeURLParameter()}"
    val nsUrl = NSURL.URLWithString(urlString)
    if (nsUrl != null) {
        UIApplication.sharedApplication.openURL(nsUrl)
    }
}

/**
 * Encodes a string for use in a URL parameter.
 */
private fun String.encodeURLParameter(): String = this
    .replace(" ", "%20")
    .replace("&", "%26")
    .replace("=", "%3D")
    .replace("?", "%3F")
    .replace("#", "%23")

/**
 * Get device information for diagnostics.
 */
fun getDeviceInfo(): String = "iOS ${UIDevice.currentDevice.systemVersion} - ${UIDevice.currentDevice.model}"
