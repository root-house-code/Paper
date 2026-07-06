package com.paper.app.data

import android.content.Context
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import kotlin.random.Random

enum class ScheduleMode {
    DAILY_FIXED,          // every day at a set time
    DAILY_RANDOM,         // every day at a random time within bounds
    WEEKLY_FIXED,         // set day, set time
    WEEKLY_FIXED_DAY_RANDOM_TIME, // set day, random time within bounds
    WEEKLY_RANDOM,        // random day, random time within bounds
    CUSTOM                // a chosen time for each selected day of the week
}

data class ScheduleConfig(
    val mode: ScheduleMode,
    /** Fixed time-of-day, minutes from midnight. Used by *_FIXED modes. */
    val fixedMinuteOfDay: Int = 9 * 60,
    /** Random window boundaries, minutes from midnight. Used by *_RANDOM modes. */
    val windowStartMinute: Int = 9 * 60,
    val windowEndMinute: Int = 21 * 60,
    /** ISO day of week 1 (Mon) .. 7 (Sun). Used by WEEKLY_FIXED* modes. */
    val dayOfWeek: Int = DayOfWeek.SUNDAY.value,
    /** ISO day of week -> minute of day. Only enabled days are present. Used by CUSTOM. */
    val customDayMinutes: Map<Int, Int> = emptyMap()
) {

    /**
     * Computes the next trigger strictly after [after]. Random modes draw a fresh
     * time (and day, for WEEKLY_RANDOM) on every call, so each occurrence is
     * independently random within the user's boundaries.
     */
    fun nextTrigger(after: LocalDateTime = LocalDateTime.now()): LocalDateTime {
        return when (mode) {
            ScheduleMode.DAILY_FIXED ->
                nextDaily(after) { LocalTime.ofSecondOfDay(fixedMinuteOfDay * 60L) }

            ScheduleMode.DAILY_RANDOM ->
                nextRandomInWindow(after, daysAhead = 1)

            ScheduleMode.WEEKLY_FIXED ->
                nextWeekly(after, DayOfWeek.of(dayOfWeek)) {
                    LocalTime.ofSecondOfDay(fixedMinuteOfDay * 60L)
                }

            ScheduleMode.WEEKLY_FIXED_DAY_RANDOM_TIME -> {
                val day = DayOfWeek.of(dayOfWeek)
                if (after.dayOfWeek == day) {
                    // Target day is today: draw from what's left of the window;
                    // if it has closed, daysAhead = 7 lands on next week's same day.
                    nextRandomInWindow(after, daysAhead = 7)
                } else {
                    nextWeekly(after, day) { randomTimeInWindow() }
                }
            }

            ScheduleMode.WEEKLY_RANDOM -> {
                // Pick a random day in the next 7 days, then a random time in the
                // window. If the pick already passed, fall forward to a later day.
                var candidate: LocalDateTime
                do {
                    val day = after.toLocalDate().plusDays(Random.nextLong(0, 7))
                    candidate = LocalDateTime.of(day, randomTimeInWindow())
                } while (!candidate.isAfter(after))
                candidate
            }

            ScheduleMode.CUSTOM ->
                // Soonest occurrence across every enabled day's own time. The UI
                // requires at least one entry before a CUSTOM schedule can be saved.
                customDayMinutes.entries
                    .map { (day, minute) ->
                        nextWeekly(after, DayOfWeek.of(day)) {
                            LocalTime.ofSecondOfDay(minute * 60L)
                        }
                    }
                    .minOrNull() ?: after.plusYears(100)
        }
    }

    fun nextTriggerMillis(after: LocalDateTime = LocalDateTime.now()): Long =
        nextTrigger(after).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    private fun randomTimeInWindow(notBeforeMinute: Int = 0): LocalTime {
        val start = maxOf(minOf(windowStartMinute, windowEndMinute), notBeforeMinute)
        val end = maxOf(windowStartMinute, windowEndMinute)
        val minute = if (start >= end) end else Random.nextInt(start, end + 1)
        return LocalTime.ofSecondOfDay(minute * 60L)
    }

    /**
     * Random draw over the next [daysAhead] day(s). If today's window hasn't
     * fully passed, today stays in play — the draw is over what remains of it
     * rather than skipping straight to tomorrow.
     */
    private fun nextRandomInWindow(after: LocalDateTime, daysAhead: Long): LocalDateTime {
        val afterMinute = after.hour * 60 + after.minute
        val todayStillOpen = afterMinute < maxOf(windowStartMinute, windowEndMinute)
        return if (todayStillOpen) {
            LocalDateTime.of(after.toLocalDate(), randomTimeInWindow(afterMinute + 1))
        } else {
            LocalDateTime.of(after.toLocalDate().plusDays(daysAhead), randomTimeInWindow())
        }
    }

    private fun nextDaily(after: LocalDateTime, time: () -> LocalTime): LocalDateTime {
        val today = LocalDateTime.of(after.toLocalDate(), time())
        return if (today.isAfter(after)) today
        else LocalDateTime.of(after.toLocalDate().plusDays(1), time())
    }

    private fun nextWeekly(
        after: LocalDateTime,
        day: DayOfWeek,
        time: () -> LocalTime
    ): LocalDateTime {
        var date: LocalDate = after.toLocalDate()
        while (date.dayOfWeek != day) date = date.plusDays(1)
        val candidate = LocalDateTime.of(date, time())
        return if (candidate.isAfter(after)) candidate
        else LocalDateTime.of(date.plusWeeks(1), time())
    }
}

