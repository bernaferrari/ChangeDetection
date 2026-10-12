package com.bernaferrari.changedetection

import android.app.Activity
import kotlin.reflect.KClass

/**
 * Provider for the main activity class.
 * This allows the library module to create intents for the main activity
 * without having a direct dependency on the app module.
 */
object MainActivityProvider {
    private var mainActivityClass: Class<out Activity>? = null

    fun register(activityClass: Class<out Activity>) {
        mainActivityClass = activityClass
    }

    fun getMainActivityClass(): Class<out Activity> = mainActivityClass ?: throw IllegalStateException(
        "MainActivityProvider not initialized. Call register() in your Application.onCreate()",
    )
}
