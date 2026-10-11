/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.util

import androidx.compose.ui.platform.Clipboard
import platform.UIKit.UIPasteboard

actual suspend fun Clipboard.readText(): String? = UIPasteboard.generalPasteboard.string
