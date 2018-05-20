package com.example.changedetection

import android.annotation.SuppressLint
import android.app.Application
import android.app.Service
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.TypedArray
import android.net.Uri
import android.os.Build
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import es.dmoral.toasty.Toasty

// Inspired from FastHub and converted to Kotlin
class PrettifyWebView : NestedWebView {
    private var onContentChangedListener: OnContentChangedListener? = null
    private var interceptTouch: Boolean = false
    private var enableNestedScrolling: Boolean = false

    interface OnContentChangedListener {
        fun onContentChanged(progress: Int)

        fun onScrollChanged(reachedTop: Boolean, scroll: Int)
    }

    constructor(context: Context) : super(context) {
        if (isInEditMode) return
        initView(null)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        initView(attrs)
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        initView(attrs)
    }

    override fun onInterceptTouchEvent(p: MotionEvent): Boolean {
        return true
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (parent != null) {
            parent.requestDisallowInterceptTouchEvent(interceptTouch)
        }
        return super.onTouchEvent(event)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun initView(attrs: AttributeSet?) {

        webChromeClient = ChromeClient()

        val settings = settings
        settings.javaScriptEnabled = true
        settings.setAppCachePath(context.cacheDir.path)
        settings.setAppCacheEnabled(true)
        settings.cacheMode = WebSettings.LOAD_NO_CACHE
        settings.defaultTextEncodingName = "utf-8"
        settings.loadsImagesAutomatically = true
        settings.blockNetworkImage = false
        settings.builtInZoomControls = true
        settings.displayZoomControls = false

        setOnLongClickListener { view ->
            val result = hitTestResult
            if (hitLinkResult(result) && result.extra.isEmpty()) {
                copyToClipboard(context, result.extra)
                return@setOnLongClickListener true
            }
            false
        }
    }

    fun copyToClipboard(context: Context, uri: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(context.getString(R.string.app_name), uri)

        clipboard.primaryClip = clip
        Toasty.success(com.example.changedetection.Application.instance!!.applicationContext, context.getString(R.string.success_copied)).show()
    }

    override fun onScrollChanged(l: Int, t: Int, oldl: Int, oldt: Int) {
        super.onScrollChanged(l, t, oldl, oldt)
        if (onContentChangedListener != null) {
            onContentChangedListener!!.onScrollChanged(t == 0, t)
        }
    }

    override fun onDetachedFromWindow() {
        onContentChangedListener = null
        super.onDetachedFromWindow()
    }

    private fun hitLinkResult(result: WebView.HitTestResult): Boolean {
        return result.type == WebView.HitTestResult.SRC_ANCHOR_TYPE || result.type == WebView.HitTestResult.IMAGE_TYPE ||
                result.type == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE
    }

    fun setOnContentChangedListener(onContentChangedListener: OnContentChangedListener) {
        this.onContentChangedListener = onContentChangedListener
    }

    fun setSource(source: String, wrap: Boolean) {
        val settings = settings
        settings.layoutAlgorithm = WebSettings.LayoutAlgorithm.TEXT_AUTOSIZING
        scrollBarStyle = View.SCROLLBARS_INSIDE_OVERLAY
        settings.setSupportZoom(!wrap)
        settings.builtInZoomControls = !wrap
        if (!wrap) settings.displayZoomControls = false
        loadCode(source)
    }

    private fun loadCode(page: String) {
        post { loadDataWithBaseURL("", page, "text/html", "utf-8", null) }
    }

    fun scrollToLine(url: String) {
        val lineNo = getLineNo(url)
        if (lineNo != null && lineNo.size > 1) {
            loadUrl("javascript:scrollToLineNumber('" + lineNo[0] + "', '" + lineNo[1] + "')")
        } else if (lineNo != null) {
            loadUrl("javascript:scrollToLineNumber('" + lineNo[0] + "', '0')")
        }
    }

    fun loadImage(url: String, isSvg: Boolean) {
        val settings = settings
        settings.layoutAlgorithm = WebSettings.LayoutAlgorithm.SINGLE_COLUMN
        scrollBarStyle = View.SCROLLBARS_INSIDE_OVERLAY
        settings.setSupportZoom(true)
        settings.builtInZoomControls = true
        settings.displayZoomControls = false
        val html: String
        if (isSvg) {
            html = url
        } else {
            html = "<html><head><style>img{display: inline; height: auto; max-width: 100%;}</style></head><body>" +
                    "<img src=\"" + url + "\"/></body></html>"
        }

        loadData(html, "text/html", null)
    }

    fun setInterceptTouch(interceptTouch: Boolean) {
        this.interceptTouch = interceptTouch
    }

    fun setEnableNestedScrolling(enableNestedScrolling: Boolean) {
        if (this.enableNestedScrolling != enableNestedScrolling) {
            isNestedScrollingEnabled = enableNestedScrolling
            this.enableNestedScrolling = enableNestedScrolling
        }
    }

    private inner class ChromeClient : WebChromeClient() {
        override fun onProgressChanged(view: WebView, progress: Int) {
            super.onProgressChanged(view, progress)
            if (onContentChangedListener != null) {
                onContentChangedListener!!.onContentChanged(progress)
            }
        }
    }

    companion object {
        fun getLineNo(url: String?): Array<String>? {
            var lineNo: Array<String>? = null
            if (url != null) {
                try {
                    val uri = Uri.parse(url)
                    val lineNumber = uri.encodedFragment
                    if (lineNumber != null) {
                        lineNo = lineNumber.replace("L".toRegex(), "").split("-".toRegex())
                            .dropLastWhile { it.isEmpty() }.toTypedArray()
                    }
                } catch (ignored: Exception) {
                }

            }
            return lineNo
        }
    }
}