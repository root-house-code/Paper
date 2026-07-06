package com.paper.app.notifications

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.getSystemService

/**
 * "Later" reschedules this occurrence one hour out. The regular cadence
 * (already armed by ReminderReceiver) is unaffected.
 */
class NotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_SNOOZE) {
            val categoryId = intent.getStringExtra(ReminderScheduler.EXTRA_CATEGORY_ID)
            context.getSystemService<NotificationManager>()
                ?.cancel(ReminderScheduler.notificationIdFor(categoryId))
            ReminderScheduler.scheduleAt(
                context,
                System.currentTimeMillis() + SNOOZE_MILLIS,
                categoryId
            )
        }
    }

    companion object {
        const val ACTION_SNOOZE = "com.paper.app.action.SNOOZE"
        private const val SNOOZE_MILLIS = 60 * 60 * 1000L
    }
}
