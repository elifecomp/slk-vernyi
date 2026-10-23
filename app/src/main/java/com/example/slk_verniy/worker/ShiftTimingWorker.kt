package com.example.slk_verniy.worker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.slk_verniy.data.PreferencesRepository
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

class ShiftTimingWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val WORK_NAME_MORNING = "ShiftTimingMorningPeriodicWork"
        private const val WORK_NAME_EVENING = "ShiftTimingEveningPeriodicWork"
        const val CHANNEL_ID = "shift_timing_channel"
        private const val NOTIFICATION_ID_MORNING = 1003
        private const val NOTIFICATION_ID_EVENING = 1004

        fun sendTestNotificationMorning(context: Context) {
            sendNotification(context, "Смена ваша началась! 🛠️", "Смена ваша началась! 🛠️", NOTIFICATION_ID_MORNING)
        }

        fun sendTestNotificationEvening(context: Context) {
            sendNotification(context, "Ваша смена окончилась! 🌙", "Ваша смена окончилась! 🌙", NOTIFICATION_ID_EVENING)
        }

        private fun sendNotification(context: Context, title: String, text: String, notificationId: Int) {
            try {
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                    ?: return

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val name = "Уведомления о сменах"
                    val descriptionText = "Уведомления о начале и окончании смен"
                    val importance = NotificationManager.IMPORTANCE_DEFAULT
                    val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                        description = descriptionText
                    }
                    notificationManager.createNotificationChannel(channel)
                }

                val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.ic_menu_today)
                    .setContentTitle(title)
                    .setContentText(text)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setAutoCancel(true)

                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    NotificationManagerCompat.from(context).notify(notificationId, builder.build())
                }
            } catch (_: Exception) {
                // Catch SecurityException or other notification errors
            }
        }

        fun schedule(context: Context) {
            try {
                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                    .build()

                // Schedule 11:00 AM (Morning)
                val morningCalendar = java.util.Calendar.getInstance()
                val morningDueDate = java.util.Calendar.getInstance().apply {
                    set(java.util.Calendar.HOUR_OF_DAY, 11)
                    set(java.util.Calendar.MINUTE, 0)
                    set(java.util.Calendar.SECOND, 0)
                }
                if (morningCalendar.after(morningDueDate)) {
                    morningDueDate.add(java.util.Calendar.DAY_OF_MONTH, 1)
                }
                val morningDelay = maxOf(0L, morningDueDate.timeInMillis - morningCalendar.timeInMillis)

                val morningInput = androidx.work.workDataOf("is_evening" to false)
                val morningRequest = PeriodicWorkRequestBuilder<ShiftTimingWorker>(24, TimeUnit.HOURS)
                    .setInitialDelay(morningDelay, TimeUnit.MILLISECONDS)
                    .setInputData(morningInput)
                    .setConstraints(constraints)
                    .build()

                WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    WORK_NAME_MORNING,
                    ExistingPeriodicWorkPolicy.UPDATE,
                    morningRequest
                )

                // Schedule 23:00 PM (Evening)
                val eveningCalendar = java.util.Calendar.getInstance()
                val eveningDueDate = java.util.Calendar.getInstance().apply {
                    set(java.util.Calendar.HOUR_OF_DAY, 23)
                    set(java.util.Calendar.MINUTE, 0)
                    set(java.util.Calendar.SECOND, 0)
                }
                if (eveningCalendar.after(eveningDueDate)) {
                    eveningDueDate.add(java.util.Calendar.DAY_OF_MONTH, 1)
                }
                val eveningDelay = maxOf(0L, eveningDueDate.timeInMillis - eveningCalendar.timeInMillis)

                val eveningInput = androidx.work.workDataOf("is_evening" to true)
                val eveningRequest = PeriodicWorkRequestBuilder<ShiftTimingWorker>(24, TimeUnit.HOURS)
                    .setInitialDelay(eveningDelay, TimeUnit.MILLISECONDS)
                    .setInputData(eveningInput)
                    .setConstraints(constraints)
                    .build()

                WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    WORK_NAME_EVENING,
                    ExistingPeriodicWorkPolicy.UPDATE,
                    eveningRequest
                )
            } catch (_: Exception) {
                // Handle scheduling exception
            }
        }
    }

    override suspend fun doWork(): Result {
        return try {
            val repository = PreferencesRepository(applicationContext)
            val shiftStartDate = repository.shiftStartDate.first()
            val extraShifts = repository.extraShifts.first()

            val today = LocalDate.now()
            if (isWorkDay(today, shiftStartDate, extraShifts)) {
                val isEvening = inputData.getBoolean("is_evening", LocalTime.now().hour >= 17)
                if (isEvening) {
                    sendNotification(
                        applicationContext,
                        "Ваша смена окончилась! 🌙",
                        "Ваша смена окончилась! 🌙",
                        NOTIFICATION_ID_EVENING
                    )
                } else {
                    sendNotification(
                        applicationContext,
                        "Смена ваша началась! 🛠️",
                        "Смена ваша началась! 🛠️",
                        NOTIFICATION_ID_MORNING
                    )
                }
            }

            Result.success()
        } catch (_: Exception) {
            Result.failure()
        }
    }
}

fun isWorkDay(date: LocalDate, shiftStartDate: LocalDate?, extraShifts: Set<LocalDate>): Boolean {
    if (extraShifts.contains(date)) {
        return true
    }
    if (shiftStartDate == null) {
        return false
    }
    if (date.isBefore(shiftStartDate)) {
        return false
    }
    val daysDifference = ChronoUnit.DAYS.between(shiftStartDate, date)
    val cycleDay = daysDifference % 4
    return cycleDay == 0L || cycleDay == 1L
}
