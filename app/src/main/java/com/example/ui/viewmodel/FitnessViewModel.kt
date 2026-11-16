package com.example.ui.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.data.model.DateKeyUtils
import com.example.data.model.DayCompletionRate
import com.example.data.model.DayTotal
import com.example.data.model.GoalCategory
import com.example.data.model.GoalWithProgress
import com.example.data.model.LogWithGoalInfo
import com.example.data.model.WeeklySummary
import com.example.data.preferences.ReminderPreferencesRepository
import com.example.data.preferences.ReminderSettings
import com.example.data.remote.FirestoreFitnessGoal
import com.example.data.remote.FirestoreFitnessRepository
import com.example.data.remote.FirestoreGoalLog
import com.example.notifications.DailyReminderScheduler
import com.example.notifications.NotificationHelper
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class PulseGoalUiState(
    val goals: List<GoalWithProgress> = emptyList(),
    val filteredGoals: List<GoalWithProgress> = emptyList(),
    val selectedCategory: GoalCategory? = null,
    val weeklySummary: WeeklySummary = WeeklySummary(
        completedTodayCount = 0,
        totalActiveGoals = 0,
        dailyOverallFraction = 0f,
        weeklyCompletionPercent = 0,
        totalCheckInsThisWeek = 0,
        bestActiveStreak = 0,
        last7DayRates = emptyList()
    ),
    val recentActivity: List<LogWithGoalInfo> = emptyList(),
    val reminderSettings: ReminderSettings = ReminderSettings(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val snackbarMessage: String? = null
)

class FitnessViewModel(
    private val currentUserId: String,
    private val firestoreRepository: FirestoreFitnessRepository,
    private val reminderPrefsRepository: ReminderPreferencesRepository,
    appContext: Context
) : ViewModel() {
    private val appContext = appContext.applicationContext
    private val selectedCategoryFlow = MutableStateFlow<GoalCategory?>(null)
    private val snackbarFlow = MutableStateFlow<String?>(null)

    val uiState: StateFlow<PulseGoalUiState> = combine(
        firestoreRepository.observeGoals(currentUserId),
        firestoreRepository.observeLogs(currentUserId),
        reminderPrefsRepository.settingsFlow,
        selectedCategoryFlow,
        snackbarFlow
    ) { goals, logs, reminderSettings, selectedCategory, snackbarMessage ->
        buildUiState(
            goals = goals,
            logs = logs,
            reminderSettings = reminderSettings,
            selectedCategory = selectedCategory,
            snackbarMessage = snackbarMessage
        )
    }.catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val rootError = firestoreError ?: error
        Log.w(TAG, "Error observing fitness goals", rootError)
        emit(
            PulseGoalUiState(
                isLoading = false,
                errorMessage = rootError.message ?: "Failed to sync fitness goals"
            )
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = PulseGoalUiState()
    )

    init {
        viewModelScope.launch(Dispatchers.IO) {
            NotificationHelper.ensureNotificationChannel(this@FitnessViewModel.appContext)
            val currentSettings = reminderPrefsRepository.getSettingsOnce()
            DailyReminderScheduler.scheduleNextReminder(this@FitnessViewModel.appContext, currentSettings)
        }
    }

    fun selectCategory(category: GoalCategory?) {
        selectedCategoryFlow.value = category
    }

    fun clearSnackbar() {
        snackbarFlow.value = null
    }

    fun createGoal(
        title: String,
        description: String = "",
        category: GoalCategory,
        targetValue: Int,
        unit: String,
        quickIncrement: Int,
        startDate: Long = System.currentTimeMillis(),
        endDate: Long? = null
    ) {
        viewModelScope.launch {
            try {
                firestoreRepository.createGoal(
                    title = title,
                    description = description,
                    category = category,
                    targetValue = targetValue,
                    unit = unit,
                    quickIncrement = quickIncrement,
                    startDate = startDate,
                    endDate = endDate
                )
                snackbarFlow.value = "Added goal \"$title\""
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                snackbarFlow.value = e.localizedMessage ?: "Failed to create goal"
            }
        }
    }

    fun updateGoal(goal: FirestoreFitnessGoal) {
        viewModelScope.launch {
            try {
                firestoreRepository.updateGoal(goal)
                snackbarFlow.value = "Updated \"${goal.title}\""
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                snackbarFlow.value = e.localizedMessage ?: "Failed to update goal"
            }
        }
    }

    fun deleteGoal(goalId: String) {
        viewModelScope.launch {
            try {
                firestoreRepository.deleteGoal(goalId)
                snackbarFlow.value = "Goal deleted"
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                snackbarFlow.value = e.localizedMessage ?: "Failed to delete goal"
            }
        }
    }

    fun logProgress(goalId: String, amount: Int, note: String = "") {
        viewModelScope.launch {
            try {
                firestoreRepository.logProgress(goalId, amount, note)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                snackbarFlow.value = e.localizedMessage ?: "Failed to log progress"
            }
        }
    }

    fun deleteLog(log: FirestoreGoalLog) {
        viewModelScope.launch {
            try {
                firestoreRepository.deleteLog(log)
                snackbarFlow.value = "Check-in removed"
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                snackbarFlow.value = e.localizedMessage ?: "Failed to delete check-in"
            }
        }
    }

    fun loadStarterGoals() {
        viewModelScope.launch {
            try {
                firestoreRepository.addStarterGoals()
                snackbarFlow.value = "Starter fitness goals added!"
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                snackbarFlow.value = e.localizedMessage ?: "Failed to load starter goals"
            }
        }
    }

    fun updateReminderEnabled(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            reminderPrefsRepository.setEnabled(enabled)
            val updated = reminderPrefsRepository.getSettingsOnce()
            DailyReminderScheduler.scheduleNextReminder(appContext, updated)
        }
    }

    fun updateReminderTime(hour: Int, minute: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            reminderPrefsRepository.setTime(hour, minute)
            val updated = reminderPrefsRepository.getSettingsOnce()
            DailyReminderScheduler.scheduleNextReminder(appContext, updated)
            snackbarFlow.value = "Daily reminder updated"
        }
    }

    fun toggleReminderDay(isoDay: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            reminderPrefsRepository.toggleActiveDay(isoDay)
            val updated = reminderPrefsRepository.getSettingsOnce()
            DailyReminderScheduler.scheduleNextReminder(appContext, updated)
        }
    }

    fun updateCustomReminderMessage(message: String) {
        viewModelScope.launch {
            reminderPrefsRepository.setCustomMessage(message)
        }
    }

    fun updateIncludeGoalSummary(include: Boolean) {
        viewModelScope.launch {
            reminderPrefsRepository.setIncludeGoalSummary(include)
        }
    }

    fun sendTestNotification() {
        viewModelScope.launch {
            val hasPerm = withContext(Dispatchers.IO) {
                NotificationHelper.hasNotificationPermission(appContext)
            }
            if (!hasPerm) {
                snackbarFlow.value = appContext.getString(R.string.permission_required_for_test)
                return@launch
            }
            val settings = withContext(Dispatchers.IO) {
                reminderPrefsRepository.getSettingsOnce()
            }
            val title = appContext.getString(R.string.default_notification_title)
            val baseMsg = settings.customMessage.trim().ifBlank {
                appContext.getString(R.string.default_notification_body)
            }
            val body = if (settings.includeGoalSummary) {
                val (remaining, total) = firestoreRepository.getTodayPendingSummary()
                when {
                    total == 0 -> baseMsg
                    remaining == 0 -> "All $total goals completed today! Keep your momentum going."
                    else -> "$remaining of $total goals remaining today. $baseMsg"
                }
            } else {
                baseMsg
            }

            val posted = withContext(Dispatchers.IO) {
                NotificationHelper.showReminderNotification(
                    context = appContext,
                    title = title,
                    body = body,
                    notificationId = NotificationHelper.TEST_NOTIFICATION_ID
                )
            }
            snackbarFlow.value = if (posted) {
                appContext.getString(R.string.test_notification_sent)
            } else {
                appContext.getString(R.string.permission_required_for_test)
            }
        }
    }

    private fun buildUiState(
        goals: List<FirestoreFitnessGoal>,
        logs: List<FirestoreGoalLog>,
        reminderSettings: ReminderSettings,
        selectedCategory: GoalCategory?,
        snackbarMessage: String?
    ): PulseGoalUiState {
        val todayKey = DateKeyUtils.todayKey()
        val last7Days = DateKeyUtils.lastNDaysKeysWithLabels(7)
        val last7KeysSet = last7Days.map { it.first }.toSet()
        val logsByGoal = logs.groupBy { it.goalId }

        val goalsWithProgress = goals.map { goal ->
            val goalLogs = logsByGoal[goal.id].orEmpty()
                .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            val dailyTotalsMap = goalLogs
                .groupBy { it.dateKey }
                .mapValues { (_, dayLogs) -> dayLogs.sumOf { it.amount.toInt() } }

            val todayLogged = dailyTotalsMap[todayKey] ?: 0
            val last7DayTotals = last7Days.map { (dateKey, shortLabel) ->
                DayTotal(
                    dateKey = dateKey,
                    shortDayLabel = shortLabel,
                    amount = dailyTotalsMap[dateKey] ?: 0,
                    target = goal.targetValue.toInt()
                )
            }

            val (currentStreak, bestStreak) = calculateStreaks(
                dailyTotalsMap = dailyTotalsMap,
                targetValue = goal.targetValue.toInt()
            )

            GoalWithProgress(
                goal = goal,
                category = GoalCategory.fromString(goal.category),
                todayLogged = todayLogged,
                currentStreakDays = currentStreak,
                bestStreakDays = bestStreak,
                weeklyTotal = last7DayTotals.sumOf { it.amount },
                last7Days = last7DayTotals,
                recentLogs = goalLogs.take(30)
            )
        }

        val totalActiveGoals = goalsWithProgress.size
        val completedTodayCount = goalsWithProgress.count { it.isCompletedToday }
        val dailyOverallFraction = if (totalActiveGoals == 0) {
            0f
        } else {
            (goalsWithProgress.sumOf { it.progressFraction.toDouble() } / totalActiveGoals.toDouble()).toFloat()
        }

        val logsByGoalAndDate = logs
            .groupBy { it.goalId }
            .mapValues { (_, goalLogs) ->
                goalLogs.groupBy { it.dateKey }.mapValues { (_, dayLogs) -> dayLogs.sumOf { it.amount.toInt() } }
            }

        val last7DayRates = last7Days.map { (dateKey, label) ->
            val completedOnDay = goals.count { goal ->
                val logged = logsByGoalAndDate[goal.id]?.get(dateKey) ?: 0
                goal.targetValue > 0L && logged.toLong() >= goal.targetValue
            }
            DayCompletionRate(
                dateKey = dateKey,
                shortDayLabel = label,
                completedCount = completedOnDay,
                totalGoals = totalActiveGoals
            )
        }

        val weeklyPossibleSlots = totalActiveGoals * 7
        val weeklyCompletedSlots = last7DayRates.sumOf { it.completedCount }
        val weeklyCompletionPercent = if (weeklyPossibleSlots == 0) 0
        else ((weeklyCompletedSlots.toFloat() / weeklyPossibleSlots.toFloat()) * 100f).toInt().coerceIn(0, 100)

        val totalCheckInsThisWeek = logs.count { it.dateKey in last7KeysSet }
        val bestActiveStreak = goalsWithProgress.maxOfOrNull { it.bestStreakDays } ?: 0

        val weeklySummary = WeeklySummary(
            completedTodayCount = completedTodayCount,
            totalActiveGoals = totalActiveGoals,
            dailyOverallFraction = dailyOverallFraction,
            weeklyCompletionPercent = weeklyCompletionPercent,
            totalCheckInsThisWeek = totalCheckInsThisWeek,
            bestActiveStreak = bestActiveStreak,
            last7DayRates = last7DayRates
        )

        val goalsById = goals.associateBy { it.id }
        val recentActivity = logs
            .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            .mapNotNull { log ->
                val goal = goalsById[log.goalId] ?: return@mapNotNull null
                LogWithGoalInfo(
                    log = log,
                    goalTitle = goal.title,
                    goalUnit = goal.unit,
                    category = GoalCategory.fromString(goal.category)
                )
            }
            .take(40)

        val filtered = if (selectedCategory == null) {
            goalsWithProgress
        } else {
            goalsWithProgress.filter { it.category == selectedCategory }
        }

        return PulseGoalUiState(
            goals = goalsWithProgress,
            filteredGoals = filtered,
            selectedCategory = selectedCategory,
            weeklySummary = weeklySummary,
            recentActivity = recentActivity,
            reminderSettings = reminderSettings,
            isLoading = false,
            errorMessage = null,
            snackbarMessage = snackbarMessage
        )
    }

    private fun calculateStreaks(
        dailyTotalsMap: Map<String, Int>,
        targetValue: Int
    ): Pair<Int, Int> {
        if (targetValue <= 0 || dailyTotalsMap.isEmpty()) return 0 to 0

        var currentStreak = 0
        val todayCompleted = (dailyTotalsMap[DateKeyUtils.dateKeyDaysAgo(0)] ?: 0) >= targetValue
        var offset = if (todayCompleted) 0 else 1
        while (offset <= 365) {
            val key = DateKeyUtils.dateKeyDaysAgo(offset)
            if ((dailyTotalsMap[key] ?: 0) >= targetValue) {
                currentStreak++
                offset++
            } else {
                break
            }
        }

        var bestStreak = currentStreak
        var running = 0
        for (dayOffset in 0..365) {
            val key = DateKeyUtils.dateKeyDaysAgo(dayOffset)
            if ((dailyTotalsMap[key] ?: 0) >= targetValue) {
                running++
                if (running > bestStreak) bestStreak = running
            } else {
                running = 0
            }
        }

        return currentStreak to bestStreak
    }

    private companion object {
        const val TAG = "FitnessVM"
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
