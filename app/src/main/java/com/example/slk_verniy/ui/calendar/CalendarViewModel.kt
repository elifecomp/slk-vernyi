package com.example.slk_verniy.ui.calendar

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.slk_verniy.data.EmployeeProfile
import com.example.slk_verniy.data.MonthlySalaryResult
import com.example.slk_verniy.data.PreferencesRepository
import com.example.slk_verniy.data.ProfilePreferencesManager
import com.example.slk_verniy.data.SalaryPreferencesManager
import com.example.slk_verniy.data.SalarySettings
import com.example.slk_verniy.worker.ShiftReminderWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

enum class ShiftType {
    WORK_DAY,
    REST_DAY,
    EXTRA_SHIFT,
    NONE
}

class CalendarViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = PreferencesRepository(application)
    private val salaryPreferencesManager = SalaryPreferencesManager(application)
    private val profilePreferencesManager = ProfilePreferencesManager(application)

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Application
                CalendarViewModel(application)
            }
        }
    }

    private val _selectedDate = MutableStateFlow<LocalDate?>(null)
    val selectedDate: StateFlow<LocalDate?> = _selectedDate.asStateFlow()

    val shiftStartDate: StateFlow<LocalDate?> = repository.shiftStartDate
        .catch { emit(null) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val extraShifts: StateFlow<Set<LocalDate>> = repository.extraShifts
        .catch { emit(emptySet()) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptySet()
        )

    val salarySettings: StateFlow<SalarySettings> = salaryPreferencesManager.salarySettings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SalarySettings()
        )

    val employeeProfile: StateFlow<EmployeeProfile> = profilePreferencesManager.employeeProfile
        .catch { emit(EmployeeProfile()) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = EmployeeProfile()
        )

    val avatarUri: StateFlow<String?> = profilePreferencesManager.avatarUri
        .catch { emit(null) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val name: StateFlow<String> = profilePreferencesManager.name
        .catch { emit("Анастасия Путилина") }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "Анастасия Путилина"
        )

    init {
        viewModelScope.launch {
            try {
                val initialDate = repository.shiftStartDate.first()
                if (_selectedDate.value == null) {
                    _selectedDate.value = initialDate
                }
            } catch (_: Exception) {
                // Ignore or handle
            }
        }
        try {
            ShiftReminderWorker.schedule(application)
        } catch (_: Exception) {
            // Ignore worker scheduling exceptions
        }
    }

    fun onDateSelected(date: LocalDate) {
        if (_selectedDate.value == date) {
            _selectedDate.value = null
            setShiftStartDate(null)
        } else {
            _selectedDate.value = date
            setShiftStartDate(date)
        }
    }

    private fun setShiftStartDate(date: LocalDate?) {
        viewModelScope.launch {
            try {
                repository.saveShiftStartDate(date)
                if (date != null) {
                    ShiftReminderWorker.schedule(getApplication())
                }
            } catch (_: Exception) {
                // Handle exception
            }
        }
    }

    fun toggleExtraShift(date: LocalDate) {
        viewModelScope.launch {
            try {
                val current = extraShifts.value.toMutableSet()
                if (current.contains(date)) {
                    current.remove(date)
                } else {
                    current.add(date)
                }
                repository.saveExtraShifts(current)
            } catch (_: Exception) {
                // Handle exception
            }
        }
    }

    fun getShiftTypeForDate(targetDate: LocalDate): ShiftType {
        if (extraShifts.value.contains(targetDate)) {
            return ShiftType.EXTRA_SHIFT
        }

        val start = shiftStartDate.value ?: return ShiftType.NONE
        if (targetDate.isBefore(start)) {
            return ShiftType.NONE
        }

        return try {
            val daysDifference = ChronoUnit.DAYS.between(start, targetDate)
            val cycleDay = daysDifference % 4

            when (cycleDay.toInt()) {
                0, 1 -> ShiftType.WORK_DAY
                2, 3 -> ShiftType.REST_DAY
                else -> ShiftType.NONE
            }
        } catch (_: Exception) {
            ShiftType.NONE
        }
    }

    fun updateSalarySettings(shiftRate: Float) {
        viewModelScope.launch {
            try {
                salaryPreferencesManager.updateSalarySettings(shiftRate)
            } catch (_: Exception) {
                // Handle exception
            }
        }
    }

    fun calculateMonthlySalary(
        yearMonth: YearMonth,
        shiftStartDate: LocalDate?,
        settings: SalarySettings
    ): MonthlySalaryResult {
        return try {
            val extra = extraShifts.value
            val length = yearMonth.lengthOfMonth()
            var workDaysCount = 0
            for (day in 1..length) {
                val date = yearMonth.atDay(day)
                val isExtra = extra.contains(date)
                val isRegularWork = if (shiftStartDate != null && !date.isBefore(shiftStartDate)) {
                    val daysDifference = ChronoUnit.DAYS.between(shiftStartDate, date)
                    val cycleDay = daysDifference % 4
                    cycleDay == 0L || cycleDay == 1L
                } else {
                    false
                }
                if (isExtra || isRegularWork) {
                    workDaysCount++
                }
            }
            val totalSalary = workDaysCount * settings.shiftRate
            MonthlySalaryResult(workDaysCount, totalSalary)
        } catch (_: Exception) {
            MonthlySalaryResult(0, 0f)
        }
    }
}
