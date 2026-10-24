package com.bernaferrari.changedetection.addedit

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.bernaferrari.changedetection.image.PlatformImageDiff
import com.bernaferrari.changedetection.image.computeVisualChange
import com.bernaferrari.changedetection.sync.AndroidScreenshotBrowserFactory
import com.bernaferrari.changedetection.sync.ScreenshotCaptureOutcome
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

/**
 * Captures real pages several times through the scheduled-check path and compares each pair the way checks do.
 * Pages that did not change between captures seconds apart must not be reported as changed.
 *
 * Needs the network, so it only runs when asked:
 * -Pandroid.testInstrumentationRunnerArguments.realPages=https://a~https://b (split on ~: the device shell would read | or , as syntax)
 */
class RealPageRepeatProbe {
    @Test fun repeatedCapturesOfUnchangedPagesAreIdentical() = runBlocking {
        val urls = InstrumentationRegistry.getArguments().getString("realPages")?.split('~')?.map { it.trim() }?.filter { it.isNotBlank() }.orEmpty()
        assumeTrue("Pass realPages to run this probe", urls.isNotEmpty())
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dir = File(context.getExternalFilesDir(null), "real-page-probe").apply {
            deleteRecursively()
            mkdirs()
        }
        val report = StringBuilder()
        for (url in urls) {
            val name = url.substringAfter("://").replace(Regex("[^A-Za-z0-9]+"), "-").take(60)
            val shots = (0 until 3).map { index ->
                withContext(Dispatchers.Main.immediate) {
                    val browser = AndroidScreenshotBrowserFactory(context).create()
                    try {
                        when (val outcome = browser.capture(previewSite(url), 5_000)) {
                            is ScreenshotCaptureOutcome.Success -> outcome.bytes.also { File(dir, "$name-$index.png").writeBytes(it) }
                            is ScreenshotCaptureOutcome.Failure -> null.also { report.appendLine("PROBE $url capture $index failed: ${outcome.message}") }
                        }
                    } catch (error: Exception) {
                        // A check would record this as a failed check: it must be counted, not stop the probe.
                        null.also { report.appendLine("PROBE $url capture $index failed: ${error::class.simpleName}: ${error.message}") }
                    } finally {
                        browser.close()
                    }
                }
            }
            for (index in 1 until shots.size) {
                val before = shots[index - 1] ?: continue
                val after = shots[index] ?: continue
                // Exactly the decision a scheduled check makes, plus the explanation the reader would show.
                val diff = PlatformImageDiff.compare(before, after, includeDiffImage = false, opaqueWhiteBackdrop = true)
                val pixelsDiffer = diff.dimensionMismatch != null || diff.differentPixels > 0
                val regions = computeVisualChange(before, after, opaqueWhiteBackdrop = true)?.regions.orEmpty()
                // A check saves a version only when pixels differ and a verified area remains.
                val changed = pixelsDiffer && regions.isNotEmpty()
                report.appendLine(
                    "PROBE $url ${index - 1}->$index changed=$changed pixelsDiffer=$pixelsDiffer size=${diff.beforeWidth}x${diff.beforeHeight}->${diff.afterWidth}x${diff.afterHeight} " +
                        "pixels=${diff.differentPixels} bounds=${diff.changedBounds} regions=" +
                        regions.joinToString { "${it.kind}[${"%.3f".format(it.afterTop)}-${"%.3f".format(it.afterBottom)}]" },
                )
            }
        }
        File(dir, "report.txt").writeText(report.toString())
        println(report)
    }
}
