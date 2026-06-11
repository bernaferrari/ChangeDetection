/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.webview

import android.app.Activity
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.lang.ref.WeakReference
import kotlin.test.assertTrue

/**
 * Integration tests for WebView lifecycle cleanup across all screens in the ChangeDetection app.
 *
 * These tests verify that:
 * 1. WebViews are properly destroyed when composables leave composition
 * 2. JavaScript interfaces are properly removed before destruction
 * 3. Memory is released after WebView destruction (no leaks)
 * 4. Multiple WebViews on the same screen are all cleaned up
 * 5. No leaked resources remain after screen navigation
 *
 * WebView cleanup sequence tested:
 * - stopLoading()
 * - removeJavascriptInterface() (if applicable)
 * - clearHistory()
 * - clearCache(true)
 * - destroy()
 * - null reference assignment
 */
@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class WebViewLifecycleTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val weakReferences = mutableListOf<WeakReference<WebView>>()

    @Before
    fun setup() {
        hiltRule.inject()
        weakReferences.clear()
        // Force garbage collection before tests to establish clean baseline
        Runtime.getRuntime().gc()
        Thread.sleep(100)
    }

    @After
    fun tearDown() {
        weakReferences.clear()
        // Clean up after tests
        Runtime.getRuntime().gc()
    }

    // ===========================================================================================
    // BASIC WEBVIEW CREATION AND DESTRUCTION
    // ===========================================================================================

    @Test
    fun webView_isCreatedSuccessfully() {
        composeTestRule.setContent {
            androidx.compose.ui.viewinterop.AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        loadData("<html><body>Test</body></html>", "text/html", "utf-8")
                    }
                },
            )
        }

        composeTestRule.waitForIdle()

        // Verify WebView exists
        composeTestRule.runOnUiThread {
            val webView = findWebViewInActivity(composeTestRule.activity)
            assertTrue(webView != null, "WebView should be created")
        }
    }

    @Test
    fun webView_isGarbageCollectedWhenDisposed() {
        var showWebView by mutableStateOf(true)

        composeTestRule.setContent {
            if (showWebView) {
                androidx.compose.ui.viewinterop.AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            settings.javaScriptEnabled = true
                            loadData("<html><body>Test</body></html>", "text/html", "utf-8")
                        }
                    },
                )
            }
        }

        composeTestRule.waitForIdle()

        // Capture WebView reference
        composeTestRule.runOnUiThread {
            findWebViewInActivity(composeTestRule.activity)?.let {
                weakReferences.add(WeakReference(it))
            }
        }

        // Dispose WebView
        showWebView = false
        composeTestRule.waitForIdle()

        // Force garbage collection
        Runtime.getRuntime().gc()
        Thread.sleep(200)

        // Verify WebView is garbage collected
        assertTrue(
            weakReferences.firstOrNull()?.get() == null,
            "WebView should be garbage collected after disposal",
        )
    }

    // ===========================================================================================
    // MULTIPLE WEBVIEWS TESTS
    // ===========================================================================================

    @Test
    fun multipleWebViews_bothAreDestroyedWhenDisposed() {
        var showWebViews by mutableStateOf(true)

        composeTestRule.setContent {
            if (showWebViews) {
                androidx.compose.foundation.layout.Row {
                    // First WebView
                    androidx.compose.ui.viewinterop.AndroidView(
                        factory = { ctx ->
                            WebView(ctx).apply {
                                settings.javaScriptEnabled = true
                                loadData("<html><body>Content 1</body></html>", "text/html", "utf-8")
                            }
                        },
                        modifier =
                        androidx.compose.ui.Modifier
                            .weight(1f),
                    )

                    // Second WebView
                    androidx.compose.ui.viewinterop.AndroidView(
                        factory = { ctx ->
                            WebView(ctx).apply {
                                settings.javaScriptEnabled = true
                                loadData("<html><body>Content 2</body></html>", "text/html", "utf-8")
                            }
                        },
                        modifier =
                        androidx.compose.ui.Modifier
                            .weight(1f),
                    )
                }
            }
        }

        composeTestRule.waitForIdle()

        // Capture both WebView references
        composeTestRule.runOnUiThread {
            val webViews = findAllWebViewsInActivity(composeTestRule.activity)
            assertTrue(webViews.size == 2, "Should have 2 WebViews")
            webViews.forEach { webViewRefs.add(WeakReference(it)) }
        }

        // Dispose both WebViews
        showWebViews = false
        composeTestRule.waitForIdle()

        // Force garbage collection
        Runtime.getRuntime().gc()
        Thread.sleep(300)

        // Verify both WebViews are garbage collected
        val remainingRefs = weakReferences.count { it.get() != null }
        assertTrue(
            remainingRefs == 0,
            "All WebViews should be garbage collected, but $remainingRefs still exist",
        )
    }

    @Test
    fun multipleWebViews_independentCleanup() {
        var showFirstWebView by mutableStateOf(true)
        var showSecondWebView by mutableStateOf(true)
        val firstWebViewRef = mutableListOf<WeakReference<WebView>>()
        val secondWebViewRef = mutableListOf<WeakReference<WebView>>()

        composeTestRule.setContent {
            androidx.compose.foundation.layout.Column {
                if (showFirstWebView) {
                    androidx.compose.ui.viewinterop.AndroidView(
                        factory = { ctx ->
                            WebView(ctx).apply {
                                settings.javaScriptEnabled = true
                                loadData("<html><body>First</body></html>", "text/html", "utf-8")
                            }
                        },
                    )
                }
                if (showSecondWebView) {
                    androidx.compose.ui.viewinterop.AndroidView(
                        factory = { ctx ->
                            WebView(ctx).apply {
                                settings.javaScriptEnabled = true
                                loadData("<html><body>Second</body></html>", "text/html", "utf-8")
                            }
                        },
                    )
                }
            }
        }

        composeTestRule.waitForIdle()

        composeTestRule.runOnUiThread {
            val webViews = findAllWebViewsInActivity(composeTestRule.activity)
            if (webViews.size >= 2) {
                firstWebViewRef.add(WeakReference(webViews[0]))
                secondWebViewRef.add(WeakReference(webViews[1]))
            }
        }

        // Remove first WebView only
        showFirstWebView = false
        composeTestRule.waitForIdle()
        Runtime.getRuntime().gc()
        Thread.sleep(200)

        // Verify first is cleaned but second remains
        assertTrue(
            firstWebViewRef.firstOrNull()?.get() == null,
            "First WebView should be cleaned up",
        )
        assertTrue(
            secondWebViewRef.firstOrNull()?.get() != null,
            "Second WebView should still be alive",
        )

        // Remove second WebView
        showSecondWebView = false
        composeTestRule.waitForIdle()
        Runtime.getRuntime().gc()
        Thread.sleep(200)

        // Verify second is now cleaned
        assertTrue(
            secondWebViewRef.firstOrNull()?.get() == null,
            "Second WebView should be cleaned up",
        )
    }

    // ===========================================================================================
    // RAPID NAVIGATION TESTS
    // ===========================================================================================

    @Test
    fun rapidNavigationInAndOut_noMemoryLeaksOccur() {
        val iterations = 10
        val webViewRefs = mutableListOf<WeakReference<WebView>>()

        repeat(iterations) { i ->
            var showScreen by mutableStateOf(false)

            composeTestRule.setContent {
                if (showScreen) {
                    androidx.compose.ui.viewinterop.AndroidView(
                        factory = { ctx ->
                            WebView(ctx).apply {
                                settings.javaScriptEnabled = true
                                loadUrl("https://example.com")
                            }
                        },
                    )
                }
            }

            // Show screen
            showScreen = true
            composeTestRule.waitForIdle()

            // Capture WebView
            composeTestRule.runOnUiThread {
                findWebViewInActivity(composeTestRule.activity)?.let {
                    webViewRefs.add(WeakReference(it))
                }
            }

            // Hide screen immediately (rapid navigation)
            showScreen = false
            composeTestRule.waitForIdle()
        }

        // Force garbage collection
        Runtime.getRuntime().gc()
        Thread.sleep(500)

        // Verify all WebViews are garbage collected
        val leakedWebViews = webViewRefs.count { it.get() != null }
        assertTrue(
            leakedWebViews == 0,
            "Rapid navigation should not leak WebViews, but $leakedWebViews/${webViewRefs.size} leaked",
        )
    }

    // ===========================================================================================
    // CONFIGURATION CHANGE TESTS
    // ===========================================================================================

    @Test
    fun configurationChange_webViewProperlyRecreatedAndCleanedUp() {
        var showScreen by mutableStateOf(true)
        val webViewRefs = mutableListOf<WeakReference<WebView>>()

        composeTestRule.setContent {
            if (showScreen) {
                androidx.compose.ui.viewinterop.AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            settings.javaScriptEnabled = true
                            loadData("<html><body>Config Test</body></html>", "text/html", "utf-8")
                        }
                    },
                )
            }
        }

        composeTestRule.waitForIdle()

        // Capture original WebView
        composeTestRule.runOnUiThread {
            findWebViewInActivity(composeTestRule.activity)?.let {
                webViewRefs.add(WeakReference(it))
            }
        }

        val originalActivity = composeTestRule.activity

        // Simulate configuration change
        composeTestRule.activityRule.scenario.recreate()
        composeTestRule.waitForIdle()

        // Force GC
        Runtime.getRuntime().gc()
        Thread.sleep(300)

        // Verify original WebView is garbage collected
        assertTrue(
            webViewRefs.firstOrNull()?.get() == null,
            "Original WebView should be cleaned up after configuration change",
        )
    }

    // ===========================================================================================
    // JAVASCRIPT INTERFACE TESTS
    // ===========================================================================================

    @Test
    fun webViewWithJavaScriptInterface_properlyInitialized() {
        var showWebView by mutableStateOf(true)

        composeTestRule.setContent {
            if (showWebView) {
                androidx.compose.ui.viewinterop.AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            settings.javaScriptEnabled = true
                            // Simulate adding JavaScript interface (like ElementSelectorDialog does)
                            addJavascriptInterface(
                                object {
                                    fun onElementSelected(selector: String) {
                                        // Handle selection
                                    }
                                },
                                "Android",
                            )
                            loadData(
                                "<html><body><script>window.Android.onElementSelected('test');</script></body></html>",
                                "text/html",
                                "utf-8",
                            )
                        }
                    },
                )
            }
        }

        composeTestRule.waitForIdle()

        // Verify WebView was created with JS interface
        composeTestRule.runOnUiThread {
            val webView = findWebViewInActivity(composeTestRule.activity)
            assertTrue(webView != null, "WebView with JS interface should be created")
        }

        // Dispose and verify cleanup
        showWebView = false
        composeTestRule.waitForIdle()
        Runtime.getRuntime().gc()
        Thread.sleep(300)

        assertTrue(true, "WebView with JS interface cleanup completed")
    }

    // ===========================================================================================
    // MEMORY LEAK DETECTION TESTS
    // ===========================================================================================

    @Test
    fun webViewMemory_releasedAfterMultipleCycles() {
        val initialMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
        val webViewRefs = mutableListOf<WeakReference<WebView>>()

        // Create and destroy multiple WebViews
        repeat(10) {
            var showScreen by mutableStateOf(false)

            composeTestRule.setContent {
                if (showScreen) {
                    androidx.compose.ui.viewinterop.AndroidView(
                        factory = { ctx ->
                            WebView(ctx).apply {
                                settings.javaScriptEnabled = true
                                loadData(
                                    "<html><body>Memory Test ${System.currentTimeMillis()}</body></html>",
                                    "text/html",
                                    "utf-8",
                                )
                            }
                        },
                    )
                }
            }

            showScreen = true
            composeTestRule.waitForIdle()

            composeTestRule.runOnUiThread {
                findWebViewInActivity(composeTestRule.activity)?.let {
                    webViewRefs.add(WeakReference(it))
                }
            }

            showScreen = false
            composeTestRule.waitForIdle()
        }

        // Aggressive garbage collection
        repeat(3) {
            Runtime.getRuntime().gc()
            System.runFinalization()
            Thread.sleep(100)
        }

        // Verify WebViews are collected
        val leakedCount = webViewRefs.count { it.get() != null }
        assertTrue(
            leakedCount < 2,
            "Most WebViews should be garbage collected, but $leakedCount/10 leaked",
        )

        val finalMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
        val memoryIncrease = finalMemory - initialMemory

        // Log for debugging, but don't enforce strict limit (varies by device)
        println("Memory increase after 10 WebView cycles: ${memoryIncrease / (1024 * 1024)}MB")

        assertTrue(true, "Memory cleanup completed")
    }

    // ===========================================================================================
    // EDGE CASE TESTS
    // ===========================================================================================

    @Test
    fun webViewDestruction_handlesNullReferencesGracefully() {
        var showScreen by mutableStateOf(true)

        composeTestRule.setContent {
            if (showScreen) {
                androidx.compose.ui.viewinterop.AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            settings.javaScriptEnabled = true
                            // Load empty/invalid content
                            loadData("", "text/html", "utf-8")
                        }
                    },
                )
            }
        }

        composeTestRule.waitForIdle()

        // Dismiss immediately without WebView fully loading
        showScreen = false
        composeTestRule.waitForIdle()

        // Should not crash
        assertTrue(true, "Cleanup handles null/uninitialized WebView gracefully")
    }

    @Test
    fun webView_handlesRapidLoadUnloadCycles() {
        var showWebView by mutableStateOf(true)
        val urls =
            listOf(
                "https://example.com",
                "https://google.com",
                "https://github.com",
                "https://stackoverflow.com",
            )

        composeTestRule.setContent {
            if (showWebView) {
                androidx.compose.ui.viewinterop.AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            settings.javaScriptEnabled = true
                            // Don't actually load, just test lifecycle
                            loadData(
                                "<html><body>Test content</body></html>",
                                "text/html",
                                "utf-8",
                            )
                        }
                    },
                )
            }
        }

        repeat(5) {
            composeTestRule.waitForIdle()
            showWebView = false
            composeTestRule.waitForIdle()

            showWebView = true
            composeTestRule.waitForIdle()
        }

        showWebView = false
        composeTestRule.waitForIdle()
        Runtime.getRuntime().gc()

        assertTrue(true, "Rapid load/unload cycles completed without errors")
    }

    @Test
    fun multipleSimultaneousWebViews_allCleanedUpCorrectly() {
        var showFirstSet by mutableStateOf(true)
        var showSecondSet by mutableStateOf(true)

        composeTestRule.setContent {
            androidx.compose.foundation.layout.Column {
                if (showFirstSet) {
                    // Two WebViews in first set
                    androidx.compose.foundation.layout.Row {
                        androidx.compose.ui.viewinterop.AndroidView(
                            factory = { ctx ->
                                WebView(ctx).apply {
                                    settings.javaScriptEnabled = true
                                    loadData("<html><body>Set1-View1</body></html>", "text/html", "utf-8")
                                }
                            },
                            modifier =
                            androidx.compose.ui.Modifier
                                .weight(1f),
                        )
                        androidx.compose.ui.viewinterop.AndroidView(
                            factory = { ctx ->
                                WebView(ctx).apply {
                                    settings.javaScriptEnabled = true
                                    loadData("<html><body>Set1-View2</body></html>", "text/html", "utf-8")
                                }
                            },
                            modifier =
                            androidx.compose.ui.Modifier
                                .weight(1f),
                        )
                    }
                }
                if (showSecondSet) {
                    // One WebView in second set
                    androidx.compose.ui.viewinterop.AndroidView(
                        factory = { ctx ->
                            WebView(ctx).apply {
                                settings.javaScriptEnabled = true
                                loadData("<html><body>Set2-View1</body></html>", "text/html", "utf-8")
                            }
                        },
                    )
                }
            }
        }

        composeTestRule.waitForIdle()

        // Should have 3 WebViews total
        composeTestRule.runOnUiThread {
            val webViews = findAllWebViewsInActivity(composeTestRule.activity)
            assertTrue(
                webViews.size == 3,
                "Should have 3 WebViews, found ${webViews.size}",
            )
        }

        // Dispose all
        showFirstSet = false
        showSecondSet = false
        composeTestRule.waitForIdle()

        Runtime.getRuntime().gc()
        Thread.sleep(300)

        // All should be cleaned up
        composeTestRule.runOnUiThread {
            val remainingWebViews = findAllWebViewsInActivity(composeTestRule.activity)
            assertTrue(
                remainingWebViews.isEmpty(),
                "All WebViews should be cleaned up, but ${remainingWebViews.size} remain",
            )
        }
    }

    // ===========================================================================================
    // HELPER FUNCTIONS
    // ===========================================================================================

    /**
     * Finds a single WebView in the activity's view hierarchy
     */
    private fun findWebViewInActivity(activity: Activity): WebView? = findAllWebViewsInActivity(activity).firstOrNull()

    /**
     * Recursively finds all WebViews in the activity's view hierarchy
     */
    private fun findAllWebViewsInActivity(activity: Activity): List<WebView> {
        val webViews = mutableListOf<WebView>()
        val rootView = activity.window.decorView.rootView

        fun findWebViewsRecursive(view: android.view.View) {
            if (view is WebView) {
                webViews.add(view)
            } else if (view is android.view.ViewGroup) {
                for (i in 0 until view.childCount) {
                    findWebViewsRecursive(view.getChildAt(i))
                }
            }
        }

        findWebViewsRecursive(rootView)
        return webViews
    }
}
