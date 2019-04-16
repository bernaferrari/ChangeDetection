package com.bernaferrari.changedetection.addedit

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.customview.getCustomView
import com.bernaferrari.changedetection.R
import com.bernaferrari.changedetection.WorkerHelper.fetchFromServer
import com.bernaferrari.changedetection.extensions.readableFileSize
import com.bernaferrari.changedetection.repo.source.WebResult
import com.bernaferrari.changedetection.ui.CustomWebView
import kotlinx.android.synthetic.main.compare_dialog.view.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class CompareDialog : DialogFragment(), CoroutineScope by MainScope() {

    private val scope = MainScope()
    lateinit var webView: CustomWebView

    companion object {
        private const val TAG = "[DetailsDialog]"
        private const val KEY_URL = "url"

        fun <T> show(
            fragment: T,
            url: String
        ) where T : FragmentActivity {
            val dialog = CompareDialog().apply {
                arguments = Bundle().apply {
                    putString(KEY_URL, url)
                }
            }

            val ft = fragment.supportFragmentManager
                .beginTransaction()
                .addToBackStack(TAG)

            dialog.show(ft, TAG)
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val context = requireActivity()

        val url = requireNotNull(arguments?.getString(KEY_URL))

        return MaterialDialog(context)
            .customView(R.layout.compare_dialog)
            .show { getCustomView().setUpViews(url) }
    }

    private fun View.setUpViews(url: String) {

        scope.launch {

            val result = fetchFromServer(url)

            directProgressBar.visibility = View.GONE
            directRequestResult.visibility = View.VISIBLE

            directRequestResult.text = when {
                result.second.isNotEmpty() -> result.second.size.readableFileSize()
                else -> result.first
            }
        }

        scope.launch {
            // for some reason, Android WebView gets buggy when it is interrupted while loading,
            // so this is needed. WebView *NEEDS* to be destroyed before scope is cleared.
            webView = CustomWebView(context)
            val result = fetchFromWebView(
                url,
                requireContext(),
                webView
            )

            findViewById<View>(R.id.browserProgressBar).visibility = View.GONE
            findViewById<View>(R.id.browserRequestResult).visibility = View.VISIBLE

            browserRequestResult.text = when (result) {
                is WebResult.Success -> result.data.size.readableFileSize()
                is WebResult.Error -> result.description
            }
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        // be absolutely sure that WebView will be destroyed.
        webView.destroy()
        super.onDismiss(dialog)
    }

    override fun onDestroy() {
        super.onDestroy()
        webView.destroy()
        scope.cancel()
    }
}
