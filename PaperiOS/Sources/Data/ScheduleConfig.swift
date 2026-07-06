import Foundation

/// Faithful Swift port of the Android `ScheduleMode` / `ScheduleConfig`
/// (`data/ScheduleConfig.kt`). The next-trigger math is the same; only the
/// date primitives change (java.time → Foundation `Calendar`).
///
/// Day-of-week uses the **ISO** convention (1 = Monday … 7 = Sunday), same as
/// the Android model, and is converted to Foundation's weekday (1 = Sunday) only
/// at the boundary.
enum ScheduleMode: String, Codable, CaseIterable {
    case dailyFixed
    case dailyRandom
    case weeklyFixed
    case weeklyFixedDayRandomTime
    case weeklyRandom
    case custom
}

struct ScheduleConfig: Codable, Equatable {
    var mode: ScheduleMode
    var fixedMinuteOfDay: Int = 9 * 60
    var windowStartMinute: Int = 9 * 60
    var windowEndMinute: Int = 21 * 60
    /// ISO day of week 1 (Mon) … 7 (Sun). Used by the WEEKLY_FIXED* modes.
    var dayOfWeek: Int = 7
    /// ISO day of week -> minute of day. Only enabled days are present. Used by `.custom`.
    var customDayMinutes: [Int: Int] = [:]

    /// Next trigger strictly after `after`. Random modes draw fresh values on
    /// every call (via `rng`), so each occurrence is independently random within
    /// the user's window — matching the Android behavior. `rng` is injectable so
    /// tests can make the random modes deterministic.
    func nextTriggerDate(after: Date = Date(),
                         calendar: Calendar = .current,
                         rng: () -> Double = { Double.random(in: 0..<1) }) -> Date {
        switch mode {
        case .dailyFixed:
            return nextDaily(after: after, calendar: calendar) { fixedMinuteOfDay }

        case .dailyRandom:
            return nextRandomInWindow(after: after, calendar: calendar, daysAhead: 1, rng: rng)

        case .weeklyFixed:
            return nextWeekly(after: after, calendar: calendar, isoDay: dayOfWeek) { fixedMinuteOfDay }

        case .weeklyFixedDayRandomTime:
            if isoWeekday(of: after, calendar: calendar) == dayOfWeek {
                // Target day is today: draw from what's left of the window; if it
                // has closed, daysAhead = 7 lands on next week's same day.
                return nextRandomInWindow(after: after, calendar: calendar, daysAhead: 7, rng: rng)
            }
            return nextWeekly(after: after, calendar: calendar, isoDay: dayOfWeek) {
                randomMinuteInWindow(rng: rng)
            }

        case .weeklyRandom:
            // Pick a random day in the next 7, then a random time in the window;
            // fall forward if the pick already passed.
            var candidate = after
            repeat {
                let offset = Int(rng() * 7).clamped(to: 0...6)
                let day = calendar.date(byAdding: .day, value: offset, to: after)!
                candidate = date(onDayOf: day, minute: randomMinuteInWindow(rng: rng), calendar: calendar)
            } while candidate <= after
            return candidate

        case .custom:
            // Soonest occurrence across every enabled day's own time.
            let candidates = customDayMinutes.map { isoDay, minute in
                nextWeekly(after: after, calendar: calendar, isoDay: isoDay) { minute }
            }
            return candidates.min() ?? calendar.date(byAdding: .year, value: 100, to: after)!
        }
    }

    // MARK: - Helpers (ports of the Kotlin private functions)

    private func randomMinuteInWindow(notBefore: Int = 0, rng: () -> Double) -> Int {
        let start = max(min(windowStartMinute, windowEndMinute), notBefore)
        let end = max(windowStartMinute, windowEndMinute)
        if start >= end { return end }
        // Inclusive of both bounds, matching Kotlin's Random.nextInt(start, end + 1).
        let span = end - start
        return start + Int(rng() * Double(span + 1)).clamped(to: 0...span)
    }

    private func nextRandomInWindow(after: Date, calendar: Calendar, daysAhead: Int, rng: () -> Double) -> Date {
        let afterMinute = minuteOfDay(of: after, calendar: calendar)
        let todayStillOpen = afterMinute < max(windowStartMinute, windowEndMinute)
        if todayStillOpen {
            return date(onDayOf: after,
                        minute: randomMinuteInWindow(notBefore: afterMinute + 1, rng: rng),
                        calendar: calendar)
        } else {
            let day = calendar.date(byAdding: .day, value: daysAhead, to: after)!
            return date(onDayOf: day, minute: randomMinuteInWindow(rng: rng), calendar: calendar)
        }
    }

    private func nextDaily(after: Date, calendar: Calendar, minute: () -> Int) -> Date {
        let today = date(onDayOf: after, minute: minute(), calendar: calendar)
        if today > after { return today }
        let tomorrow = calendar.date(byAdding: .day, value: 1, to: after)!
        return date(onDayOf: tomorrow, minute: minute(), calendar: calendar)
    }

    private func nextWeekly(after: Date, calendar: Calendar, isoDay: Int, minute: () -> Int) -> Date {
        var day = after
        while isoWeekday(of: day, calendar: calendar) != isoDay {
            day = calendar.date(byAdding: .day, value: 1, to: day)!
        }
        let candidate = date(onDayOf: day, minute: minute(), calendar: calendar)
        if candidate > after { return candidate }
        let nextWeek = calendar.date(byAdding: .day, value: 7, to: day)!
        return date(onDayOf: nextWeek, minute: minute(), calendar: calendar)
    }

    private func minuteOfDay(of date: Date, calendar: Calendar) -> Int {
        let c = calendar.dateComponents([.hour, .minute], from: date)
        return (c.hour ?? 0) * 60 + (c.minute ?? 0)
    }

    /// Start of the given day plus `minute` minutes — unambiguous vs. `bySettingHour`,
    /// which can roll forward when the time already passed.
    private func date(onDayOf date: Date, minute: Int, calendar: Calendar) -> Date {
        let start = calendar.startOfDay(for: date)
        return calendar.date(byAdding: .minute, value: minute, to: start)!
    }

    /// Foundation weekday (1 = Sun … 7 = Sat) -> ISO (1 = Mon … 7 = Sun).
    private func isoWeekday(of date: Date, calendar: Calendar) -> Int {
        let w = calendar.component(.weekday, from: date)
        return w == 1 ? 7 : w - 1
    }
}

private extension Comparable {
    func clamped(to range: ClosedRange<Self>) -> Self {
        min(max(self, range.lowerBound), range.upperBound)
    }
}
