/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.browser

import androidx.compose.runtime.*
import com.multiplatform.webview.request.*
import com.multiplatform.webview.web.WebViewNavigator

@Composable
internal fun rememberNavigationInterceptor(onRequest: ((String) -> Boolean)?): RequestInterceptor? {
    val current by rememberUpdatedState(onRequest)
    return remember(onRequest != null) {
        if (onRequest == null) {
            null
        } else {
            object : RequestInterceptor {
                override fun onInterceptUrlRequest(request: WebRequest, navigator: WebViewNavigator): WebRequestInterceptResult = if (current?.invoke(request.url) != false) WebRequestInterceptResult.Allow else WebRequestInterceptResult.Reject
            }
        }
    }
}
