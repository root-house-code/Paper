# Paper for iOS

Native Swift/SwiftUI port of the Android app (`../app`). Same premise: local-only,
offline, password-gated journaling with scheduled prompts. The journal JSON format is
kept byte-identical with Android so entries are portable.

See the full porting plan at `../../.claude/plans/cozy-percolating-brook.md` (or wherever
the plan file lives) for the phase breakdown.

## How this builds (no Mac used interactively)

Xcode/`codesign` only run on macOS, so **all builds happen on a macOS CI runner** — you
write Swift on Windows and push. The iOS Simulator is used *only on the CI runner* for the
unit-test pass; the app itself runs on a physical iPhone.

The Xcode project is **generated** from `project.yml` via [XcodeGen] and is not committed
(so there's no `.pbxproj` to merge-conflict on Windows). CI regenerates it each run.

### CI (already wired): `.github/workflows/ios.yml`

On every push touching `PaperiOS/**`:

1. `brew install xcodegen`
2. `xcodegen generate` → `Paper.xcodeproj`
3. `xcodebuild test` on the runner's simulator (pure-logic unit tests), no signing.

This is the fast feedback loop. If the first run reports the simulator destination is
unavailable, adjust the `name=iPhone 15` device in the workflow to one the runner image has.

## Getting a build onto the iPhone (not yet wired — needs an Apple account)

Two paths; pick one (this is the open cost decision from the plan):

- **TestFlight (recommended, $99/yr Apple Developer Program).** Add a release workflow that
  runs `xcodebuild archive` → `-exportArchive` (App Store method) → upload via
  `xcrun altool`/`fastlane pilot`. Install the TestFlight app on the iPhone. 90-day builds,
  notifications keep working — best fit for a daily-use notification app. Requires a signing
  certificate (`.p12`) + provisioning profile stored as repo secrets.
- **Free sideload ($0).** Archive with the development method to an ad-hoc `.ipa`, download to
  Windows, install with **Sideloadly** or **AltStore** (both run on Windows, sign over USB with
  a free Apple ID). Downside: **7-day expiry** — the app stops launching weekly until re-signed,
  taking its notifications with it. Fine for a quick trial, poor for daily use.

## Layout

```
project.yml            # XcodeGen spec (edit this, not the .xcodeproj)
Sources/
  PaperApp.swift       # @main entry
  UI/                  # Theme, RootView, screens
  Data/                # ScheduleConfig, PromptCategory, (auth/journal/prefs — upcoming)
  Editor/              # markdown span-finder + text ops (UITextView interop upcoming)
  Notifications/       # (upcoming) UNUserNotificationCenter scheduler
Tests/                 # pure-logic unit tests, run on CI
```

[XcodeGen]: https://github.com/yonaskolb/XcodeGen
