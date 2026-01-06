package com.bernaferrari.changedetection.platform

import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication

class IosShareManager : ShareManager {
    override fun shareText(text: String, title: String) {
        val activityController = UIActivityViewController(listOf(text), null)
        val window = UIApplication.sharedApplication.keyWindow
        window?.rootViewController?.presentViewController(activityController, true, null)
    }

    override fun shareFile(fileName: String, text: String, title: String) {
        // Simple implementation for now, just sharing text as handling files in iOS requires writing to tmp and passing URL
        val activityController = UIActivityViewController(listOf(text), null)
        val window = UIApplication.sharedApplication.keyWindow
        window?.rootViewController?.presentViewController(activityController, true, null)
    }
}
