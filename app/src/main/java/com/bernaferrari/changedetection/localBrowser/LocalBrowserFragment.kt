package com.bernaferrari.changedetection.localBrowser

import android.app.Dialog
import android.os.Bundle
import android.webkit.WebView
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.customview.getCustomView
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import com.bernaferrari.changedetection.MainActivity
import com.bernaferrari.changedetection.R
import com.bernaferrari.changedetection.extensions.replaceRelativePathWithAbsolute
import com.bernaferrari.ui.extras.BaseDaggerMvRxDialogFragment
import kotlinx.android.synthetic.main.content_web.view.*
import javax.inject.Inject

class LocalBrowserFragment : BaseDaggerMvRxDialogFragment() {

    lateinit var webView: WebView

    private val viewModelLocal: LocalBrowserViewModel by fragmentViewModel()
    private var url = ""

    @Inject
    lateinit var textTopViewModelFactoryLocal: LocalBrowserViewModel.Factory

    override fun invalidate() = withState(viewModelLocal) {

        if (it.content.isNotBlank()) {
            putDataOnWebView(it.content.replaceRelativePathWithAbsolute(url))
        } else {
            webView.loadUrl("about:blank")
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val context = activity ?: throw IllegalStateException("Oh no!")
        val snapId =
            arguments?.getString(MainActivity.SNAPID) ?: throw IllegalStateException("No site!")
        url = arguments?.getString(MainActivity.URL) ?: throw IllegalStateException("No url!")

        return MaterialDialog(context)
            .customView(R.layout.content_web)
            .show {
                webView = getCustomView().webview
                viewModelLocal.fetchData(snapId)
            }
    }

    private fun putDataOnWebView(data: String) {
        println("rawr! ${data}")
//        val encodedHtml = Base64.encodeToString(data.toByteArray(), Base64.DEFAULT)
//        webView.loadData(encodedHtml, "text/html", "base64")

        webView.loadDataWithBaseURL("", data, "text/html", "UTF-8", "")
    }
}
