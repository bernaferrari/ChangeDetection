package com.bernaferrari.changedetection.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SettingsFragment : Fragment() {

    private val viewModel: SettingsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                SettingsScreen(
                    viewModel = viewModel,
                    onAboutClick = {
                        // Handle about click
                    },
                    onBackgroundSyncClick = {
                        // Show background sync dialog
                    },
                    onRecreateActivity = {
                        activity?.recreate()
                    }
                )
            }
        }
    }
}
