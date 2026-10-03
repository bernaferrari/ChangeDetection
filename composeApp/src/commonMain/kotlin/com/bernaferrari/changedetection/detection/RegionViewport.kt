/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.detection

internal data class RegionViewport(val x: Int = 0, val y: Int = 0) {
    init {
        require(x in 0..10_000_000 && y in 0..10_000_000)
    }
}

/** Browser messages contain bounded integers only, never executable source or external URLs. */
internal fun regionViewportFromNavigation(url: String): RegionViewport? {
    val prefix = "https://selection.changedetection.invalid/"
    if (!url.startsWith(prefix)) return null
    val parts = url.removePrefix(prefix).split('/')
    val coordinates = when {
        parts.size == 3 && parts[0] == "view" -> parts.drop(1)
        parts.size == 4 && parts[0] == "pick" && parts[1].isNotEmpty() && parts[1].all { it in '0'..'9' } && parts[1].toIntOrNull() != null -> parts.drop(2)
        else -> return null
    }
    val values = coordinates.map { part ->
        if (part.isEmpty() || part.any { it !in '0'..'9' }) return null
        part.toIntOrNull()?.takeIf { it in 0..10_000_000 } ?: return null
    }
    return RegionViewport(values[0], values[1])
}
