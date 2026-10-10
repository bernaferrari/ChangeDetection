/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.resources

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Compose resources compile even when two files define the same name, then crash the screen that reads it
 * ("Resource ... has more than one file"). Every language must also define every English string.
 */
class StringResourcesTest {
    private val root = File("src/commonMain/composeResources")
    private val name = Regex("""<(string|plurals)\s+name="([^"]+)"""")

    private fun names(folder: String): List<String> =
        root.resolve(folder).listFiles { f -> f.extension == "xml" }.orEmpty().flatMap { file -> name.findAll(file.readText()).map { it.groupValues[2] } }

    @Test fun noNameIsDefinedTwice() {
        root.listFiles { f -> f.isDirectory && f.name.startsWith("values") }.orEmpty().forEach { folder ->
            val duplicates = names(folder.name).groupingBy { it }.eachCount().filterValues { it > 1 }.keys
            assertTrue("${folder.name} defines more than once: $duplicates", duplicates.isEmpty())
        }
    }

    @Test fun everyLanguageHasEveryCurrentString() {
        // Older files predate full translation; the current string file must be complete in every language.
        val current = name.findAll(root.resolve("values/strings_v7.xml").readText()).map { it.groupValues[2] }.toSet()
        listOf("values-es", "values-it", "values-pt").forEach { folder ->
            val missing = current - names(folder).toSet()
            assertTrue("$folder is missing: $missing", missing.isEmpty())
        }
    }
}
