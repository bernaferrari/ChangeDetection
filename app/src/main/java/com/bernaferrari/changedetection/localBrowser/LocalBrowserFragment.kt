package com.bernaferrari.changedetection.localBrowser

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
class LocalBrowserFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val snapId = arguments?.getString(MainActivity.SNAPID) 
            ?: throw IllegalStateException("No snapId provided!")
        val url = arguments?.getString(MainActivity.URL) 
            ?: throw IllegalStateException("No url provided!")

        return ComposeView(requireContext()).apply {
            setContent {
                LocalBrowserScreen(
                    snapId = snapId,
                    url = url,
                    onDismiss = { findNavController().navigateUp() }
                )
            }
        }
    }
}
