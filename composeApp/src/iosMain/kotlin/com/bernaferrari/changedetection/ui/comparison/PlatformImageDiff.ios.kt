/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.ui.comparison

internal actual object PlatformImageDiff {
    actual suspend fun compare(
        beforeBytes: ByteArray,
        afterBytes: ByteArray,
        threshold: Double,
    ): SnapshotImageDiff = SnapshotImageDiff(
        match = false,
        mismatchPercentage = 100.0,
        differentPixels = 0,
        totalPixels = 0,
        beforeWidth = 0,
        beforeHeight = 0,
        afterWidth = 0,
        afterHeight = 0,
        diffImagePng = null,
        error = "Image diff is currently available on Android screenshots.",
    )
}
