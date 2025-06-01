package com.bernaferrari.changedetection.detailsvisual

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
class VisualFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val siteId = arguments?.getString(MainActivity.SITEID)
            ?: throw IllegalStateException("No siteId provided!")
        val contentType = arguments?.getString(MainActivity.TYPE)
            ?: throw IllegalStateException("No contentType provided!")
        val url = arguments?.getString(MainActivity.URL)
            ?: throw IllegalStateException("No url provided!")

        return ComposeView(requireContext()).apply {
            setContent {
                VisualScreen(
                    siteId = siteId,
                    contentType = contentType,
                    url = url,
                    onNavigateUp = { findNavController().navigateUp() },
                    onShareSnap = { snap ->
                        // Handle share functionality
                        shareSnap(snap)
                    }
                )
            }
        }
    }

    private fun shareSnap(snap: com.bernaferrari.changedetection.repo.Snap) {
        // ...existing share logic...
    }
}
