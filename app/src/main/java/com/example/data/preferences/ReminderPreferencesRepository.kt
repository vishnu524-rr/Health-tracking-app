package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.reminderDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "pulsegoal_reminders"
)

data class ReminderSettings(
    val isEnabled: Boolean = true,
    val hour: Int = 8,
    val minute: Int = 30,
    val activeDays: Set<Int> = setOf(1, 2, 3, 4, 5, 6, 7),
    val customMessage: String = "",
    val includeGoalSummary: Boolean = true
)

class ReminderPreferencesRepository(context: Context) {
    private val dataStore = context.applicationContext.reminderDataStore

    private object Keys {
        val ENABLED = booleanPreferencesKey("reminder_enabled")
        val HOUR = intPreferencesKey("reminder_hour")
        val MINUTE = intPreferencesKey("reminder_minute")
        val ACTIVE_DAYS = stringSetPreferencesKey("reminder_active_days")
        val CUSTOM_MESSAGE = stringPreferencesKey("reminder_custom_message")
        val INCLUDE_GOAL_SUMMARY = booleanPreferencesKey("reminder_include_summary")
    }

    val settingsFlow: Flow<ReminderSettings> = dataStore.data.map { prefs ->
        val dayStrings = prefs[Keys.ACTIVE_DAYS]
        val parsedDays = dayStrings
            ?.mapNotNull { it.toIntOrNull() }
            ?.filter { it in 1..7 }
            ?.toSet()
            ?: setOf(1, 2, 3, 4, 5, 6, 7)

        ReminderSettings(
            isEnabled = prefs[Keys.ENABLED] ?: true,
            hour = (prefs[Keys.HOUR] ?: 8).coerceIn(0, 23),
            minute = (prefs[Keys.MINUTE] ?: 30).coerceIn(0, 59),
            activeDays = parsedDays,
            customMessage = prefs[Keys.CUSTOM_MESSAGE] ?: "",
            includeGoalSummary = prefs[Keys.INCLUDE_GOAL_SUMMARY] ?: true
        )
    }

    suspend fun getSettingsOnce(): ReminderSettings = settingsFlow.first()

    suspend fun setEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.ENABLED] = enabled }
    }

    suspend fun setTime(hour: Int, minute: Int) {
        dataStore.edit {
            it[Keys.HOUR] = hour.coerceIn(0, 23)
            it[Keys.MINUTE] = minute.coerceIn(0, 59)
        }
    }

    suspend fun toggleActiveDay(isoDay: Int) {
        if (isoDay !in 1..7) return
        dataStore.edit { prefs ->
            val current = prefs[Keys.ACTIVE_DAYS]
                ?.mapNotNull { it.toIntOrNull() }
                ?.toMutableSet()
                ?: mutableSetOf(1, 2, 3, 4, 5, 6, 7)
            if (current.contains(isoDay)) {
                if (current.size > 1) {
                    current.remove(isoDay)
                }
            } else {
                current.add(isoDay)
            }
            prefs[Keys.ACTIVE_DAYS] = current.map { it.toString() }.toSet()
        }
    }

    suspend fun setCustomMessage(message: String) {
        dataStore.edit { it[Keys.CUSTOM_MESSAGE] = message }
    }

    suspend fun setIncludeGoalSummary(include: Boolean) {
        dataStore.edit { it[Keys.INCLUDE_GOAL_SUMMARY] = include }
    }
}
