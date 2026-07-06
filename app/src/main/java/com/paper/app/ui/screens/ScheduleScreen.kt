package com.paper.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paper.app.data.ScheduleConfig
import com.paper.app.data.ScheduleMode
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

private fun formatMinute(minuteOfDay: Int): String {
    val h = minuteOfDay / 60
    val m = minuteOfDay % 60
    val amPm = if (h < 12) "AM" else "PM"
    val h12 = when (h % 12) { 0 -> 12; else -> h % 12 }
    return "%d:%02d %s".format(h12, m, amPm)
}

private val modeLabels = mapOf(
    ScheduleMode.DAILY_FIXED to "Daily, at a set time",
    ScheduleMode.DAILY_RANDOM to "Daily, at a random time",
    ScheduleMode.WEEKLY_FIXED to "Weekly, on a set day and time",
    ScheduleMode.WEEKLY_FIXED_DAY_RANDOM_TIME to "Weekly, on a set day at a random time",
    ScheduleMode.WEEKLY_RANDOM to "Weekly, on a random day and time",
    ScheduleMode.CUSTOM to "Custom, a time for each day"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    initial: ScheduleConfig?,
    categoryLabel: String? = null,
    onSaved: (ScheduleConfig) -> Unit
) {
    var mode by remember { mutableStateOf(initial?.mode ?: ScheduleMode.DAILY_FIXED) }
    var fixedMinute by remember { mutableIntStateOf(initial?.fixedMinuteOfDay ?: 9 * 60) }
    var windowStart by remember { mutableIntStateOf(initial?.windowStartMinute ?: 9 * 60) }
    var windowEnd by remember { mutableIntStateOf(initial?.windowEndMinute ?: 21 * 60) }
    var dayOfWeek by remember { mutableIntStateOf(initial?.dayOfWeek ?: DayOfWeek.SUNDAY.value) }
    var enabledDays by remember {
        mutableStateOf(initial?.customDayMinutes?.keys ?: emptySet())
    }
    // Remembers each day's time even while disabled, so toggling a day off and
    // back on doesn't lose the time it was set to.
    var dayTimes by remember {
        mutableStateOf(
            DayOfWeek.entries.associate { d ->
                d.value to (initial?.customDayMinutes?.get(d.value) ?: (9 * 60))
            }
        )
    }
    var customError by remember { mutableStateOf<String?>(null) }

    val needsCustomDays = mode == ScheduleMode.CUSTOM
    val needsFixedTime = !needsCustomDays &&
        (mode == ScheduleMode.DAILY_FIXED || mode == ScheduleMode.WEEKLY_FIXED)
    val needsWindow = !needsCustomDays && !needsFixedTime
    val needsDay = mode == ScheduleMode.WEEKLY_FIXED ||
        mode == ScheduleMode.WEEKLY_FIXED_DAY_RANDOM_TIME

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 32.dp, vertical = 48.dp)
    ) {
        categoryLabel?.let {
            Text(
                it.uppercase(Locale.getDefault()),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
        }
        Text(
            "When would you like to write?",
            style = MaterialTheme.typography.headlineMedium.copy(fontSize = 22.sp),
            maxLines = 1
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "You'll get one quiet nudge. Write, or push it back an hour.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(32.dp))

        modeLabels.forEach { (m, label) ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(selected = mode == m, onClick = { mode = m })
                    .padding(vertical = 4.dp)
            ) {
                RadioButton(selected = mode == m, onClick = { mode = m })
                Text(label, style = MaterialTheme.typography.bodyLarge)
            }
        }

        Spacer(Modifier.height(24.dp))

        if (needsCustomDays) {
            Text("Days", style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(8.dp))
            DayOfWeek.entries.forEach { day ->
                val enabled = day.value in enabledDays
                val minute = dayTimes[day.value] ?: (9 * 60)

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = enabled,
                            onClick = {
                                enabledDays =
                                    if (enabled) enabledDays - day.value
                                    else enabledDays + day.value
                                customError = null
                            }
                        )
                        .padding(vertical = 4.dp)
                ) {
                    Checkbox(
                        checked = enabled,
                        onCheckedChange = { checked ->
                            enabledDays =
                                if (checked) enabledDays + day.value
                                else enabledDays - day.value
                            customError = null
                        }
                    )
                    Text(
                        day.getDisplayName(TextStyle.FULL, Locale.getDefault()),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f)
                    )
                    if (enabled) {
                        Text(
                            formatMinute(minute),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (enabled) {
                    Slider(
                        value = minute.toFloat(),
                        onValueChange = {
                            dayTimes = dayTimes + (day.value to (it.toInt() / 15) * 15)
                        },
                        valueRange = 0f..(24f * 60 - 15),
                        steps = (24 * 4) - 2,
                        modifier = Modifier.padding(start = 40.dp)
                    )
                }
            }
            customError?.let {
                Spacer(Modifier.height(4.dp))
                Text(
                    it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Spacer(Modifier.height(24.dp))
        }

        if (needsDay) {
            Text("Day", style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                DayOfWeek.entries.forEach { day ->
                    FilterChip(
                        modifier = Modifier.weight(1f),
                        selected = dayOfWeek == day.value,
                        onClick = { dayOfWeek = day.value },
                        label = {
                            Text(
                                day.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
        }

        if (needsFixedTime) {
            Text(
                "Time — ${formatMinute(fixedMinute)}",
                style = MaterialTheme.typography.labelMedium
            )
            Slider(
                value = fixedMinute.toFloat(),
                onValueChange = { fixedMinute = (it.toInt() / 15) * 15 },
                valueRange = 0f..(24f * 60 - 15),
                steps = (24 * 4) - 2
            )
        }

        if (needsWindow) {
            Text(
                "Window — ${formatMinute(windowStart)} – ${formatMinute(windowEnd)}",
                style = MaterialTheme.typography.labelMedium
            )
            RangeSlider(
                value = windowStart.toFloat()..windowEnd.toFloat(),
                onValueChange = { range ->
                    windowStart = (range.start.toInt() / 15) * 15
                    windowEnd = (range.endInclusive.toInt() / 15) * 15
                },
                valueRange = 0f..(24f * 60 - 15),
                steps = (24 * 4) - 2
            )
        }

        Spacer(Modifier.height(32.dp))
        Button(
            onClick = {
                if (needsCustomDays && enabledDays.isEmpty()) {
                    customError = "Choose at least one day."
                } else {
                    onSaved(
                        ScheduleConfig(
                            mode = mode,
                            fixedMinuteOfDay = fixedMinute,
                            windowStartMinute = windowStart,
                            windowEndMinute = windowEnd,
                            dayOfWeek = dayOfWeek,
                            customDayMinutes = enabledDays.associateWith {
                                dayTimes[it] ?: (9 * 60)
                            }
                        )
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Set schedule")
        }
    }
}
