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
