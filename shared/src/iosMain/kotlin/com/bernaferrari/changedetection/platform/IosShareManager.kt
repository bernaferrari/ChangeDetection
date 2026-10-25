@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.bernaferrari.changedetection.platform

import kotlinx.cinterop.useContents
import platform.Foundation.valueForKey
import platform.CoreGraphics.CGRectMake
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow
import platform.UIKit.popoverPresentationController

class IosShareManager : ShareManager {
    override fun shareText(text: String, title: String) = present(text)

    override fun shareFile(fileName: String, text: String, title: String) = present(text)

    /** From whatever is on screen (a sheet included); on iPad the share sheet is a popover by the share button. */
    private fun present(text: String) {
        val application = UIApplication.sharedApplication
        val window = application.keyWindow ?: ((application.valueForKey("windows") as? List<*>)?.firstOrNull() as? UIWindow) ?: throw ShareUnavailableException()
        var top: UIViewController = window.rootViewController ?: throw ShareUnavailableException()
        while (true) top = top.presentedViewController ?: break
        if (top.view.window == null || top.isBeingDismissed()) throw ShareUnavailableException()
        val sheet = UIActivityViewController(listOf(text), null)
        sheet.popoverPresentationController?.let { popover: platform.UIKit.UIPopoverPresentationController ->
            popover.sourceView = top.view
            // The share button sits at the top trailing corner of every screen that offers sharing.
            val width = top.view.bounds.useContents { size.width }
            popover.sourceRect = CGRectMake(width - 96.0, 44.0, 1.0, 1.0)
        }
        top.presentViewController(sheet, animated = true, completion = null)
    }
}
