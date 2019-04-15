package com.bernaferrari.changedetection.addedit

import android.content.Context
import android.os.Build
import android.webkit.*
import com.bernaferrari.changedetection.repo.source.WebResult
import com.bernaferrari.changedetection.ui.CustomWebView
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

@JavascriptInterface
suspend fun fetchFromWebView(
    url: String,
    context: Context,
    wv: CustomWebView = CustomWebView(context)
) = suspendCancellableCoroutine<WebResult<ByteArray>> {

    wv.webViewClient = object : WebViewClient() {

        override fun onLoadResource(view: WebView?, url: String?) {
            println("RAWR onLoadResource")
            super.onLoadResource(view, url)
        }

        override fun onReceivedError(
            view: WebView?,
            request: WebResourceRequest?,
            error: WebResourceError?
        ) {
            println("rawr ERROR GLOBO")
            val errorMessage = if (Build.VERSION.SDK_INT >= 23) {
                error?.description
            } else {
                error.toString()
            }

            it.resume(WebResult.Error(description = "$errorMessage from WebView"))
            super.onReceivedError(view, request, error)
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            println("rawr onPageFinished GLOBO")

            val js =
                "(function() { return ('<html>'+document.getElementsByTagName('html')[0].innerHTML+'</html>'); })();"

            wv.evaluateJavascript(js) { value ->
                // no charset
                it.resume(WebResult.Success(value.toByteArray()))
                view?.destroy()
            }

            super.onPageFinished(view, url)
        }
    }

    wv.loadUrl(url)
    println("RAWR loaded url: ${url}")
}