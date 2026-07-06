import SwiftUI

@main
struct PaperApp: App {
    @StateObject private var theme = ThemeManager()

    var body: some Scene {
        WindowGroup {
            RootView()
                .environmentObject(theme)
                .preferredColorScheme(theme.colorScheme)
        }
    }
}
