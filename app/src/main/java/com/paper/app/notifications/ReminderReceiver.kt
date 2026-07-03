package com.paper.app.notifications

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.getSystemService
import com.paper.app.MainActivity
import com.paper.app.PaperApp
import com.paper.app.R

/** Fires at the scheduled moment: shows the prompt and arms the next occurrence. */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        showNotification(context)
        ReminderScheduler.scheduleNext(context)
    }

    private fun showNotification(context: Context) {
        val writeIntent = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).apply {
                putExtra(MainActivity.EXTRA_OPEN_EDITOR, true)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val laterIntent = PendingIntent.getBroadcast(
            context, 1,
            Intent(context, NotificationActionReceiver::class.java)
                .setAction(NotificationActionReceiver.ACTION_SNOOZE),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = Notification.Builder(context, PaperApp.REMINDER_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_paper)
            .setContentTitle(context.getString(R.string.notification_title))
            .setContentText(context.getString(R.string.notification_body))
            .setContentIntent(writeIntent)
            .setAutoCancel(true)
            .addAction(
                Notification.Action.Builder(
                    null, context.getString(R.string.notification_action_write), writeIntent
                ).build()
            )
            .addAction(
                Notification.Action.Builder(
                    null, context.getString(R.string.notification_action_later), laterIntent
                ).build()
            )
            .build()

        context.getSystemService<NotificationManager>()
            ?.notify(NOTIFICATION_ID, notification)
    }

    companion object {
        const val NOTIFICATION_ID = 2001
    }
}
