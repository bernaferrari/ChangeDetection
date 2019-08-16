package com.bernaferrari.changedetection.addedit

import android.content.Context
import android.os.Build
import android.webkit.*
import com.bernaferrari.changedetection.repo.source.WebResult
import com.bernaferrari.changedetection.ui.CustomWebView
import kotlinx.coroutines.suspendCancellableCoroutine
import org.apache.commons.text.StringEscapeUtils
import kotlin.coroutines.resume

@JavascriptInterface
suspend fun fetchFromWebView(
    url: String,
    context: Context,
    wv: CustomWebView = CustomWebView(context)
) = suspendCancellableCoroutine<WebResult<ByteArray>> {

    wv.webViewClient = object : WebViewClient() {

        override fun onReceivedError(
            view: WebView?,
            request: WebResourceRequest?,
            error: WebResourceError?
        ) {
            val errorMessage = if (Build.VERSION.SDK_INT >= 23) {
                error?.description
            } else {
                error.toString()
            }

            it.resume(WebResult.Error(description = "$errorMessage from WebView"))
            super.onReceivedError(view, request, error)
        }

        override fun onPageFinished(view: WebView?, url: String?) {

            val js =
                "(function() { return ('<html>'+document.getElementsByTagName('html')[0].innerHTML+'</html>'); })();"

            wv.evaluateJavascript(js) { value ->

                val trimmedValue = value.substring(1, value.count() - 1)

                val unescapedValue = try {
                    StringEscapeUtils.unescapeEcmaScript(trimmedValue)
                } catch (e: IllegalArgumentException) {
                    trimmedValue
                }

                it.resume(WebResult.Success(unescapedValue.toByteArray()))
                view?.destroy()
            }

            super.onPageFinished(view, url)
        }
    }

    wv.loadUrl(url)
}