/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.util

import org.junit.Assert.assertTrue
import org.junit.Test

class UnifiedDiffUtilsTest {
    @Test
    fun `keeps original context between separated changes`() {
        val original = listOf("one", "two", "three", "four", "five", "six")
        val revised = listOf("one", "TWO", "three", "four", "FIVE", "six")

        val diff = UnifiedDiffUtils.generateOriginalAndDiff(original, revised)

        assertTrue(diff.contains("-two"))
        assertTrue(diff.contains("+TWO"))
        assertTrue(diff.contains("-five"))
        assertTrue(diff.contains("+FIVE"))
        assertTrue(diff.contains(" three"))
        assertTrue(diff.contains(" four"))
    }

    @Test
    fun `keeps original context after an earlier deletion shifts revised lines`() {
        val original = listOf("one", "two", "three", "four", "five", "six")
        val revised = listOf("one", "three", "four", "FIVE", "six")

        val diff = UnifiedDiffUtils.generateOriginalAndDiff(original, revised)

        assertTrue(diff.contains("-two"))
        assertTrue(diff.contains("-five"))
        assertTrue(diff.contains("+FIVE"))
        assertTrue(diff.contains(" three"))
        assertTrue(diff.contains(" four"))
    }
}
