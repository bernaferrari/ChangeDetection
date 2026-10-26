package com.bernaferrari.changedetection.addedit

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.bernaferrari.changedetection.diagnostics.*
import com.bernaferrari.changedetection.repo.CaptureMode
import com.bernaferrari.changedetection.sync.AndroidScreenshotBrowserFactory
import com.bernaferrari.changedetection.sync.ScreenshotCaptureOutcome
import kotlinx.coroutines.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

/** Explicit opt-in public-site investigation. No snapshots/page contents or cookies are written to evidence. */
class LiveSiteDiagnosticProbe {
    @Test fun screenshotAndTextCaptureClassifications() = runBlocking {
        assumeTrue("Pass liveDiagnostics=true for this network probe", InstrumentationRegistry.getArguments().getString("liveDiagnostics") == "true")
        val context = ApplicationProvider.getApplicationContext<Context>()
        val factory = AndroidScreenshotBrowserFactory(context)
        val prior = DiagnosticLog.sink
        val temporary = File(context.cacheDir, "live-diag-${java.util.UUID.randomUUID()}")
        val args = InstrumentationRegistry.getArguments()
        val web = android.webkit.WebView.getCurrentWebViewPackage()
        val store = AndroidDiagnosticStore(context, temporary, runtime = {
            DiagnosticRuntime(args.getString("probeVersionCode")?.toLongOrNull() ?: 0, "3.0", DiagnosticVariant.DEBUG, args.getString("probeRevision") ?: "unavailable", android.os.Build.VERSION.SDK_INT, if (web?.packageName == "com.google.android.webview") DiagnosticProvider.GOOGLE else DiagnosticProvider.OTHER, web?.versionName.orEmpty(), DiagnosticSpace.UNKNOWN, (Runtime.getRuntime().maxMemory() / 1048576).toInt())
        })
        val summary = mutableListOf<String>()
        try {
            DiagnosticLog.sink = store
            for ((label, url) in listOf("snfcc" to "https://www.ticketservices.gr/el/snfcc/", "onassis" to "https://tickets.onassis.org/el/Organizer/sgt/Portal")) {
                for (mode in listOf(CaptureMode.SCREENSHOT, CaptureMode.SCREENSHOT, CaptureMode.TEXT)) {
                    val result = runCatching {
                        recordDiagnostic(mode, false, false, DiagnosticTrigger.CHECK, "probe-$label") {
                            withTimeout(50_000) {
                                withContext(Dispatchers.Main.immediate) {
                                    val browser = factory.create()
                                    try {
                                        val outcome = browser.capture(previewSite(url).copy(captureMode = mode), 5_000)
                                        when (outcome) {
                                            is ScreenshotCaptureOutcome.Success -> {
                                                outcome.validateCapture()
                                                outcome.bytes.size
                                            }

                                            is ScreenshotCaptureOutcome.Failure -> error("Capture returned failure")
                                        }
                                    } finally {
                                        browser.close()
                                    }
                                }
                            }
                        }
                    }
                    summary += "$label ${mode.name} ${result.fold({ "SUCCESS bytes=$it" }, { "FAILURE class=${it::class.simpleName} kind=${exceptionKind(it).name}" })}"
                    println("LIVE_DIAGNOSTIC ${summary.last()}")
                }
            }
            val dir = File(context.getExternalFilesDir(null), "live-diagnostic-probe").apply { mkdirs() }
            File(dir, "summary.txt").writeText(summary.joinToString("\n"))
            File(dir, "sanitized-report.json").writeText(store.report().json)
        } finally {
            DiagnosticLog.sink = prior
            store.close()
            temporary.deleteRecursively()
        }
    }
}
