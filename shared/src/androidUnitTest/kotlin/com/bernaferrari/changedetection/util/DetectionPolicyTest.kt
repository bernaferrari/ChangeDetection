/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.util

import com.bernaferrari.changedetection.repo.CaptureMode
import org.junit.Assert.*
import org.junit.Test

class DetectionPolicyTest {
    private fun hash(html: String, mode: CaptureMode) = ContentHasher.detectionHash(html.toByteArray(), "text/html", mode)

    @Test fun `readable text ignores markup scripts and explicitly hidden content`() {
        val before = "<style>p{color:red}</style><p class='old'>Price: 100</p><script>nonce=1</script><div hidden>A</div>"
        val after = "<style>p{color:blue}</style><p class='new'>Price: 100</p><script>nonce=2</script><div hidden>B</div>"
        assertEquals(hash(before, CaptureMode.TEXT), hash(after, CaptureMode.TEXT))
        assertNotEquals(hash(before, CaptureMode.SOURCE), hash(after, CaptureMode.SOURCE))
    }

    @Test fun `same length readable replacements and reordered words are detected`() {
        assertNotEquals(hash("<p>Price: 100</p>", CaptureMode.TEXT), hash("<p>Price: 200</p>", CaptureMode.TEXT))
        assertNotEquals(hash("<p>red blue red</p>", CaptureMode.TEXT), hash("<p>blue red red</p>", CaptureMode.TEXT))
    }

    @Test fun `source mode includes script nonce and whitespace changes`() {
        listOf("<p>A</p><script>v=2</script>", "<p nonce='2'>A</p><script>v=1</script>", "<p>A </p><script>v=1</script>").forEach {
            assertNotEquals(hash("<p>A</p><script>v=1</script>", CaptureMode.SOURCE), hash(it, CaptureMode.SOURCE))
        }
    }

    @Test fun `legacy hashes stay compatible and policies have distinct identities`() {
        val html = "<p>Hello</p>"
        assertEquals(ContentHasher.hashHtmlContent(html.toByteArray()), hash(html, CaptureMode.HTML))
        assertEquals("legacy", ContentHasher.detectionPolicy(hash(html, CaptureMode.DEFAULT)))
        assertEquals("text-v1", ContentHasher.detectionPolicy(hash(html, CaptureMode.TEXT)))
        assertEquals("source-v1", ContentHasher.detectionPolicy(hash(html, CaptureMode.SOURCE)))
    }
}
