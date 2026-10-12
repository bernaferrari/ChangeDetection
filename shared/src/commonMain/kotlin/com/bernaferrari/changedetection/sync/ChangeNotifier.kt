package com.bernaferrari.changedetection.sync

import com.bernaferrari.changedetection.repo.Site

/** Completion means the saved change alert has been submitted, or delivery is unavailable. */
fun interface ChangeNotifier {
    /** Platform capability; runtime permission is a separate concern. */
    val supportsDelivery: Boolean get() = true

    suspend fun notifyChange(site: Site, snapId: String)
}

/** Platforms without notification delivery still participate in the shared sync pipeline. */
object NoOpChangeNotifier : ChangeNotifier {
    override val supportsDelivery: Boolean = false
    override suspend fun notifyChange(site: Site, snapId: String) = Unit
}
