import UIKit
import Shared

@main
class AppDelegate: UIResponder, UIApplicationDelegate {
    var window: UIWindow?

    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?
    ) -> Bool {
        // Initialize Koin
        KoinKt.doInitKoin { _ in }
        
        // Register background tasks
        let scheduler = IosBackgroundScheduler()
        scheduler.registerBackgroundTasks {
            // This is called when the background task is triggered
            let syncEngine = KoinKt.getSyncEngine() 
            syncEngine.syncAllSites { _, _ in }
        }
        
        return true
    }
}
