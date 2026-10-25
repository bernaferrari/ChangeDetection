package com.bernaferrari.changedetection.platform

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertTrue
import org.junit.Test

class PlatformShareFailureTest {
    @Test fun unavailableChooserIsReportedToTheCaller() {
        val context = object : ContextWrapper(ApplicationProvider.getApplicationContext<Context>()) {
            override fun startActivity(intent: Intent) { throw ActivityNotFoundException("No handler in fixture") }
        }
        val failure = runCatching { AndroidShareManager(context).shareText("fixture", "fixture") }.exceptionOrNull()
        assertTrue(failure is ShareUnavailableException)
    }
}
