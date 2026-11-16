package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [FitnessGoal::class, GoalLogEntity::class],
    version = 2,
    exportSchema = false
)
abstract class PulseGoalDatabase : RoomDatabase() {
    abstract fun fitnessGoalDao(): FitnessGoalDao
    abstract fun fitnessDao(): FitnessDao

    companion object {
        @Volatile
        private var INSTANCE: PulseGoalDatabase? = null

        fun getInstance(context: Context): PulseGoalDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    PulseGoalDatabase::class.java,
                    "pulsegoal.db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
