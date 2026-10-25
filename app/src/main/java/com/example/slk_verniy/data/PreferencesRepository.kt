package com.example.slk_verniy.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.LocalDate

class PreferencesRepository(private val context: Context) {

    companion object {
        private val SHIFT_START_DATE_KEY = stringPreferencesKey("shift_start_date")
        private val EXTRA_SHIFT_DATES_KEY = stringSetPreferencesKey("extra_shift_dates")
        private val NOTIFICATION_HOUR_KEY = intPreferencesKey("notification_hour")
        private val NOTIFICATION_MINUTE_KEY = intPreferencesKey("notification_minute")
        private val SHIFT_START_HOUR_KEY = intPreferencesKey("shift_start_hour")
        private val SHIFT_START_MINUTE_KEY = intPreferencesKey("shift_start_minute")
        private val SHIFT_END_HOUR_KEY = intPreferencesKey("shift_end_hour")
        private val SHIFT_END_MINUTE_KEY = intPreferencesKey("shift_end_minute")
    }

    val shiftStartDate: Flow<LocalDate?> = context.dataStore.data
        .catch { _ ->
            try {
                emit(emptyPreferences())
            } catch (_: Exception) {
                // Fallback
            }
        }
        .map { preferences ->
            try {
                val dateString = preferences[SHIFT_START_DATE_KEY]
                if (!dateString.isNullOrBlank()) {
                    LocalDate.parse(dateString)
                } else {
                    null
                }
            } catch (_: Exception) {
                null
            }
        }

    val extraShifts: Flow<Set<LocalDate>> = context.dataStore.data
        .catch { _ ->
            try {
                emit(emptyPreferences())
            } catch (_: Exception) {
                // Fallback
            }
        }
        .map { preferences ->
            try {
                val dateStrings = preferences[EXTRA_SHIFT_DATES_KEY] ?: emptySet()
                dateStrings.mapNotNull {
                    try {
                        LocalDate.parse(it)
                    } catch (_: Exception) {
                        null
                    }
                }.toSet()
            } catch (_: Exception) {
                emptySet()
            }
        }

    val notificationHour: Flow<Int> = context.dataStore.data
        .catch { _ ->
            try {
                emit(emptyPreferences())
            } catch (_: Exception) {}
        }
        .map { preferences ->
            try {
                preferences[NOTIFICATION_HOUR_KEY] ?: 20
            } catch (_: Exception) {
                20
            }
        }

    val notificationMinute: Flow<Int> = context.dataStore.data
        .catch { _ ->
            try {
                emit(emptyPreferences())
            } catch (_: Exception) {}
        }
        .map { preferences ->
            try {
                preferences[NOTIFICATION_MINUTE_KEY] ?: 0
            } catch (_: Exception) {
                0
            }
        }

    val shiftStartHour: Flow<Int> = context.dataStore.data
        .catch { _ ->
            try {
                emit(emptyPreferences())
            } catch (_: Exception) {}
        }
        .map { preferences ->
            try {
                preferences[SHIFT_START_HOUR_KEY] ?: 11
            } catch (_: Exception) {
                11
            }
        }

    val shiftStartMinute: Flow<Int> = context.dataStore.data
        .catch { _ ->
            try {
                emit(emptyPreferences())
            } catch (_: Exception) {}
        }
        .map { preferences ->
            try {
                preferences[SHIFT_START_MINUTE_KEY] ?: 0
            } catch (_: Exception) {
                0
            }
        }

    val shiftEndHour: Flow<Int> = context.dataStore.data
        .catch { _ ->
            try {
                emit(emptyPreferences())
            } catch (_: Exception) {}
        }
        .map { preferences ->
            try {
                preferences[SHIFT_END_HOUR_KEY] ?: 23
            } catch (_: Exception) {
                23
            }
        }

    val shiftEndMinute: Flow<Int> = context.dataStore.data
        .catch { _ ->
            try {
                emit(emptyPreferences())
            } catch (_: Exception) {}
        }
        .map { preferences ->
            try {
                preferences[SHIFT_END_MINUTE_KEY] ?: 0
            } catch (_: Exception) {
                0
            }
        }

    suspend fun saveShiftStartDate(date: LocalDate?) {
        withContext(Dispatchers.IO + NonCancellable) {
            try {
                context.dataStore.edit { preferences ->
                    if (date != null) {
                        preferences[SHIFT_START_DATE_KEY] = date.toString()
                    } else {
                        preferences.remove(SHIFT_START_DATE_KEY)
                    }
                }
            } catch (_: Exception) {
                // Log or handle DataStore write exception
            }
        }
    }

    suspend fun saveExtraShifts(shifts: Set<LocalDate>) {
        withContext(Dispatchers.IO + NonCancellable) {
            try {
                context.dataStore.edit { preferences ->
                    preferences[EXTRA_SHIFT_DATES_KEY] = shifts.map { it.toString() }.toSet()
                }
            } catch (_: Exception) {
                // Log or handle DataStore write exception
            }
        }
    }

    suspend fun saveNotificationTime(hour: Int, minute: Int) {
        withContext(Dispatchers.IO + NonCancellable) {
            try {
                context.dataStore.edit { preferences ->
                    preferences[NOTIFICATION_HOUR_KEY] = hour
                    preferences[NOTIFICATION_MINUTE_KEY] = minute
                }
            } catch (_: Exception) {
                // Log or handle DataStore write exception
            }
        }
    }

    suspend fun saveShiftStartTime(hour: Int, minute: Int) {
        withContext(Dispatchers.IO + NonCancellable) {
            try {
                context.dataStore.edit { preferences ->
                    preferences[SHIFT_START_HOUR_KEY] = hour
                    preferences[SHIFT_START_MINUTE_KEY] = minute
                }
            } catch (_: Exception) {
                // Log or handle DataStore write exception
            }
        }
    }

    suspend fun saveShiftEndTime(hour: Int, minute: Int) {
        withContext(Dispatchers.IO + NonCancellable) {
            try {
                context.dataStore.edit { preferences ->
                    preferences[SHIFT_END_HOUR_KEY] = hour
                    preferences[SHIFT_END_MINUTE_KEY] = minute
                }
            } catch (_: Exception) {
                // Log or handle DataStore write exception
            }
        }
    }
}
