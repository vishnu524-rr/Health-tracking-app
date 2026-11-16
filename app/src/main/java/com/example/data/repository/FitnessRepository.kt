package com.example.data.repository

import com.example.data.local.FitnessDao
import com.example.data.local.FitnessGoal
import com.example.data.local.FitnessGoalDao
import com.example.data.local.GoalLogEntity
import com.example.data.model.DateKeyUtils
import com.example.data.model.DayCompletionRate
import com.example.data.model.DayTotal
import com.example.data.model.GoalCategory
import com.example.data.model.GoalWithProgress
import com.example.data.model.LogWithGoalInfo
import com.example.data.model.WeeklySummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.util.concurrent.TimeUnit

class FitnessRepository(
    private val goalDao: FitnessGoalDao,
    private val logDao: FitnessDao
) {

    val goalsWithProgressFlow: Flow<List<GoalWithProgress>> = combine(
        goalDao.observeAllGoals(),
        logDao.observeAllLogs()
    ) { goals, logs ->
        val todayKey = DateKeyUtils.todayKey()
        val last7Days = DateKeyUtils.lastNDaysKeysWithLabels(7)
        val logsByGoal = logs.groupBy { it.goalId }

        goals.map { goal ->
            val goalLogs = logsByGoal[goal.id].orEmpty()
            val dailyTotalsMap = goalLogs
                .groupBy { it.dateKey }
                .mapValues { (_, dayLogs) -> dayLogs.sumOf { it.amount } }

            val todayLogged = dailyTotalsMap[todayKey] ?: goal.currentProgress
            val last7DayTotals = last7Days.map { (dateKey, shortLabel) ->
                DayTotal(
                    dateKey = dateKey,
                    shortDayLabel = shortLabel,
                    amount = dailyTotalsMap[dateKey] ?: 0,
                    target = goal.targetValue
                )
            }

            val (currentStreak, bestStreak) = calculateStreaks(
                dailyTotalsMap = dailyTotalsMap,
                targetValue = goal.targetValue
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
    }

    val weeklySummaryFlow: Flow<WeeklySummary> = combine(
        goalDao.observeAllGoals(),
        logDao.observeAllLogs()
    ) { goals, logs ->
        val todayKey = DateKeyUtils.todayKey()
        val last7Days = DateKeyUtils.lastNDaysKeysWithLabels(7)
        val last7KeysSet = last7Days.map { it.first }.toSet()

        val logsByGoalAndDate = logs
            .groupBy { it.goalId }
            .mapValues { (_, goalLogs) ->
                goalLogs.groupBy { it.dateKey }.mapValues { (_, dayLogs) -> dayLogs.sumOf { it.amount } }
            }

        val totalActiveGoals = goals.size
        val completedTodayCount = goals.count { goal ->
            val logged = logsByGoalAndDate[goal.id]?.get(todayKey) ?: goal.currentProgress
            goal.targetValue > 0 && logged >= goal.targetValue
        }

        val dailyOverallFraction = if (totalActiveGoals == 0) {
            0f
        } else {
            val averageGoalProgress = goals.sumOf { goal ->
                val logged = logsByGoalAndDate[goal.id]?.get(todayKey) ?: goal.currentProgress
                val ratio = if (goal.targetValue <= 0) 0.0
                else (logged.toDouble() / goal.targetValue.toDouble()).coerceIn(0.0, 1.0)
                ratio
            } / totalActiveGoals.toDouble()
            averageGoalProgress.toFloat()
        }

        val last7DayRates = last7Days.map { (dateKey, label) ->
            val completedOnDay = goals.count { goal ->
                val logged = logsByGoalAndDate[goal.id]?.get(dateKey) ?: 0
                goal.targetValue > 0 && logged >= goal.targetValue
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

        val bestActiveStreak = goals.maxOfOrNull { goal ->
            val dailyMap = logsByGoalAndDate[goal.id].orEmpty()
            calculateStreaks(dailyMap, goal.targetValue).second
        } ?: 0

        WeeklySummary(
            completedTodayCount = completedTodayCount,
            totalActiveGoals = totalActiveGoals,
            dailyOverallFraction = dailyOverallFraction,
            weeklyCompletionPercent = weeklyCompletionPercent,
            totalCheckInsThisWeek = totalCheckInsThisWeek,
            bestActiveStreak = bestActiveStreak,
            last7DayRates = last7DayRates
        )
    }

    val recentActivityFlow: Flow<List<LogWithGoalInfo>> = combine(
        goalDao.observeAllGoals(),
        logDao.observeAllLogs()
    ) { goals, logs ->
        val goalsById = goals.associateBy { it.id }
        logs.mapNotNull { log ->
            val goal = goalsById[log.goalId] ?: return@mapNotNull null
            LogWithGoalInfo(
                log = log,
                goalTitle = goal.title,
                goalUnit = goal.unit,
                category = GoalCategory.fromString(goal.category)
            )
        }.take(40)
    }

    suspend fun createGoal(
        title: String,
        description: String = "",
        category: GoalCategory,
        targetValue: Int,
        unit: String,
        quickIncrement: Int,
        startDate: Long = System.currentTimeMillis(),
        endDate: Long? = null
    ): Long {
        val goal = FitnessGoal(
            title = title.trim(),
            description = description.trim(),
            category = category.name,
            targetValue = targetValue.coerceAtLeast(1),
            currentProgress = 0,
            unit = unit.trim().ifBlank { category.defaultUnit },
            quickIncrement = quickIncrement.coerceAtLeast(1),
            startDate = startDate,
            endDate = endDate
        )
        return goalDao.insertGoal(goal)
    }

    suspend fun updateGoal(goal: FitnessGoal) {
        goalDao.updateGoal(goal)
    }

    suspend fun deleteGoal(goalId: Long) {
        goalDao.deleteGoalById(goalId)
    }

    suspend fun logProgress(goalId: Long, amount: Int, note: String = "") {
        if (amount <= 0) return
        val todayKey = DateKeyUtils.todayKey()
        val log = GoalLogEntity(
            goalId = goalId,
            amount = amount,
            dateKey = todayKey,
            note = note.trim()
        )
        logDao.insertLog(log)
        val todayTotal = logDao.getTotalLoggedForGoalOnDate(goalId, todayKey)
        goalDao.updateProgress(goalId, todayTotal)
    }

    suspend fun deleteLog(logId: Long) {
        val existing = logDao.getLogById(logId)
        logDao.deleteLogById(logId)
        if (existing != null && existing.dateKey == DateKeyUtils.todayKey()) {
            val todayTotal = logDao.getTotalLoggedForGoalOnDate(existing.goalId, existing.dateKey)
            goalDao.updateProgress(existing.goalId, todayTotal)
        }
    }

    suspend fun addStarterGoals() {
        val existing = goalDao.getAllGoalsOnce()
        if (existing.isNotEmpty()) return
        val now = System.currentTimeMillis()
        val thirtyDaysOut = now + TimeUnit.DAYS.toMillis(30)
        val starters = listOf(
            FitnessGoal(
                title = "Daily Steps",
                description = "Hit 8,000 steps daily for steady cardiovascular endurance.",
                category = GoalCategory.STEPS.name,
                targetValue = 8000,
                currentProgress = 0,
                unit = "steps",
                quickIncrement = 1000,
                startDate = now,
                endDate = thirtyDaysOut,
                createdAtMillis = now
            ),
            FitnessGoal(
                title = "Cardio Session",
                description = "30 minutes of brisk running, cycling, or HIIT.",
                category = GoalCategory.CARDIO.name,
                targetValue = 30,
                currentProgress = 0,
                unit = "min",
                quickIncrement = 10,
                startDate = now,
                endDate = thirtyDaysOut,
                createdAtMillis = now + 1
            ),
            FitnessGoal(
                title = "Hydration",
                description = "Drink 8 full glasses of water throughout the day.",
                category = GoalCategory.HYDRATION.name,
                targetValue = 8,
                currentProgress = 0,
                unit = "glasses",
                quickIncrement = 1,
                startDate = now,
                endDate = null,
                createdAtMillis = now + 2
            ),
            FitnessGoal(
                title = "Mobility & Stretch",
                description = "15 minutes of joint mobility and post-workout stretching.",
                category = GoalCategory.MOBILITY.name,
                targetValue = 15,
                currentProgress = 0,
                unit = "min",
                quickIncrement = 5,
                startDate = now,
                endDate = thirtyDaysOut,
                createdAtMillis = now + 3
            )
        )
        goalDao.insertGoals(starters)
    }

    suspend fun getTodayPendingSummary(): Pair<Int, Int> {
        val goals = goalDao.getAllGoalsOnce()
        if (goals.isEmpty()) return 0 to 0
        val todayLogs = logDao.getLogsForDateOnce(DateKeyUtils.todayKey())
        val totalsByGoal = todayLogs.groupBy { it.goalId }.mapValues { (_, list) -> list.sumOf { it.amount } }
        val remaining = goals.count { goal ->
            val logged = totalsByGoal[goal.id] ?: 0
            logged < goal.targetValue
        }
        return remaining to goals.size
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
            val total = dailyTotalsMap[key] ?: 0
            if (total >= targetValue) {
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
}
