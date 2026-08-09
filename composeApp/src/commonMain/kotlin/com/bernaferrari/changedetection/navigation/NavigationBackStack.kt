/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.navigation

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey

/** Replace an open detail instead of building a trail of sibling details behind it. */
fun NavBackStack<NavKey>.openComparison(siteId: String) {
    val destination = ComparisonDestination(siteId)
    if (lastOrNull() is ComparisonDestination) {
        this[lastIndex] = destination
    } else {
        add(destination)
    }
}
