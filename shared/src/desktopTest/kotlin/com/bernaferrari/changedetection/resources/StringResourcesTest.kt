package com.bernaferrari.changedetection.resources

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Compose resources compile even when two files define the same name, then crash the screen that reads it
 * ("Resource ... has more than one file"). Every language must also define every English string.
 */
class StringResourcesTest {
    private val root = File("src/commonMain/composeResources")
    private val name = Regex("""<(string|plurals)\s+name="([^"]+)"""")

    private fun names(folder: String): List<String> = root.resolve(folder).listFiles { f -> f.extension == "xml" }.orEmpty().flatMap { file -> name.findAll(file.readText()).map { it.groupValues[2] } }

    @Test fun noNameIsDefinedTwice() {
        root.listFiles { f -> f.isDirectory && f.name.startsWith("values") }.orEmpty().forEach { folder ->
            val duplicates = names(folder.name).groupingBy { it }.eachCount().filterValues { it > 1 }.keys
            assertTrue("${folder.name} defines more than once: $duplicates", duplicates.isEmpty())
        }
    }

    @Test fun everyLanguageHasEveryCurrentString() {
        val current = names("values").toSet()
        val locales = root.listFiles { f -> f.isDirectory && f.name.startsWith("values-") }.orEmpty()
        assertTrue("Expected eleven translated locales", locales.size == 11)
        for (folder in locales) {
            val missing = current - names(folder.name).toSet()
            assertTrue("$folder is missing: $missing", missing.isEmpty())
        }
    }

    @Test fun theWidgetAndAppUseTheSameQuickMonitorName() {
        val nativeRoot = File("src/androidMain/res")
        for (folder in root.listFiles { f -> f.isDirectory && f.name.startsWith("values") }.orEmpty()) {
            // Android resource qualifiers retain the older code for Indonesian.
            val nativeFolder = if (folder.name == "values-id") "values-in" else folder.name
            val app = checkNotNull(Regex("""<string name="turbo_mode">([^<]+)</string>""").find(folder.resolve("strings.xml").readText()))
            val widget = checkNotNull(Regex("""<string name="quick_monitor_title">([^<]+)</string>""").find(nativeRoot.resolve("$nativeFolder/strings.xml").readText()))
            assertEquals(folder.name, app.groupValues[1], widget.groupValues[1])
        }
    }
}
