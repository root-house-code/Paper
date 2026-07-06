import SwiftUI

/// Placeholder shell. The real setup → unlock → journal gate (mirroring the
/// Android `Routes`/`unlocked` state machine) lands in P1/P2; for now this just
/// proves the target builds and the theme resolves.
struct RootView: View {
    @Environment(\.colorScheme) private var systemScheme
    @EnvironmentObject private var theme: ThemeManager

    var body: some View {
        let colors = PaperColors(scheme: theme.colorScheme ?? systemScheme)
        ZStack {
            colors.background.ignoresSafeArea()
            VStack(spacing: 12) {
                Text("Paper")
                    .font(.paperSerif(40))
                    .foregroundColor(colors.onBackground)
                Text("iOS port — foundation in place.")
                    .font(.system(size: 14))
                    .foregroundColor(colors.onSurfaceVariant)
            }
        }
    }
}
