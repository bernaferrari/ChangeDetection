package com.bernaferrari.changedetection

import android.os.Bundle
import android.support.v7.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.nav_frag)
    }

    companion object {
        const val SITEID = "SITEID"
        const val TITLE = "TITLE"
        const val TYPE = "TYPE"
        const val URL = "URL"
        const val TRANSITION = 175L
    }
}
