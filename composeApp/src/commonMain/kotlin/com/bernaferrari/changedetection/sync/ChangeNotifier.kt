/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.sync

import com.bernaferrari.changedetection.repo.Site

/** Delivers a user-facing alert after a real website change has been persisted. */
fun interface ChangeNotifier {
    /** Platform capability; runtime permission is a separate concern. */
    val supportsDelivery: Boolean get() = true

    fun notifyChange(site: Site, snapId: String)
}

/** Platforms without notification delivery still participate in the shared sync pipeline. */
object NoOpChangeNotifier : ChangeNotifier {
    override val supportsDelivery: Boolean = false
    override fun notifyChange(site: Site, snapId: String) = Unit
}
