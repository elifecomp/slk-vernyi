package com.example.slk_verniy.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

data class SalarySettings(
    val shiftRate: Float = 0f
)

data class MonthlySalaryResult(
    val workDaysCount: Int,
    val totalSalary: Float
)

class SalaryPreferencesManager(private val context: Context) {

    companion object {
        private val SHIFT_RATE_KEY = floatPreferencesKey("shift_rate")
    }

    val salarySettings: Flow<SalarySettings> = context.dataStore.data
        .catch { _ ->
            try {
                emit(emptyPreferences())
            } catch (_: Exception) {
                // Fallback if emit fails
            }
        }
        .map { preferences ->
            try {
                val shiftRate = preferences[SHIFT_RATE_KEY] ?: 0f
                SalarySettings(shiftRate = shiftRate)
            } catch (_: Exception) {
                SalarySettings()
            }
        }

    suspend fun updateSalarySettings(shiftRate: Float) {
        withContext(Dispatchers.IO + NonCancellable) {
            try {
                context.dataStore.edit { preferences ->
                    preferences[SHIFT_RATE_KEY] = shiftRate
                }
            } catch (_: Exception) {
                // Log or handle DataStore write exception
            }
        }
    }
}