/** Persists schedules so BootReceiver and ReminderReceiver can re-arm alarms. Each prompt
 *  category gets its own key alongside the default; DEFAULT_KEY reproduces the original
 *  unprefixed keys so existing installs' saved schedule loads unchanged. */
object ScheduleStore {
    private const val PREFS = "paper_schedule"
    const val DEFAULT_KEY = ""

    fun save(context: Context, config: ScheduleConfig, key: String = DEFAULT_KEY) {
        val p = prefix(key)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString("${p}mode", config.mode.name)
            .putInt("${p}fixedMinuteOfDay", config.fixedMinuteOfDay)
            .putInt("${p}windowStartMinute", config.windowStartMinute)
            .putInt("${p}windowEndMinute", config.windowEndMinute)
            .putInt("${p}dayOfWeek", config.dayOfWeek)
            .putStringSet(
                "${p}customDayMinutes",
                config.customDayMinutes.map { (day, minute) -> "$day:$minute" }.toSet()
            )
            .apply()
    }

    fun load(context: Context, key: String = DEFAULT_KEY): ScheduleConfig? {
        val p = prefix(key)
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val mode = prefs.getString("${p}mode", null) ?: return null
        val customDayMinutes = prefs.getStringSet("${p}customDayMinutes", emptySet())
            .orEmpty()
            .mapNotNull { entry ->
                val parts = entry.split(":")
                val day = parts.getOrNull(0)?.toIntOrNull()
                val minute = parts.getOrNull(1)?.toIntOrNull()
                if (day != null && minute != null) day to minute else null
            }
            .toMap()
        return ScheduleConfig(
            mode = ScheduleMode.valueOf(mode),
            fixedMinuteOfDay = prefs.getInt("${p}fixedMinuteOfDay", 9 * 60),
            windowStartMinute = prefs.getInt("${p}windowStartMinute", 9 * 60),
            windowEndMinute = prefs.getInt("${p}windowEndMinute", 21 * 60),
            dayOfWeek = prefs.getInt("${p}dayOfWeek", DayOfWeek.SUNDAY.value),
            customDayMinutes = customDayMinutes
        )
    }

    private fun prefix(key: String): String = if (key.isEmpty()) "" else "${key}_"
}
