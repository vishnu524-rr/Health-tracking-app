package com.example.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.R
import com.example.data.model.DateKeyUtils
import com.example.data.preferences.ReminderPreferencesRepository
import com.example.data.remote.FirestoreFitnessRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DailyReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val pendingResult = goAsync()
        val appContext = context.applicationContext

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val prefsRepo = ReminderPreferencesRepository(appContext)
                val settings = prefsRepo.getSettingsOnce()
                val todayIso = DateKeyUtils.currentIsoDayOfWeek()

                if (settings.isEnabled && todayIso in settings.activeDays) {
                    val title = appContext.getString(R.string.default_notification_title)
                    val baseMessage = settings.customMessage.trim().ifBlank {
                        appContext.getString(R.string.default_notification_body)
                    }

                    val body = if (settings.includeGoalSummary && Firebase.auth.currentUser != null) {
                        val repo = FirestoreFitnessRepository(appContext)
                        val (remaining, total) = repo.getTodayPendingSummary()
                        when {
                            total == 0 -> baseMessage
                            remaining == 0 -> "All $total goals completed today! Keep your momentum going."
                            else -> "$remaining of $total goals remaining today. $baseMessage"
                        }
                    } else {
                        baseMessage
                    }

                    NotificationHelper.showReminderNotification(
                        context = appContext,
                        title = title,
                        body = body,
                        notificationId = NotificationHelper.DAILY_NOTIFICATION_ID
                    )
                }

                DailyReminderScheduler.scheduleNextReminder(appContext, settings)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
