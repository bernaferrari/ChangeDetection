/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.navigation

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey

/** Replace an open detail instead of building a trail of sibling details behind it. */
fun NavBackStack<NavKey>.openComparison(
    siteId: String,
    snapId: String? = null,
    showHistory: Boolean = false,
) {
    val destination = ComparisonDestination(siteId, snapId, showHistory)
    if (lastOrNull() is ComparisonDestination) {
        this[lastIndex] = destination
    } else {
        add(destination)
    }
}

/** Selecting a sibling page replaces its overview; comparison remains a separate child. */
fun NavBackStack<NavKey>.openPage(siteId: String) {
    val destination = PageDetailDestination(siteId)
    if (lastOrNull() is ComparisonDestination && getOrNull(lastIndex - 1) == destination) {
        removeAt(lastIndex)
    } else if (lastOrNull() is PageDetailDestination || lastOrNull() is ComparisonDestination) {
        this[lastIndex] = destination
    } else {
        add(destination)
    }
}
