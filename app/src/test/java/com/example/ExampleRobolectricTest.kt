package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.FitnessGoal
import com.example.data.local.GoalLogEntity
import com.example.data.local.PulseGoalDatabase
import com.example.data.model.DateKeyUtils
import com.example.data.model.GoalCategory
import com.example.data.repository.FitnessRepository
import com.example.notifications.DailyReminderScheduler
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `app name matches PulseGoal`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("PulseGoal", appName)
    }

    @Test
    fun `fitnessGoalDao persists description target currentProgress and start end dates`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, PulseGoalDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val goalDao = db.fitnessGoalDao()

        val start = System.currentTimeMillis()
        val end = start + TimeUnit.DAYS.toMillis(30)
        val goalId = goalDao.insertGoal(
            FitnessGoal(
                title = "5K Prep",
                description = "Run 5km three times per week",
                category = GoalCategory.CARDIO.name,
                targetValue = 15,
                currentProgress = 5,
                unit = "km",
                quickIncrement = 5,
                startDate = start,
                endDate = end
            )
        )

        goalDao.incrementProgress(goalId, 5)
        val loaded = goalDao.getGoalById(goalId)
        assertNotNull(loaded)
        assertEquals("Run 5km three times per week", loaded!!.description)
        assertEquals(15, loaded.targetValue)
        assertEquals(10, loaded.currentProgress)
        assertEquals(start, loaded.startDate)
        assertEquals(end, loaded.endDate)

        db.close()
    }

    @Test
    fun `fitness repository tracks progress and streaks`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, PulseGoalDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val goalDao = db.fitnessGoalDao()
        val logDao = db.fitnessDao()
        val repo = FitnessRepository(goalDao, logDao)

        val goalId = repo.createGoal(
            title = "Morning Run",
            description = "Daily outdoor cardio",
            category = GoalCategory.CARDIO,
            targetValue = 30,
            unit = "min",
            quickIncrement = 10
        )

        logDao.insertLog(
            GoalLogEntity(
                goalId = goalId,
                amount = 30,
                dateKey = DateKeyUtils.dateKeyDaysAgo(1)
            )
        )
        repo.logProgress(goalId, 30, "Tempo run")

        val goals = repo.goalsWithProgressFlow.first()
        assertEquals(1, goals.size)
        val progress = goals.first()
        assertEquals(30, progress.todayLogged)
        assertEquals(30, progress.goal.currentProgress)
        assertTrue(progress.isCompletedToday)
        assertEquals(2, progress.currentStreakDays)
        assertEquals(2, progress.bestStreakDays)

        db.close()
    }

    @Test
    fun `daily reminder scheduler skips empty active days and picks next valid slot`() {
        assertNull(DailyReminderScheduler.computeNextTriggerMillis(8, 30, emptySet()))

        val now = Calendar.getInstance().apply {
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, Calendar.NOVEMBER)
            set(Calendar.DAY_OF_MONTH, 16)
            set(Calendar.HOUR_OF_DAY, 10)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val trigger = DailyReminderScheduler.computeNextTriggerMillis(
            hour = 8,
            minute = 30,
            activeDays = setOf(3),
            nowMillis = now
        )
        assertNotNull(trigger)
        val triggerCal = Calendar.getInstance().apply { timeInMillis = trigger!! }
        assertEquals(Calendar.WEDNESDAY, triggerCal.get(Calendar.DAY_OF_WEEK))
        assertEquals(8, triggerCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(30, triggerCal.get(Calendar.MINUTE))
    }
}
