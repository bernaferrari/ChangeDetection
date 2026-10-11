/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.sync

import com.bernaferrari.changedetection.preview.ChangePreviewRepository
import com.bernaferrari.changedetection.repo.Site
import com.bernaferrari.changedetection.repo.source.SnapsRepository
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNUserNotificationCenter

/** Local alerts for saved changes. Tapping one opens that exact version (see [IosDeepLinks]). */
class IosChangeNotifier : ChangeNotifier {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun notifyChange(site: Site, snapId: String) {
        scope.launch {
            val koin = org.koin.mp.KoinPlatformTools.defaultContext().getOrNull()
            val (title, body) = changeAlertText(site, snapId, koin?.getOrNull<SnapsRepository>(), koin?.getOrNull<ChangePreviewRepository>())
            val content = UNMutableNotificationContent().apply {
                setTitle(title)
                setBody(body)
                setSound(UNNotificationSound.defaultSound)
                setThreadIdentifier(site.id)
                setUserInfo(mapOf(IosDeepLinks.SITE_ID to site.id, IosDeepLinks.SNAP_ID to snapId))
            }
            // One alert per page: a newer change replaces the older banner instead of stacking.
            val request = UNNotificationRequest.requestWithIdentifier(site.id, content, null)
            UNUserNotificationCenter.currentNotificationCenter().addNotificationRequest(request) { error ->
                if (error != null) Napier.w("Could not post change alert: ${error.localizedDescription}")
            }
        }
    }
}

/** Asks once; iOS remembers the answer and only shows the system prompt the first time. */
fun requestIosNotificationPermission() {
    UNUserNotificationCenter.currentNotificationCenter().requestAuthorizationWithOptions(
        UNAuthorizationOptionAlert or UNAuthorizationOptionSound or UNAuthorizationOptionBadge,
    ) { _, error -> if (error != null) Napier.w("Notification permission failed: ${error.localizedDescription}") }
}

/** Bridges notification taps (handled in Swift) into the Compose navigation. */
object IosDeepLinks {
    const val SITE_ID = "siteId"
    const val SNAP_ID = "snapId"

    private val pendingChange = MutableStateFlow<Pair<String, String?>?>(null)
    val pending: StateFlow<Pair<String, String?>?> = pendingChange

    fun openChange(siteId: String, snapId: String?) {
        pendingChange.value = siteId to snapId
    }

    fun consume() {
        pendingChange.value = null
    }
}
