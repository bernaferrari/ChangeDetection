package com.bernaferrari.changedetection.detailstext

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bernaferrari.changedetection.MainActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TextFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val siteId = arguments?.getString(MainActivity.SITEID)
            ?: throw IllegalStateException("No siteId provided!")
        val url = arguments?.getString(MainActivity.URL)
            ?: throw IllegalStateException("No url provided!")
        val contentType = arguments?.getString(MainActivity.TYPE) ?: "%"

        return ComposeView(requireContext()).apply {
            setContent {
                TextScreen(
                    siteId = siteId,
                    url = url,
                    contentType = contentType,
                    onNavigateUp = { findNavController().navigateUp() }
                )
            }
        }
    }
}
