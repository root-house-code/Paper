package com.paper.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.core.content.getSystemService

class PaperApp : Application() {

    override fun onCreate() {
        super.onCreate()
        val channel = NotificationChannel(
            REMINDER_CHANNEL_ID,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Prompts you to write at your chosen schedule."
        }
        getSystemService<NotificationManager>()?.createNotificationChannel(channel)
    }

    companion object {
        const val REMINDER_CHANNEL_ID = "journal_reminders"
    }
}
