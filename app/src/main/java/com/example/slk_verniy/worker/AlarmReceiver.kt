package com.example.slk_verniy.worker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.slk_verniy.data.PreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return

        if (action == Intent.ACTION_BOOT_COMPLETED) {
            AlarmScheduler.scheduleAll(context)
            return
        }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repository = PreferencesRepository(context)
                val shiftStartDate = repository.shiftStartDate.first()
                val extraShifts = repository.extraShifts.first()
                val today = LocalDate.now()

                when (action) {
                    AlarmScheduler.ACTION_SHIFT_START_11 -> {
                        if (isWorkDay(today, shiftStartDate, extraShifts)) {
                            sendNotification(
                                context,
                                ShiftTimingWorker.CHANNEL_ID,
                                "Уведомления о сменах",
                                "Смена ваша началась!",
                                1003
                            )
                        }
                    }
                    AlarmScheduler.ACTION_SHIFT_END_23 -> {
                        if (isWorkDay(today, shiftStartDate, extraShifts)) {
                            sendNotification(
                                context,
                                ShiftTimingWorker.CHANNEL_ID,
                                "Уведомления о сменах",
                                "Ваша смена окончилась!",
                                1004
                            )
                        }
                    }
                    AlarmScheduler.ACTION_SHIFT_REMINDER_20 -> {
                        val tomorrow = today.plusDays(1)
                        if (isWorkDay(tomorrow, shiftStartDate, extraShifts)) {
                            sendNotification(
                                context,
                                ShiftReminderWorker.CHANNEL_ID,
                                "Напоминания о сменах",
                                "Завтра в смену!",
                                1001
                            )
                        }
                    }
                    AlarmScheduler.ACTION_PAYDAY_9 -> {
                        val dayOfMonth = today.dayOfMonth
                        if (dayOfMonth == 10 || dayOfMonth == 25) {
                            sendNotification(
                                context,
                                PaydayWorker.CHANNEL_ID,
                                "Выплаты и зарплата",
                                "Зачисление зарплаты/аванса",
                                1002
                            )
                        }
                    }
                }
            } catch (_: Exception) {
                // Handle error
            } finally {
                // Reschedule alarm for the next day
                AlarmScheduler.scheduleAll(context)
                pendingResult.finish()
            }
        }
    }

    private fun isWorkDay(date: LocalDate, shiftStartDate: LocalDate?, extraShifts: Set<LocalDate>): Boolean {
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

    private fun sendNotification(
        context: Context,
        channelId: String,
        channelName: String,
        text: String,
        notificationId: Int
    ) {
        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val importance = NotificationManager.IMPORTANCE_DEFAULT
                val channel = NotificationChannel(channelId, channelName, importance).apply {
                    description = channelName
                }
                notificationManager.createNotificationChannel(channel)
            }

            val builder = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.ic_menu_today)
                .setContentTitle(text)
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
}
