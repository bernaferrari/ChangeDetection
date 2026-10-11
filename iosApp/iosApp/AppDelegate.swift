import UIKit
import UserNotifications
import Shared

class AppDelegate: NSObject, UIApplicationDelegate, UNUserNotificationCenterDelegate {
    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?
    ) -> Bool {
        _ = PlatformDiModule_iosKt.doInitKoinIos()
        UNUserNotificationCenter.current().delegate = self

        // Identifiers are declared in Info.plist (BGTaskSchedulerPermittedIdentifiers).
        IosBackgroundSchedulerKt.registerIosBackgroundTasks()
        return true
    }

    // Show the banner even while the app is open: the change is worth seeing either way.
    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        completionHandler([.banner, .list, .sound])
    }

    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse,
        withCompletionHandler completionHandler: @escaping () -> Void
    ) {
        let info = response.notification.request.content.userInfo
        if let siteId = info["siteId"] as? String {
            IosDeepLinks.shared.openChange(siteId: siteId, snapId: info["snapId"] as? String)
        }
        completionHandler()
    }
}
