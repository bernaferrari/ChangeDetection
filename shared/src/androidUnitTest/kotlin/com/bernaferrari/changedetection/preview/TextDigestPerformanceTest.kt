package com.bernaferrari.changedetection.preview

import org.junit.Assert.*
import org.junit.Test
import kotlin.system.measureNanoTime

/** Repeatable work/result coverage; wall-clock samples are diagnostic, never pass/fail gates. */
class TextDigestPerformanceTest {
    @Test fun largeReplacementKeepsBoundedMatchingAndStableResults() {
        for (size in listOf(40, 300, 1200)) {
            val old = (1..size).map { "Original listing $it with reserved theatre seats available today" }
            val new = (1..size).map { "Replacement listing $it with reserved theatre seats available today" }
            val expected = textDigest(old, new)
            repeat(2) { assertEquals(expected, textDigest(old, new)) }
            val samples = (1..5).map { measureNanoTime { assertEquals(expected, textDigest(old, new)) } / 1_000_000.0 }
            assertEquals(size, expected.added.size + expected.edited.size + expected.numberUpdates.size)
            assertEquals(size, expected.removed.size + expected.edited.size + expected.numberUpdates.size)
            println("DIGEST size=$size ms=$samples median=${samples.sorted()[samples.size / 2]}")
        }
    }
}
