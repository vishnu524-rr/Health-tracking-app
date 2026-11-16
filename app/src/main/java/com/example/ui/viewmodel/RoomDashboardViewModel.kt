package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.FitnessGoal
import com.example.data.model.GoalCategory
import com.example.data.repository.FitnessRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class RoomGoalDashboardItem(
    val goal: FitnessGoal,
    val category: GoalCategory
) {
    val progressFraction: Float
        get() = if (goal.targetValue <= 0) 0f
        else (goal.currentProgress.toFloat() / goal.targetValue.toFloat()).coerceIn(0f, 1f)

    val percentComplete: Int
        get() = (progressFraction * 100f).toInt().coerceIn(0, 100)

    val isCompleted: Boolean
        get() = goal.targetValue > 0 && goal.currentProgress >= goal.targetValue

    val remaining: Int
        get() = (goal.targetValue - goal.currentProgress).coerceAtLeast(0)
}

data class RoomDashboardUiState(
    val goals: List<RoomGoalDashboardItem> = emptyList(),
    val filteredGoals: List<RoomGoalDashboardItem> = emptyList(),
    val selectedCategory: GoalCategory? = null,
    val completedCount: Int = 0,
    val totalCount: Int = 0,
    val overallProgressFraction: Float = 0f,
    val isLoading: Boolean = true,
    val snackbarMessage: String? = null
)

class RoomDashboardViewModel(
    private val repository: FitnessRepository
) : ViewModel() {

    private val selectedCategoryFlow = MutableStateFlow<GoalCategory?>(null)
    private val snackbarFlow = MutableStateFlow<String?>(null)

    val uiState: StateFlow<RoomDashboardUiState> = combine(
        repository.fitnessGoalsFlow,
        selectedCategoryFlow,
        snackbarFlow
    ) { goals, selectedCategory, snackbarMessage ->
        val items = goals.map { goal ->
            RoomGoalDashboardItem(
                goal = goal,
                category = GoalCategory.fromString(goal.category)
            )
        }
        val totalCount = items.size
        val completedCount = items.count { it.isCompleted }
        val overallProgressFraction = if (totalCount == 0) {
            0f
        } else {
            (items.sumOf { it.progressFraction.toDouble() } / totalCount.toDouble()).toFloat()
        }
        val filtered = if (selectedCategory == null) {
            items
        } else {
            items.filter { it.category == selectedCategory }
        }

        RoomDashboardUiState(
            goals = items,
            filteredGoals = filtered,
            selectedCategory = selectedCategory,
            completedCount = completedCount,
            totalCount = totalCount,
            overallProgressFraction = overallProgressFraction,
            isLoading = false,
            snackbarMessage = snackbarMessage
        )
    }
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = RoomDashboardUiState()
        )

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
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.createGoal(
                    title = title,
                    description = description,
                    category = category,
                    targetValue = targetValue,
                    unit = unit,
                    quickIncrement = quickIncrement,
                    startDate = startDate,
                    endDate = endDate
                )
                snackbarFlow.value = "Saved \"$title\" to Room database"
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                snackbarFlow.value = e.localizedMessage ?: "Failed to create goal"
            }
        }
    }

    fun updateGoalDetails(
        goalId: Long,
        title: String,
        description: String,
        category: GoalCategory,
        targetValue: Int,
        unit: String,
        quickIncrement: Int,
        startDate: Long,
        endDate: Long?
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.updateGoalDetails(
                    goalId = goalId,
                    title = title,
                    description = description,
                    category = category,
                    targetValue = targetValue,
                    unit = unit,
                    quickIncrement = quickIncrement,
                    startDate = startDate,
                    endDate = endDate
                )
                snackbarFlow.value = "Updated \"$title\""
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                snackbarFlow.value = e.localizedMessage ?: "Failed to update goal"
            }
        }
    }

    fun incrementProgress(goalId: Long, amount: Int, note: String = "") {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.incrementGoalProgress(goalId, amount, note)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                snackbarFlow.value = e.localizedMessage ?: "Failed to update progress"
            }
        }
    }

    fun deleteGoal(goalId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.deleteGoal(goalId)
                snackbarFlow.value = "Goal deleted from Room"
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                snackbarFlow.value = e.localizedMessage ?: "Failed to delete goal"
            }
        }
    }

    fun loadStarterGoals() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.addStarterGoals()
                snackbarFlow.value = "Starter goals saved to Room!"
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                snackbarFlow.value = e.localizedMessage ?: "Failed to load starter goals"
            }
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
