/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.addedit

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class PickerBridgeTest {
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
