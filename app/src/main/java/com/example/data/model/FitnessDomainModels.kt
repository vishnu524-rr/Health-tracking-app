package com.example.data.model

import com.example.data.remote.FirestoreFitnessGoal
import com.example.data.remote.FirestoreGoalLog
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class DayTotal(
    val dateKey: String,
    val shortDayLabel: String,
    val amount: Int,
    val target: Int
) {
    val progressFraction: Float
        get() = if (target <= 0) 0f else (amount.toFloat() / target.toFloat()).coerceIn(0f, 1f)
}

data class GoalWithProgress(
    val goal: FirestoreFitnessGoal,
    val category: GoalCategory,
    val todayLogged: Int,
    val currentStreakDays: Int,
    val bestStreakDays: Int,
    val weeklyTotal: Int,
    val last7Days: List<DayTotal>,
    val recentLogs: List<FirestoreGoalLog>
) {
    val progressFraction: Float
        get() = if (goal.targetValue <= 0L) 0f
        else (todayLogged.toFloat() / goal.targetValue.toFloat()).coerceIn(0f, 1f)

    val isCompletedToday: Boolean
        get() = goal.targetValue > 0L && todayLogged.toLong() >= goal.targetValue

    val remainingToday: Int
        get() = (goal.targetValue.toInt() - todayLogged).coerceAtLeast(0)
}

data class LogWithGoalInfo(
    val log: FirestoreGoalLog,
    val goalTitle: String,
    val goalUnit: String,
    val category: GoalCategory
)

data class DayCompletionRate(
    val dateKey: String,
    val shortDayLabel: String,
    val completedCount: Int,
    val totalGoals: Int
) {
    val fraction: Float
        get() = if (totalGoals <= 0) 0f else (completedCount.toFloat() / totalGoals.toFloat()).coerceIn(0f, 1f)
}

data class WeeklySummary(
    val completedTodayCount: Int,
    val totalActiveGoals: Int,
    val dailyOverallFraction: Float,
    val weeklyCompletionPercent: Int,
    val totalCheckInsThisWeek: Int,
    val bestActiveStreak: Int,
    val last7DayRates: List<DayCompletionRate>
)

object DateKeyUtils {
    private val keyFormatter = ThreadLocal.withInitial {
        SimpleDateFormat("yyyy-MM-dd", Locale.US)
    }
    private val dayLabelFormatter = ThreadLocal.withInitial {
        SimpleDateFormat("EEE", Locale.getDefault())
    }
    private val timeFormatter = ThreadLocal.withInitial {
        SimpleDateFormat("h:mm a", Locale.getDefault())
    }
    private val headerDateFormatter = ThreadLocal.withInitial {
        SimpleDateFormat("EEEE, MMM d", Locale.getDefault())
    }
    private val shortDateFormatter = ThreadLocal.withInitial {
        SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    }

    fun formatShortDate(millis: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = millis }
        return shortDateFormatter.get()!!.format(cal.time)
    }

    fun todayKey(): String = keyFormatter.get()!!.format(Calendar.getInstance().time)

    fun formatHeaderDate(): String = headerDateFormatter.get()!!.format(Calendar.getInstance().time)

    fun formatLogTime(millis: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = millis }
        return timeFormatter.get()!!.format(cal.time)
    }

    fun dateKeyDaysAgo(daysAgo: Int): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -daysAgo)
        return keyFormatter.get()!!.format(cal.time)
    }

    fun shortLabelDaysAgo(daysAgo: Int): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -daysAgo)
        return dayLabelFormatter.get()!!.format(cal.time).take(3)
    }

    fun lastNDaysKeysWithLabels(days: Int = 7): List<Pair<String, String>> {
        return ((days - 1) downTo 0).map { offset ->
            dateKeyDaysAgo(offset) to shortLabelDaysAgo(offset)
        }
    }

    fun calendarDayToIso(calendarDayOfWeek: Int): Int {
        return when (calendarDayOfWeek) {
            Calendar.MONDAY -> 1
            Calendar.TUESDAY -> 2
            Calendar.WEDNESDAY -> 3
            Calendar.THURSDAY -> 4
            Calendar.FRIDAY -> 5
            Calendar.SATURDAY -> 6
            Calendar.SUNDAY -> 7
            else -> 1
        }
    }

    fun currentIsoDayOfWeek(): Int {
        return calendarDayToIso(Calendar.getInstance().get(Calendar.DAY_OF_WEEK))
    }
}
