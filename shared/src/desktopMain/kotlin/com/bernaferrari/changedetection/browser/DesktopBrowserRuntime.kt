package com.bernaferrari.changedetection.browser

import dev.datlag.kcef.KCEF
import java.io.File
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Process-scoped: opening another view or hot-reloading composition must not reinitialize CEF. */
internal object DesktopBrowserRuntime {
    enum class State { Idle, Starting, Ready, Failed }
    private val mutableState = MutableStateFlow(State.Idle)
    val state = mutableState.asStateFlow()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @OptIn(dev.datlag.kcef.KCEFAcknowledge::class)
    fun start(directory: File = File(System.getProperty("user.home"), ".change-detection-desktop/browser")) {
        if (!mutableState.compareAndSet(State.Idle, State.Starting)) return
        scope.launch {
            delay(20_000)
            mutableState.compareAndSet(State.Starting, State.Failed)
        }
        scope.launch {
            try {
                if (org.cef.CefApp::class.java.module.name == "jcef") {
                    val config = com.jetbrains.cef.JCefAppConfig.getInstance()
                    org.cef.CefApp.addAppHandler(object : org.cef.handler.CefAppHandlerAdapter(config.appArgs) {})
                    check(org.cef.CefApp.startup(config.appArgs)) { "Bundled browser startup failed" }
                    val settings = config.cefSettings.apply {
                        cache_path = File(directory, "cache").absolutePath
                    }
                    val app = org.cef.CefApp.getInstance(settings)
                    app.onInitialization({ nativeState ->
                        if (nativeState == org.cef.CefApp.CefAppState.INITIALIZED) {
                            KCEF.init(app)
                            markInitialized()
                        }
                    }, false)
                    return@launch
                }
                KCEF.init(
                    builder = {
                        installDir(File(directory, "runtime"))
                        settings { cachePath = File(directory, "cache").absolutePath }
                        progress { onInitialized { markInitialized() } }
                    },
                    onError = { failure ->
                        appLog.e("Desktop browser initialization failed", failure)
                        mutableState.value = State.Failed
                    },
                    onRestartRequired = { mutableState.value = State.Failed },
                )
                // Only an actual initialization callback may recover a timeout or failure.
                mutableState.compareAndSet(State.Starting, State.Ready)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Throwable) {
                appLog.e("Desktop browser initialization failed", failure)
                mutableState.value = State.Failed
            }
        }
    }

    private fun markInitialized() {
        // A timeout is a UI deadline; initialization may still finish successfully afterward.
        mutableState.update { if (it == State.Starting || it == State.Failed) State.Ready else it }
    }

    fun dispose() {
        scope.cancel()
        val wasStarted = mutableState.value != State.Idle
        mutableState.value = State.Idle
        if (wasStarted) org.cef.CefApp.getInstanceIfAny()?.dispose()
    }
}

private val appLog = com.bernaferrari.changedetection.logging.AppLog.tagged("DesktopBrowserRuntime")
