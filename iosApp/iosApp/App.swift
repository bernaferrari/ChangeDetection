import SwiftUI

// This file is only needed if not using AppDelegate-based lifecycle.
// However, since we need BGTaskScheduler registration, AppDelegate is preferred.
//@main
struct iosApp: App {
    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
