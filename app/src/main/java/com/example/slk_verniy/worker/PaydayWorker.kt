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
import java.time.LocalDate
import java.util.concurrent.TimeUnit

class PaydayWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val WORK_NAME = "PaydayPeriodicWork"
        const val CHANNEL_ID = "payday_reminder_channel"
        private const val NOTIFICATION_ID = 1002

        fun sendTestNotification(context: Context, isSalary: Boolean) {
            val title = if (isSalary) "Зачисление сегодня Зарплаты" else "Зачисление Аванса сегодня"
            sendNotificationInternal(context, title, title)
        }

        private fun sendNotificationInternal(context: Context, title: String, text: String) {
            try {
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                    ?: return

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val name = "Выплаты и зарплата"
                    val descriptionText = "Уведомления о зарплате и авансе"
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
                    NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build())
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

                val currentDate = java.util.Calendar.getInstance()
                val dueDate = java.util.Calendar.getInstance().apply {
                    set(java.util.Calendar.HOUR_OF_DAY, 9)
                    set(java.util.Calendar.MINUTE, 0)
                    set(java.util.Calendar.SECOND, 0)
                }
                if (currentDate.after(dueDate)) {
                    dueDate.add(java.util.Calendar.DAY_OF_MONTH, 1)
                }
                val initialDelayMillis = maxOf(0L, dueDate.timeInMillis - currentDate.timeInMillis)

                val workRequest = PeriodicWorkRequestBuilder<PaydayWorker>(24, TimeUnit.HOURS)
                    .setInitialDelay(initialDelayMillis, TimeUnit.MILLISECONDS)
                    .setConstraints(constraints)
                    .build()

                WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.UPDATE,
                    workRequest
                )
            } catch (_: Exception) {
                // Handle scheduling exception
            }
        }
    }

    override suspend fun doWork(): Result {
        return try {
            val today = LocalDate.now()
            val dayOfMonth = today.dayOfMonth

            if (dayOfMonth == 10) {
                sendNotificationInternal(
                    applicationContext,
                    "Зачисление сегодня Зарплаты",
                    "Зачисление сегодня Зарплаты"
                )
            } else if (dayOfMonth == 25) {
                sendNotificationInternal(
                    applicationContext,
                    "Зачисление Аванса сегодня",
                    "Зачисление Аванса сегодня"
                )
            }

            Result.success()
        } catch (_: Exception) {
            Result.failure()
        }
    }
}
