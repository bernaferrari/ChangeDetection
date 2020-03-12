package com.bernaferrari.changedetection

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.NavigationUI
import kotlinx.android.synthetic.main.nav_frag.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.nav_frag)

        NavigationUI.setupWithNavController(
                bottom_nav,
                nav_host_fragment.findNavController()
        )
    }

    companion object {
        const val DARKMODE = "dark mode"
        const val LASTCHANGE = "LASTCHANGE"
        const val SITEID = "SITEID"
        const val SNAPID = "SNAPID"
        const val TITLE = "TITLE"
        const val TYPE = "TYPE"
        const val URL = "URL"
        const val TRANSITION = 175L
    }
}
