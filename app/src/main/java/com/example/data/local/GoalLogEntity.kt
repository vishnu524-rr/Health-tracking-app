package com.example.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "goal_logs",
    foreignKeys = [
        ForeignKey(
            entity = FitnessGoal::class,
            parentColumns = ["id"],
            childColumns = ["goalId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("goalId"), Index("dateKey")]
)
data class GoalLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val goalId: Long,
    val amount: Int,
    val dateKey: String,
    val note: String = "",
    val loggedAtMillis: Long = System.currentTimeMillis()
)
