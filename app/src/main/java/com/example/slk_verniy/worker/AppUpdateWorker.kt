package com.example.slk_verniy.worker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
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
import java.util.concurrent.TimeUnit

class AppUpdateWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val WORK_NAME = "AppUpdatePeriodicWork"
        const val CHANNEL_ID = "app_update_channel"
        private const val NOTIFICATION_ID = 1003

        fun sendNotification(context: Context, releaseInfo: ReleaseInfo) {
            try {
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                    ?: return

                val name = "Обновления приложения"
                val descriptionText = "Уведомления о новых версиях приложения"
                val importance = NotificationManager.IMPORTANCE_DEFAULT
                val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                    description = descriptionText
                }
                notificationManager.createNotificationChannel(channel)

                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(releaseInfo.releaseUrl)).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                val pendingIntent = android.app.PendingIntent.getActivity(
                    context,
                    0,
                    intent,
                    android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
                )

                val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.ic_menu_upload)
                    .setContentTitle("Доступно обновление ${releaseInfo.versionName}!")
                    .setContentText("Нажмите, чтобы обновить")
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setContentIntent(pendingIntent)
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
                // Ignore notification errors
            }
        }

        fun schedule(context: Context) {
            try {
                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()

                val workRequest = PeriodicWorkRequestBuilder<AppUpdateWorker>(24, TimeUnit.HOURS)
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
            val currentVersion = try {
                applicationContext.packageManager.getPackageInfo(applicationContext.packageName, 0).versionName ?: "1.0.0"
            } catch (_: Exception) {
                "1.0.0"
            }
            val releaseInfo = UpdateManager.checkForUpdate(currentVersion)
            if (releaseInfo != null) {
                sendNotification(applicationContext, releaseInfo)
            }
            Result.success()
        } catch (_: Exception) {
            Result.failure()
        }
    }
}
