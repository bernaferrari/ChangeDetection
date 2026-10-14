package com.bernaferrari.changedetection.addedit

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class PickerBridgeTest {
    @Test fun liveBrowserUrlsSupportNativeAndJsonCallbacksWithoutFallingBackToTheDraft() {
        val live = "https://example.test/product/b?name=one%20two#/details"
        assertEquals(live, decodeBrowserUrl(live))
        assertEquals(live, decodeBrowserUrl(Json.encodeToString(live)))
        for (invalid in listOf("null", "undefined", "about:blank", "file:///private/document", "\"https://example.test/")) assertNull(decodeBrowserUrl(invalid))
    }

    @Test fun livePageCarriesItsOwnNameAndIconFromEitherCallbackShape() {
        val url = "https://example.test/product"
        val page = Json.encodeToString(listOf(url, "Product | Example", "https://example.test/icon.png"))
        val expected = LivePage(url, "Product | Example", "https://example.test/icon.png")
        assertEquals(expected, decodeLivePage(page))
        assertEquals(expected, decodeLivePage(Json.encodeToString(page)))
        assertEquals(LivePage(url, null, null), decodeLivePage(Json.encodeToString(listOf(url, " ", "javascript:alert(1)"))))
        assertEquals(LivePage(url, null, null), decodeLivePage(url))
        assertNull(decodeLivePage(Json.encodeToString(listOf("about:blank", "Blank", ""))))
    }

    @Test fun onlyBoundedReportsDuringActivePickingReachTheNativeControls() {
        val valid = PickedPart("#price", 2, 9, "Available", "div")
        assertEquals(valid, decodePickedPart(Json.encodeToString(valid), true))
        assertNull(decodePickedPart(Json.encodeToString(valid), false))
        for (invalid in listOf(valid.copy(maxDepth = Int.MAX_VALUE), valid.copy(maxDepth = -1), valid.copy(depth = -1), valid.copy(depth = 10), valid.copy(selector = ""), valid.copy(selector = "x".repeat(4097)), valid.copy(text = "x".repeat(121)), valid.copy(tag = "x".repeat(33)))) {
            assertNull(decodePickedPart(Json.encodeToString(invalid), true))
        }
        assertNull(decodePickedPart("x".repeat(8193), true))
        assertNull(decodePickedPart("not JSON", true))
    }
}
