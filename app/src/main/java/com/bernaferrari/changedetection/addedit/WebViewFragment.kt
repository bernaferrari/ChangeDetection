package com.bernaferrari.changedetection.addedit

import android.graphics.Bitmap
import android.os.Bundle
import android.text.Editable
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.findNavController
import com.bernaferrari.base.misc.hideKeyboard
import com.bernaferrari.base.view.onKey
import com.bernaferrari.changedetection.R
import com.bernaferrari.changedetection.ScopedFragment
import com.bernaferrari.changedetection.extensions.fixUrlIfNecessary
import kotlinx.android.synthetic.main.webview_frag.*

class WebViewFragment : ScopedFragment() {

    private lateinit var model: SharedViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.webview_frag, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        model = activity?.run {
            ViewModelProviders.of(this).get(SharedViewModel::class.java)
        } ?: throw Exception("Invalid Activity")

        fun goPreviousFragment() {
            webview.destroy()
            activity?.supportFragmentManager?.popBackStack()
        }

        materialButton.setOnClickListener {
            model.select(webview.url)
            goPreviousFragment()
        }

        webview.webViewClient = object : WebViewClient() {

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                refreshBack(view)
                updateUrl(url)
                progress?.visibility = View.VISIBLE
                super.onPageStarted(view, url, favicon)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                refreshBack(view)
                updateUrl(url)
                progress?.visibility = View.GONE
                super.onPageFinished(view, url)
            }
        }

        close.setOnClickListener { goPreviousFragment() }

        back.setOnClickListener { webview.goBack() }

        queryInput.onKey {
            if (it.keyCode == KeyEvent.KEYCODE_ENTER) {
                activity?.hideKeyboard()

                val url = queryInput.text.toString()
                val fixedUrl = url.fixUrlIfNecessary()
                updateUrl(fixedUrl)
                webview.loadUrl(fixedUrl)
                true
            } else {
                false
            }
        }

        if (savedInstanceState != null) {
            webview.destroy()
            view.findNavController().navigate(R.id.action_webviewFragment_to_mainFragmentNEW)
            return
        }

        val url = arguments?.getString("url") ?: "https://google.com"
        updateUrl(url)
        webview.loadUrl(url)
        refreshBack(webview)
    }

    fun refreshBack(view: WebView?) {
        back?.isVisible = view?.canGoBack() == true
    }

    fun updateUrl(url: String?) {
        queryInput?.text = url?.toEditText()
    }

    fun String.toEditText(): Editable = Editable.Factory.getInstance().newEditable(this)

    companion object {

        private const val KEY_URL = "url"

        fun newInstance(url: String): WebViewFragment {
            return WebViewFragment().apply {
                arguments = Bundle().apply { putString(KEY_URL, url) }
            }
        }
    }
}
