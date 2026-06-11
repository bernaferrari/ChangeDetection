/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
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
