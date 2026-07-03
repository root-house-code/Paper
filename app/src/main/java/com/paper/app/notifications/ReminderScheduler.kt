package com.paper.app.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.getSystemService
import com.paper.app.data.ScheduleStore

object ReminderScheduler {

    private const val REQUEST_CODE = 1001

    /** Arms the alarm for the next occurrence of the saved schedule. */
    fun scheduleNext(context: Context) {
        val config = ScheduleStore.load(context) ?: return
        scheduleAt(context, config.nextTriggerMillis())
    }

    /** Arms a one-off alarm, e.g. for a snoozed notification. */
    fun scheduleAt(context: Context, triggerAtMillis: Long) {
        val alarmManager = context.getSystemService<AlarmManager>() ?: return
        val pending = reminderPendingIntent(context)

        val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            alarmManager.canScheduleExactAlarms()
        if (canExact) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP, triggerAtMillis, pending
            )
        } else {
            // Exact-alarm permission revoked: fall back to an inexact alarm rather
            // than silently dropping the reminder.
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP, triggerAtMillis, pending
            )
        }
    }

    fun cancel(context: Context) {
        context.getSystemService<AlarmManager>()?.cancel(reminderPendingIntent(context))
    }

    private fun reminderPendingIntent(context: Context): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            Intent(context, ReminderReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
}
