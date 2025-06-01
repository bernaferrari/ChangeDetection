package com.bernaferrari.changedetection.addedit

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bernaferrari.changedetection.repo.Site
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AddEditFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val site = arguments?.getParcelable<Site>("site")

        return ComposeView(requireContext()).apply {
            setContent {
                AddEditScreen(
                    site = site,
                    onNavigateUp = { findNavController().navigateUp() },
                    onNavigateToWebView = { url ->
                        // Navigate to web view
                    },
                    onShowCompareDialog = { url ->
                        // Show compare dialog
                    }
                )
            }
        }
    }
}
