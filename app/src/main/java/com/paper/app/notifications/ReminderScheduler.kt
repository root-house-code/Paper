package com.paper.app.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.getSystemService
import com.paper.app.data.PromptCategory
import com.paper.app.data.PromptCategoryStore
import com.paper.app.data.ScheduleConfig
import com.paper.app.data.ScheduleMode
import com.paper.app.data.ScheduleStore

/**
 * Schedules reminders for the default prompt and for every opted-in prompt category.
 * Each gets its own alarm, notification, and snooze action (distinct request codes/IDs
 * per category) so several armed at once can't collide or clobber one another.
 */
object ReminderScheduler {

    const val EXTRA_CATEGORY_ID = "category_id"

    private const val ALARM_REQUEST_CODE_BASE = 1001
    private const val NOTIFICATION_ID_BASE = 2001
    private const val SNOOZE_REQUEST_CODE_BASE = 3001

    /** Arms the alarm for the next occurrence of one schedule. null = the default prompt. */
    fun scheduleNext(context: Context, categoryId: String? = null) {
        val config = ScheduleStore.load(context, categoryId ?: ScheduleStore.DEFAULT_KEY) ?: return
        scheduleAt(context, config.nextTriggerMillis(), categoryId)
    }

    /** Re-arms the default schedule plus every currently opted-in category. Alarms don't
     *  survive reboot, so this is what BootReceiver calls. */
    fun scheduleAll(context: Context) {
        scheduleNext(context)
        PromptCategoryStore.loadEnabled(context).forEach { categoryId -> scheduleNext(context, categoryId) }
    }

    /** Arms a one-off alarm, e.g. for a snoozed notification. */
    fun scheduleAt(context: Context, triggerAtMillis: Long, categoryId: String? = null) {
        val alarmManager = context.getSystemService<AlarmManager>() ?: return
        val pending = reminderPendingIntent(context, categoryId)

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

    fun cancel(context: Context, categoryId: String? = null) {
        context.getSystemService<AlarmManager>()?.cancel(reminderPendingIntent(context, categoryId))
    }

    /** Persists opt-in, seeds a default schedule the first time a category is turned on,
     *  then arms or cancels its alarm accordingly. */
    fun setCategoryEnabled(context: Context, categoryId: String, enabled: Boolean) {
        PromptCategoryStore.setEnabled(context, categoryId, enabled)
        if (enabled) {
            if (ScheduleStore.load(context, categoryId) == null) {
                ScheduleStore.save(context, ScheduleConfig(mode = ScheduleMode.DAILY_FIXED), categoryId)
            }
            scheduleNext(context, categoryId)
        } else {
            cancel(context, categoryId)
        }
    }

    fun notificationIdFor(categoryId: String?): Int = NOTIFICATION_ID_BASE + slotFor(categoryId)

    fun snoozeRequestCodeFor(categoryId: String?): Int = SNOOZE_REQUEST_CODE_BASE + slotFor(categoryId)

    private fun slotFor(categoryId: String?): Int = PromptCategory.byId(categoryId)?.slot ?: 0

    private fun reminderPendingIntent(context: Context, categoryId: String?): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            if (categoryId != null) putExtra(EXTRA_CATEGORY_ID, categoryId)
        }
        return PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE_BASE + slotFor(categoryId),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
