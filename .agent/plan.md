# Project Plan

Android app named 'Моя Работа' (My Work). Core feature: Calendar showing a rolling shift schedule of 2 working days followed by 2 days off (2/2 schedule). The user can pick a starting date of their shift, and the app will automatically calculate and highlight the working days and rest days on the calendar.

## Project Brief

# Project Brief: Моя Работа (My Work)

## Features
1. **Interactive Calendar View:** A visually clear, scrollable monthly calendar display.
2. **Shift Start Selection:** Users can select any date on the calendar to designate it as the starting point (Day 1) of their shift.
3. **2/2 Schedule Auto-Calculation & Highlighting:** The app automatically calculates a rolling schedule (2 days of work followed by 2 days of rest) based on the chosen start date and highlights working days and rest days with distinct colors.

## High-Level Tech Stack
* **Language:** Kotlin
* **UI Toolkit:** Jetpack Compose (using Material 3 components)
* **Navigation & Adaptive Strategy:** **Jetpack Navigation 3** (state-driven) combined with the **Compose Material Adaptive** library to gracefully handle layouts across different device form factors.
* **Architecture & State:** Android Architecture Components (ViewModel) combined with Kotlin Coroutines and StateFlow for reactive UI state management and asynchronous date calculations.

## Implementation Steps
**Total Duration:** 1h 7m 13s

### Task_1_CalendarUI: Implement the interactive, scrollable monthly calendar UI using Jetpack Compose Material 3.
- **Status:** COMPLETED
- **Updates:** The coder_agent implemented a robust CalendarViewModel and a scrollable Calendar UI using LazyColumn. It uses java.time APIs, has smooth color transitions with Material 3, highlights current and selected dates, and the minSdk was updated to 26 for java.time support.
- **Acceptance Criteria:**
  - Calendar UI displays months and days correctly
  - Calendar is scrollable
  - Dates are selectable
- **Duration:** 28m 44s

### Task_2_StateLogic: Implement ViewModel with Kotlin Coroutines and StateFlow to handle the selected start date and auto-calculate the 2/2 shift schedule.
- **Status:** COMPLETED
- **Updates:** The coder_agent added ShiftType enum and shiftStartDate state to CalendarViewModel. Implemented getShiftTypeForDate to calculate if a date is a WORK_DAY (mod 4 is 0 or 1) or REST_DAY (mod 4 is 2 or 3) based on the shift start date. The ViewModel state is ready for UI integration.
- **Acceptance Criteria:**
  - ViewModel accepts a start date
  - Calculates 2 work days and 2 rest days in a rolling pattern
  - Exposes state via StateFlow
- **Duration:** 48s

### Task_3_Integration: Integrate Calendar UI with ViewModel. Apply distinct color highlights to working days and rest days based on the calculated schedule.
- **Status:** COMPLETED
- **Updates:** The coder_agent successfully integrated the UI with the ViewModel. Tapping a date now sets the shiftStartDate. DayView uses Material 3 colors (tertiaryContainer for WORK_DAY, secondaryContainer for REST_DAY) to highlight the schedule. Adaptive UI is supported by adding a max width constraint (600.dp) so the calendar doesn't stretch infinitely on tablets.
- **Acceptance Criteria:**
  - Tapping a date updates the start date in ViewModel
  - Working days and rest days are clearly distinguishable by color
  - App accommodates different screen sizes using adaptive techniques
- **Duration:** 4m 14s

### Task_4_RunVerify: Run and Verify step. Instruct critic_agent to verify application stability, confirm alignment with 2/2 schedule user requirements, and report critical UI issues.
- **Status:** COMPLETED
- **Updates:** The critic_agent verified the app on a phone emulator. The app is stable with no crashes. Tapping a date correctly starts a 2/2 rolling shift, which is visually distinguishable by colors. UI layout looks correct and is edge-to-edge compliant. Validation passed.
- **Acceptance Criteria:**
  - make sure all existing tests pass
  - build pass
  - app does not crash
- **Duration:** 1m 11s

### Task_5_Persistence: Implement DataStore or SharedPreferences to persist the selected shift start date. Update CalendarViewModel to read this date upon initialization and save it when updated by the user.
- **Status:** COMPLETED
- **Updates:** The coder_agent fixed the persistence issue. The issues were: 1. viewModelScope cancellation before disk write completed. 2. Missing IOException catching. 3. Race condition between DataStore emissions and local state.
- **Acceptance Criteria:**
  - Preferences DataStore or SharedPreferences implemented
  - CalendarViewModel loads saved date on startup
  - CalendarViewModel saves new date on user selection
- **Duration:** 22m 56s

### Task_6_RunVerify_Persistence: Run and Verify step. Instruct critic_agent to verify that the app does not crash, and the 2/2 shift schedule start date successfully survives an app restart.
- **Status:** COMPLETED
- **Updates:** The critic_agent verified the persistence fix. The app was built, the 15th was selected, and after a simulated process death (force stop) and restart, the app successfully loaded the 15th as the start date with the 2/2 schedule accurately highlighted. The app is stable. Validation passed.
- **Acceptance Criteria:**
  - make sure all existing tests pass
  - build pass
  - app does not crash
  - Saved shift start date survives app restarts
- **Duration:** 4m 16s

### Task_7_SalaryCalculatorAndNotifications: Add WorkManager dependency and SalaryPreferencesManager to store hourly rate and shift duration. Update CalendarViewModel to calculate monthly salary based on work days and hours. Implement ShiftReminderWorker using WorkManager and request POST_NOTIFICATIONS permission.
- **Status:** COMPLETED
- **Updates:** The coder_agent added WorkManager dependency, created SalaryPreferencesManager, updated CalendarViewModel with salary calculations and settings state flow, and implemented ShiftReminderWorker for daily 20:00 shift reminders. Build and tests passed successfully.
- **Acceptance Criteria:**
  - WorkManager dependency added
  - SalaryPreferencesManager stores rate and hours
  - CalendarViewModel calculates monthly salary correctly
  - ShiftReminderWorker and notification permission integrated
- **Duration:** 3m 46s

### Task_8_SalaryUIAndVerification: Add MonthlySalaryCard under the calendar view, add settings dialog or inputs for salary/hours, and trigger worker scheduling. Run final verification with critic_agent.
- **Status:** COMPLETED
- **Updates:** The coder_agent added MonthlySalaryCard, SalarySettingsDialog for hourly rate and shift hours configuration, integrated workmanager shift reminders scheduling, and added POST_NOTIFICATIONS permission request for Android 13+. Build and tests passed successfully.
- **Acceptance Criteria:**
  - MonthlySalaryCard displays calculated salary
  - Settings UI allows configuring rate and hours
  - Worker scheduling triggered
  - make sure all existing tests pass
  - build pass
  - app does not crash
- **Duration:** 1m 18s

