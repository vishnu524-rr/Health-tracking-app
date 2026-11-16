package com.example.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.preferences.ReminderPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_TIME_CHANGED &&
            action != Intent.ACTION_TIMEZONE_CHANGED
        ) {
            return
        }

        val pendingResult = goAsync()
        val appContext = context.applicationContext

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val settings = ReminderPreferencesRepository(appContext).getSettingsOnce()
                DailyReminderScheduler.scheduleNextReminder(appContext, settings)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
