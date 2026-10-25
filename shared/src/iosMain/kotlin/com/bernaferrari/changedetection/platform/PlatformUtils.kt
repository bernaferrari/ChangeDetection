package com.bernaferrari.changedetection.platform

import platform.Foundation.NSURL
import com.bernaferrari.changedetection.localization.getString
import com.bernaferrari.changedetection.shared.generated.resources.Res
import com.bernaferrari.changedetection.shared.generated.resources.app_name
import com.bernaferrari.changedetection.shared.generated.resources.done
import com.bernaferrari.changedetection.shared.generated.resources.external_open_failed
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import platform.UIKit.UIApplication
import platform.UIKit.UIDevice

/**
 * Opens a URL in the default browser.
 */
fun openUrl(url: String) {
    val nsUrl = NSURL.URLWithString(url)
    if (nsUrl != null) {
        if (!UIApplication.sharedApplication.openURL(nsUrl)) showOpenFailure()
    } else showOpenFailure()
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
        if (!UIApplication.sharedApplication.openURL(nsUrl)) showOpenFailure()
    } else showOpenFailure()
}

/** Process-owned UI feedback; completed actions release their controller references. */
private val externalFeedback = CoroutineScope(SupervisorJob() + Dispatchers.Main)

private fun showOpenFailure() {
    externalFeedback.launch {
        val title = getString(Res.string.app_name)
        val message = getString(Res.string.external_open_failed)
        val done = getString(Res.string.done)
        val window = UIApplication.sharedApplication.keyWindow ?: return@launch
        var top = window.rootViewController ?: return@launch
        while (top.presentedViewController != null) top = top.presentedViewController!!
        val alert = platform.UIKit.UIAlertController.alertControllerWithTitle(title, message, platform.UIKit.UIAlertControllerStyleAlert)
        alert.addAction(platform.UIKit.UIAlertAction.actionWithTitle(done, platform.UIKit.UIAlertActionStyleDefault, null))
        top.presentViewController(alert, true, null)
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
