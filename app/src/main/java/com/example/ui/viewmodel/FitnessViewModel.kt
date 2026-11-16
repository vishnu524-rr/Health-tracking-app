package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.data.local.FitnessGoal
import com.example.data.local.PulseGoalDatabase
import com.example.data.model.GoalCategory
import com.example.data.model.GoalWithProgress
import com.example.data.model.LogWithGoalInfo
import com.example.data.model.WeeklySummary
import com.example.data.preferences.ReminderPreferencesRepository
import com.example.data.preferences.ReminderSettings
import com.example.data.repository.FitnessRepository
import com.example.notifications.DailyReminderScheduler
import com.example.notifications.NotificationHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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
    val snackbarMessage: String? = null
)

class FitnessViewModel(application: Application) : AndroidViewModel(application) {
    private val appContext = application.applicationContext
    private val database = PulseGoalDatabase.getInstance(appContext)
    private val fitnessRepository = FitnessRepository(
        goalDao = database.fitnessGoalDao(),
        logDao = database.fitnessDao()
    )
    private val reminderPrefsRepository = ReminderPreferencesRepository(appContext)

    private val selectedCategoryFlow = MutableStateFlow<GoalCategory?>(null)
    private val snackbarFlow = MutableStateFlow<String?>(null)

    val uiState: StateFlow<PulseGoalUiState> = combine(
        fitnessRepository.goalsWithProgressFlow,
        fitnessRepository.weeklySummaryFlow,
        fitnessRepository.recentActivityFlow,
        reminderPrefsRepository.settingsFlow,
        selectedCategoryFlow,
        snackbarFlow
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val goals = values[0] as List<GoalWithProgress>
        val weeklySummary = values[1] as WeeklySummary
        @Suppress("UNCHECKED_CAST")
        val recentActivity = values[2] as List<LogWithGoalInfo>
        val reminderSettings = values[3] as ReminderSettings
        val selectedCategory = values[4] as GoalCategory?
        val snackbarMessage = values[5] as String?

        val filtered = if (selectedCategory == null) {
            goals
        } else {
            goals.filter { it.category == selectedCategory }
        }

        PulseGoalUiState(
            goals = goals,
            filteredGoals = filtered,
            selectedCategory = selectedCategory,
            weeklySummary = weeklySummary,
            recentActivity = recentActivity,
            reminderSettings = reminderSettings,
            isLoading = false,
            snackbarMessage = snackbarMessage
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = PulseGoalUiState()
    )

    init {
        NotificationHelper.ensureNotificationChannel(appContext)
        viewModelScope.launch {
            val currentSettings = reminderPrefsRepository.getSettingsOnce()
            DailyReminderScheduler.scheduleNextReminder(appContext, currentSettings)
        }
    }

    fun selectCategory(category: GoalCategory?) {
        selectedCategoryFlow.value = category
    }

    fun clearSnackbar() {
        snackbarFlow.value = null
    }

    fun showSnackbar(message: String) {
        snackbarFlow.value = message
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
            fitnessRepository.createGoal(
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
        }
    }

    fun updateGoal(goal: FitnessGoal) {
        viewModelScope.launch {
            fitnessRepository.updateGoal(goal)
            snackbarFlow.value = "Updated \"${goal.title}\""
        }
    }

    fun deleteGoal(goalId: Long) {
        viewModelScope.launch {
            fitnessRepository.deleteGoal(goalId)
            snackbarFlow.value = "Goal deleted"
        }
    }

    fun logProgress(goalId: Long, amount: Int, note: String = "") {
        viewModelScope.launch {
            fitnessRepository.logProgress(goalId, amount, note)
        }
    }

    fun deleteLog(logId: Long) {
        viewModelScope.launch {
            fitnessRepository.deleteLog(logId)
            snackbarFlow.value = "Check-in removed"
        }
    }

    fun loadStarterGoals() {
        viewModelScope.launch {
            fitnessRepository.addStarterGoals()
            snackbarFlow.value = "Starter fitness goals added!"
        }
    }

    fun updateReminderEnabled(enabled: Boolean) {
        viewModelScope.launch {
            reminderPrefsRepository.setEnabled(enabled)
            val updated = reminderPrefsRepository.getSettingsOnce()
            DailyReminderScheduler.scheduleNextReminder(appContext, updated)
        }
    }

    fun updateReminderTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            reminderPrefsRepository.setTime(hour, minute)
            val updated = reminderPrefsRepository.getSettingsOnce()
            DailyReminderScheduler.scheduleNextReminder(appContext, updated)
            snackbarFlow.value = "Daily reminder updated"
        }
    }

    fun toggleReminderDay(isoDay: Int) {
        viewModelScope.launch {
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
            if (!NotificationHelper.hasNotificationPermission(appContext)) {
                snackbarFlow.value = appContext.getString(R.string.permission_required_for_test)
                return@launch
            }
            val settings = reminderPrefsRepository.getSettingsOnce()
            val title = appContext.getString(R.string.default_notification_title)
            val baseMsg = settings.customMessage.trim().ifBlank {
                appContext.getString(R.string.default_notification_body)
            }
            val body = if (settings.includeGoalSummary) {
                val (remaining, total) = fitnessRepository.getTodayPendingSummary()
                when {
                    total == 0 -> baseMsg
                    remaining == 0 -> "All $total goals completed today! Keep your momentum going."
                    else -> "$remaining of $total goals remaining today. $baseMsg"
                }
            } else {
                baseMsg
            }

            val posted = NotificationHelper.showReminderNotification(
                context = appContext,
                title = title,
                body = body,
                notificationId = NotificationHelper.TEST_NOTIFICATION_ID
            )
            snackbarFlow.value = if (posted) {
                appContext.getString(R.string.test_notification_sent)
            } else {
                appContext.getString(R.string.permission_required_for_test)
            }
        }
    }
}
