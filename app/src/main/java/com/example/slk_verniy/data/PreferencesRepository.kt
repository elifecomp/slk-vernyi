package com.example.slk_verniy.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
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
}
