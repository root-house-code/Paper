# Paper

A minimalist Android journaling app built on two ideas:

1. People write more when they're **prompted** rather than left to self-start.
2. People write more honestly when they **trust their journal is private** — local only, password-protected, no cloud.

## Flow

1. **First launch** — create a password (PBKDF2-hashed; the password itself is never stored). There is no recovery by design.
2. **Set a schedule** — one of five modes:
   - Daily at a set time
   - Daily at a random time within adjustable boundaries
   - Weekly on a set day and time
   - Weekly on a set day at a random time within boundaries
   - Weekly on a random day and random time within boundaries
3. **Get nudged** — a single notification fires per occurrence, with two actions: **Write** (opens the editor, behind the password gate) or **Later** (pushes this occurrence back one hour; the regular cadence is unaffected). Random modes draw a fresh time/day for every occurrence.
4. **Write** — a Slack-style editor: bold, italic, strikethrough, inline code, code blocks, quotes, bulleted and numbered lists. Entries can be any length.
5. **Journal** — entries are stored in a single JSON file in app-private internal storage (`filesDir/journal.json`). Nothing ever leaves the device. Reading the journal in-app requires the password.

## Project layout

- [MainActivity.kt](app/src/main/java/com/paper/app/MainActivity.kt) — navigation and the unlock gate
- [data/](app/src/main/java/com/paper/app/data/) — password hashing, schedule model + next-trigger math, prompt categories, journal file repository
- [notifications/](app/src/main/java/com/paper/app/notifications/) — AlarmManager scheduling, reminder/snooze/boot receivers
- [ui/screens/](app/src/main/java/com/paper/app/ui/screens/) — setup, unlock, schedule, journal, editor, prompts screens
- [ui/editor/Markdown.kt](app/src/main/java/com/paper/app/ui/editor/Markdown.kt) — Slack-flavored markup styling for the editor and read views

## Building

Open the project root in **Android Studio** (Ladybug or newer). It will download Gradle 8.9 via the wrapper config and sync. Requires JDK 17 (bundled with Android Studio). Min SDK 26, target SDK 35.

On first run the app asks for notification permission (Android 13+). For exact-time reminders on Android 12+, grant "Alarms & reminders" in system settings; without it, Paper falls back to inexact alarms (reminders may drift by a few minutes).

## v2: encryption at rest

Today the journal is protected by app sandboxing plus the in-app password gate, but the file itself is plaintext JSON — readable if the device is rooted or the file is extracted. Planned for v2:

- Derive an AES-256 key from the user's password with the **same PBKDF2 parameters already stored** by `PasswordManager` (salt + 120k iterations were chosen with this in mind).
- Encrypt `journal.json` with **AES-256-GCM** (authenticated encryption: tampering is detected, not just hidden).
- Hold the derived key in memory only while the app is unlocked; wipe on background/lock.
- Optional: wrap the derived key with an Android Keystore key so biometric unlock can coexist with the password without weakening it.

This makes the journal unreadable even with direct local file access unless the password is entered — no password, no plaintext, including for us.

## Prompt categories

Alongside the default reminder ("Take a moment"), Paper offers five opt-in prompt categories — Goals, Dreams, Personal growth, Work, and Relationships — managed from the prompts icon on the journal screen. Each category:

- Is opted into independently — none, some, or all can be active at once.
- Keeps its own notification schedule (the same five modes above), fully independent of the default and of every other category.
- Draws a random prompt from its own pool of ten each time it fires, so the wording varies instead of repeating.

Under the hood this reuses the existing schedule model (`ScheduleConfig`/`ScheduleStore`) keyed per category, and gives each category its own alarm, notification, and snooze action so several armed at once can't collide.
