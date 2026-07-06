import XCTest
@testable import Paper

/// Pins the ported next-trigger math against the Android behavior. A fixed UTC
/// calendar makes the deterministic modes exact; random modes use a constant
/// `rng` so their output is also deterministic.
final class ScheduleConfigTests: XCTestCase {

    private func utc() -> Calendar {
        var c = Calendar(identifier: .gregorian)
        c.timeZone = TimeZone(identifier: "UTC")!
        return c
    }

    private func date(_ cal: Calendar, _ y: Int, _ mo: Int, _ d: Int, _ h: Int, _ mi: Int) -> Date {
        var c = DateComponents()
        c.year = y; c.month = mo; c.day = d; c.hour = h; c.minute = mi
        return cal.date(from: c)!
    }

    private let half: () -> Double = { 0.5 }

    // 2026-01-01 is a Thursday → ISO weekday 4.

    func testDailyFixedLaterToday() {
        let cal = utc()
        let cfg = ScheduleConfig(mode: .dailyFixed, fixedMinuteOfDay: 9 * 60)
        let after = date(cal, 2026, 1, 1, 8, 0)
        XCTAssertEqual(cfg.nextTriggerDate(after: after, calendar: cal), date(cal, 2026, 1, 1, 9, 0))
    }

    func testDailyFixedRollsToTomorrow() {
        let cal = utc()
        let cfg = ScheduleConfig(mode: .dailyFixed, fixedMinuteOfDay: 9 * 60)
        let after = date(cal, 2026, 1, 1, 10, 0)
        XCTAssertEqual(cfg.nextTriggerDate(after: after, calendar: cal), date(cal, 2026, 1, 2, 9, 0))
    }

    func testWeeklyFixedSameDayLater() {
        let cal = utc()
        let cfg = ScheduleConfig(mode: .weeklyFixed, fixedMinuteOfDay: 9 * 60, dayOfWeek: 4)
        let after = date(cal, 2026, 1, 1, 8, 0) // Thursday
        XCTAssertEqual(cfg.nextTriggerDate(after: after, calendar: cal), date(cal, 2026, 1, 1, 9, 0))
    }

    func testWeeklyFixedRollsToNextWeek() {
        let cal = utc()
        let cfg = ScheduleConfig(mode: .weeklyFixed, fixedMinuteOfDay: 9 * 60, dayOfWeek: 4)
        let after = date(cal, 2026, 1, 1, 10, 0) // Thursday, past 9am
        XCTAssertEqual(cfg.nextTriggerDate(after: after, calendar: cal), date(cal, 2026, 1, 8, 9, 0))
    }

    func testCustomPicksSoonestEnabledDay() {
        let cal = utc()
        // Thursday 10:00 and Saturday 09:00 enabled; from Thursday 08:00 the soonest is Thu 10:00.
        let cfg = ScheduleConfig(mode: .custom, customDayMinutes: [4: 10 * 60, 6: 9 * 60])
        let after = date(cal, 2026, 1, 1, 8, 0)
        XCTAssertEqual(cfg.nextTriggerDate(after: after, calendar: cal), date(cal, 2026, 1, 1, 10, 0))
    }

    func testDailyRandomIsDeterministicWithFixedRng() {
        let cal = utc()
        // Window 09:00–21:00; from 08:00 with rng 0.5 → 540 + floor(0.5*721) = 900 → 15:00.
        let cfg = ScheduleConfig(mode: .dailyRandom, windowStartMinute: 9 * 60, windowEndMinute: 21 * 60)
        let after = date(cal, 2026, 1, 1, 8, 0)
        XCTAssertEqual(cfg.nextTriggerDate(after: after, calendar: cal, rng: half), date(cal, 2026, 1, 1, 15, 0))
    }

    func testWeeklyRandomIsDeterministicWithFixedRng() {
        let cal = utc()
        // rng 0.5 → day offset floor(0.5*7)=3 (Thu+3 = Sun Jan 4), time 15:00.
        let cfg = ScheduleConfig(mode: .weeklyRandom, windowStartMinute: 9 * 60, windowEndMinute: 21 * 60)
        let after = date(cal, 2026, 1, 1, 8, 0)
        XCTAssertEqual(cfg.nextTriggerDate(after: after, calendar: cal, rng: half), date(cal, 2026, 1, 4, 15, 0))
    }

    func testRandomStaysWithinWindowAcrossManyDraws() {
        let cal = utc()
        let cfg = ScheduleConfig(mode: .dailyRandom, windowStartMinute: 9 * 60, windowEndMinute: 21 * 60)
        let after = date(cal, 2026, 1, 1, 0, 0) // before the window opens
        for _ in 0..<200 {
            let d = cfg.nextTriggerDate(after: after, calendar: cal)
            let c = cal.dateComponents([.hour, .minute], from: d)
            let minute = c.hour! * 60 + c.minute!
            XCTAssertGreaterThanOrEqual(minute, 9 * 60)
            XCTAssertLessThanOrEqual(minute, 21 * 60)
            XCTAssertGreaterThan(d, after)
        }
    }
}
