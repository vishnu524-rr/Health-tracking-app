package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FitnessDao {

    @Query("SELECT * FROM goal_logs ORDER BY loggedAtMillis DESC")
    fun observeAllLogs(): Flow<List<GoalLogEntity>>

    @Query("SELECT * FROM goal_logs WHERE id = :logId LIMIT 1")
    suspend fun getLogById(logId: Long): GoalLogEntity?

    @Query("SELECT * FROM goal_logs WHERE dateKey = :dateKey ORDER BY loggedAtMillis DESC")
    suspend fun getLogsForDateOnce(dateKey: String): List<GoalLogEntity>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM goal_logs WHERE goalId = :goalId AND dateKey = :dateKey")
    suspend fun getTotalLoggedForGoalOnDate(goalId: Long, dateKey: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: GoalLogEntity): Long

    @Query("DELETE FROM goal_logs WHERE id = :logId")
    suspend fun deleteLogById(logId: Long)
}
