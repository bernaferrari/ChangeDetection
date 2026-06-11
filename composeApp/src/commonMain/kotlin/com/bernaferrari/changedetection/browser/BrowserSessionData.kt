/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.browser

data class BrowserSessionData(
    val url: String,
    val title: String,
    val cookies: String,
    val userAgent: String,
    val selectedElements: List<SelectedElement>,
    val zappedElements: List<ZappedElement>,
)

data class SelectedElement(
    val selector: String,
    val description: String,
    val timestamp: Long,
)

data class ZappedElement(
    val selector: String,
    val description: String,
    val timestamp: Long,
)

data class ParentElement(
    val selector: String,
    val displayName: String,
    val tagName: String,
    val className: String,
    val id: String,
    val index: Int,
)

enum class InteractionMode {
    NORMAL,
    LOGIN_CAPTURE,
    ELEMENT_SELECT,
    ELEMENT_ZAP,
}
