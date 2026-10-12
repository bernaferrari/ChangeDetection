package com.bernaferrari.changedetection.sync

import com.bernaferrari.changedetection.preview.ChangePreviewRepository
import com.bernaferrari.changedetection.repo.Site
import com.bernaferrari.changedetection.repo.source.SnapsRepository
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNUserNotificationCenter

/** Local alerts for saved changes. Tapping one opens that exact version (see [IosDeepLinks]). */
class IosChangeNotifier : ChangeNotifier {
    private val delivery = ChangeAlertDelivery(prepare = { site, snapId ->
        val koin = org.koin.mp.KoinPlatformTools.defaultContext().getOrNull()
        changeAlertText(site, snapId, koin?.getOrNull<SnapsRepository>(), koin?.getOrNull<ChangePreviewRepository>()).second
    }, publish = { site, snapId, summary ->
        val (title, fallback) = changeAlertText(site, snapId, null, null)
        val content = UNMutableNotificationContent().apply {
            setTitle(title)
            setBody(summary ?: fallback)
            setSound(UNNotificationSound.defaultSound)
            setThreadIdentifier(site.id)
            setUserInfo(mapOf(IosDeepLinks.SITE_ID to site.id, IosDeepLinks.SNAP_ID to snapId))
        }
        val request = UNNotificationRequest.requestWithIdentifier(site.id, content, null)
        kotlinx.coroutines.suspendCancellableCoroutine<Unit> { continuation ->
            UNUserNotificationCenter.currentNotificationCenter().addNotificationRequest(request) { error ->
                if (continuation.isActive) {
                    continuation.resumeWith(if (error == null) Result.success(Unit) else Result.failure(IllegalStateException(error.localizedDescription)))
                }
            }
        }
    })

    override suspend fun notifyChange(site: Site, snapId: String) = delivery.deliver(site, snapId)
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
