package com.example.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.data.model.DateKeyUtils
import com.example.data.preferences.ReminderSettings
import java.util.Calendar

object DailyReminderScheduler {
    private const val REQUEST_CODE_DAILY = 4201

    fun scheduleNextReminder(context: Context, settings: ReminderSettings) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = buildPendingIntent(context)

        if (!settings.isEnabled || settings.activeDays.isEmpty()) {
            alarmManager.cancel(pendingIntent)
            return
        }

        val triggerAtMillis = computeNextTriggerMillis(
            hour = settings.hour,
            minute = settings.minute,
            activeDays = settings.activeDays
        ) ?: return

        alarmManager.cancel(pendingIntent)
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis,
            pendingIntent
        )
    }

    fun cancelReminders(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(buildPendingIntent(context))
    }

    private fun buildPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, DailyReminderReceiver::class.java)
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_DAILY,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    internal fun computeNextTriggerMillis(
        hour: Int,
        minute: Int,
        activeDays: Set<Int>,
        nowMillis: Long = System.currentTimeMillis()
    ): Long? {
        if (activeDays.isEmpty()) return null
        val now = Calendar.getInstance().apply { timeInMillis = nowMillis }

        for (dayOffset in 0..7) {
            val candidate = Calendar.getInstance().apply {
                timeInMillis = nowMillis
                add(Calendar.DAY_OF_YEAR, dayOffset)
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val isoDay = DateKeyUtils.calendarDayToIso(candidate.get(Calendar.DAY_OF_WEEK))
            if (isoDay in activeDays && candidate.timeInMillis > now.timeInMillis + 1_000L) {
                return candidate.timeInMillis
            }
        }
        return null
    }
}
