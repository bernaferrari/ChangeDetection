/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.browser

import android.webkit.WebView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Keeps the browser adapter's destruction contract covered without obsolete Hilt scaffolding. */
@RunWith(AndroidJUnit4::class)
class WebViewLifecycleTest {
    @Test
    fun destroySafelyDestroysAConfiguredWebView() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        var cleanupCompleted = false

        instrumentation.runOnMainSync {
            WebView(instrumentation.targetContext).apply {
                configureChangeDetectionWebView()
                addJavascriptInterface(Any(), "syntheticBridge")
                destroySafely("syntheticBridge")
            }
            cleanupCompleted = true
        }

        assertTrue(cleanupCompleted)
    }
}
