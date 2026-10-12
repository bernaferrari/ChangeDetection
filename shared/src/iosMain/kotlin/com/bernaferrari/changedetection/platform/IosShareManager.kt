@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.bernaferrari.changedetection.platform

import kotlinx.cinterop.useContents
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
        val window = UIApplication.sharedApplication.keyWindow ?: UIApplication.sharedApplication.windows.firstOrNull() as? UIWindow ?: return
        var top: UIViewController = window.rootViewController ?: return
        while (true) top = top.presentedViewController ?: break
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
