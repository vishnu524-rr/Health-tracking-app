package com.example.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "fitness_goals")
data class FitnessGoal(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val title: String,
    @ColumnInfo(name = "description")
    val description: String = "",
    val category: String = "CARDIO",
    @ColumnInfo(name = "target_value")
    val targetValue: Int,
    @ColumnInfo(name = "current_progress")
    val currentProgress: Int = 0,
    val unit: String = "min",
    val quickIncrement: Int = 10,
    @ColumnInfo(name = "start_date")
    val startDate: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "end_date")
    val endDate: Long? = null,
    val isArchived: Boolean = false,
    val createdAtMillis: Long = System.currentTimeMillis()
)
