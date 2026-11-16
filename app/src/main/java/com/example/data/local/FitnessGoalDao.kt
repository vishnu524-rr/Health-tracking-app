package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FitnessGoalDao {

    @Query("SELECT * FROM fitness_goals WHERE isArchived = 0 ORDER BY createdAtMillis ASC")
    fun observeAllGoals(): Flow<List<FitnessGoal>>

    @Query("SELECT * FROM fitness_goals WHERE isArchived = 0 ORDER BY createdAtMillis ASC")
    suspend fun getAllGoalsOnce(): List<FitnessGoal>

    @Query("SELECT * FROM fitness_goals WHERE id = :goalId LIMIT 1")
    fun observeGoalById(goalId: Long): Flow<FitnessGoal?>

    @Query("SELECT * FROM fitness_goals WHERE id = :goalId LIMIT 1")
    suspend fun getGoalById(goalId: Long): FitnessGoal?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: FitnessGoal): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoals(goals: List<FitnessGoal>)

    @Update
    suspend fun updateGoal(goal: FitnessGoal)

    @Query("UPDATE fitness_goals SET current_progress = :currentProgress WHERE id = :goalId")
    suspend fun updateProgress(goalId: Long, currentProgress: Int)

    @Query("UPDATE fitness_goals SET current_progress = MAX(0, current_progress + :delta) WHERE id = :goalId")
    suspend fun incrementProgress(goalId: Long, delta: Int)

    @Delete
    suspend fun deleteGoal(goal: FitnessGoal)

    @Query("DELETE FROM fitness_goals WHERE id = :goalId")
    suspend fun deleteGoalById(goalId: Long)
}
