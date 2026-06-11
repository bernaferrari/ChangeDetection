import UIKit
import Shared

@main
class AppDelegate: UIResponder, UIApplicationDelegate {
    var window: UIWindow?

    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?
    ) -> Bool {
        // Initialize Koin with iOS modules
        IosModuleKt.doInitKoinIos { _ in }
        
        // Register background tasks for periodic sync
        // Note: BGTaskScheduler requires Info.plist configuration
        let scheduler = IosBackgroundScheduler()
        scheduler.registerBackgroundTasks {
            // Called when background task is triggered
            Task {
                let syncEngine = KoinHelper.shared.getSyncEngine()
                try? await syncEngine.syncAllSites()
            }
        }
        
        return true
    }
    
    // MARK: - UISceneSession Lifecycle (iOS 13+)
    
    func application(
        _ application: UIApplication,
        configurationForConnecting connectingSceneSession: UISceneSession,
        options: UIScene.ConnectionOptions
    ) -> UISceneConfiguration {
        return UISceneConfiguration(name: "Default Configuration", sessionRole: connectingSceneSession.role)
    }
}
