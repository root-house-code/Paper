import SwiftUI

/// Minimalist, paper-like palette ported verbatim from the Android theme
/// (`ui/theme/Theme.kt`): near-black ink on warm off-white, no accent noise.
enum PaperPalette {
    // Light
    static let ink = Color(hex: 0x1A1A1A)
    static let paperWhite = Color(hex: 0xFAF8F5)
    static let faintLine = Color(hex: 0xE5E1DB)
    static let softGray = Color(hex: 0x8A857E)
    // Dark
    static let nightInk = Color(hex: 0xE8E6E3)
    static let nightPaper = Color(hex: 0x141414)
    static let nightLine = Color(hex: 0x2A2A2A)
}

/// Semantic colors resolved against the active color scheme — mirrors the
/// Material `colorScheme` roles the Android screens read.
struct PaperColors {
    let scheme: ColorScheme

    var background: Color { scheme == .dark ? PaperPalette.nightPaper : PaperPalette.paperWhite }
    var onBackground: Color { scheme == .dark ? PaperPalette.nightInk : PaperPalette.ink }
    var onSurfaceVariant: Color { PaperPalette.softGray }
    var outline: Color { scheme == .dark ? PaperPalette.nightLine : PaperPalette.faintLine }
    /// Text/icon color on top of the inked primary button.
    var onPrimary: Color { scheme == .dark ? PaperPalette.nightPaper : PaperPalette.paperWhite }
    var primary: Color { onBackground }
}

/// Persists the light/dark override (nil = follow system), matching the Android
/// `ThemePreference` toggle behavior.
final class ThemeManager: ObservableObject {
    private let key = "dark_mode"
    private let defaults: UserDefaults

    /// nil = follow system; true/false = explicit user override.
    @Published var darkModeOverride: Bool? {
        didSet {
            if let v = darkModeOverride {
                defaults.set(v, forKey: key)
            } else {
                defaults.removeObject(forKey: key)
            }
        }
    }

    init(defaults: UserDefaults = .standard) {
        self.defaults = defaults
        self.darkModeOverride = defaults.object(forKey: key) as? Bool
    }

    /// SwiftUI `preferredColorScheme` value (nil follows the system).
    var colorScheme: ColorScheme? {
        guard let v = darkModeOverride else { return nil }
        return v ? .dark : .light
    }

    /// Flips relative to what's currently showing, then persists the explicit choice.
    func toggle(current: ColorScheme) {
        let isDark = darkModeOverride ?? (current == .dark)
        darkModeOverride = !isDark
    }
}

extension Font {
    /// Serif for headlines/titles, matching Android's `FontFamily.Serif`.
    static func paperSerif(_ size: CGFloat, weight: Font.Weight = .regular) -> Font {
        .system(size: size, weight: weight, design: .serif)
    }
}

extension Color {
    init(hex: UInt32) {
        self.init(
            red: Double((hex >> 16) & 0xFF) / 255.0,
            green: Double((hex >> 8) & 0xFF) / 255.0,
            blue: Double(hex & 0xFF) / 255.0
        )
    }
}
