package com.bernaferrari.changedetection.ui.about

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController

@Composable
fun HowItWorksScreen(navController: NavHostController) {
    // Deprecated duplicate. Use ui.howItWorks.HowItWorksScreen instead.
    // Keeping as a thin redirect to avoid accidental usages.
    com.bernaferrari.changedetection.ui.howItWorks.HowItWorksScreen(
        onBackClick = { navController.popBackStack() }
    )
}
