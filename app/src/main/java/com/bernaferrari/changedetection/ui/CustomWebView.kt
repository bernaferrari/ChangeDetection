package com.bernaferrari.changedetection.ui

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.webkit.WebChromeClient
import android.webkit.WebView

class CustomWebView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : WebView(context, attrs, defStyleAttr) {

    init {
        initView()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun initView() {

        webChromeClient = WebChromeClient()
        settings.javaScriptEnabled = true
        settings.defaultTextEncodingName = "utf-8"
        settings.loadsImagesAutomatically = true
        settings.blockNetworkImage = false
        settings.builtInZoomControls = true
        settings.displayZoomControls = false

        settings.useWideViewPort = true
        settings.loadWithOverviewMode = true
    }
}
