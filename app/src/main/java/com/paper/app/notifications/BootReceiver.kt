package com.paper.app.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Alarms don't survive reboot; re-arm from the persisted schedule. */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            ReminderScheduler.scheduleAll(context)
        }
    }
}
