package com.bernaferrari.changedetection

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        if (Injector.get().sharedPrefs().getBoolean(MainActivity.DARKMODE, false)) {
            setTheme(R.style.AppThemeDark)
        } else {
            setTheme(R.style.AppThemeLight)
        }

        super.onCreate(savedInstanceState)
        setContentView(R.layout.nav_frag)
    }

    companion object {
        const val DARKMODE = "dark mode"
        const val LASTCHANGE = "LASTCHANGE"
        const val SITEID = "SITEID"
        const val TITLE = "TITLE"
        const val TYPE = "TYPE"
        const val URL = "URL"
        const val TRANSITION = 175L
    }
}
