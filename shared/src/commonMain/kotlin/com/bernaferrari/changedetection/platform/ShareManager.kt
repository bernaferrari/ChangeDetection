package com.bernaferrari.changedetection.platform

interface ShareManager {
    fun shareText(
        text: String,
        title: String,
    )

    fun shareFile(
        fileName: String,
        text: String,
        title: String,
    )
}

/** Fixed UI-safe failure; callers report it rather than claiming a chooser opened. */
class ShareUnavailableException(cause: Throwable? = null) : Exception("Could not open sharing", cause)
