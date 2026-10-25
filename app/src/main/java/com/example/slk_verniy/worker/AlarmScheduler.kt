package com.example.slk_verniy.worker

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.slk_verniy.data.PreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.util.Calendar

object AlarmScheduler {

    const val ACTION_SHIFT_START_11 = "com.example.slk_verniy.ACTION_SHIFT_START_11"
    const val ACTION_SHIFT_END_23 = "com.example.slk_verniy.ACTION_SHIFT_END_23"
    const val ACTION_SHIFT_REMINDER_20 = "com.example.slk_verniy.ACTION_SHIFT_REMINDER_20"
    const val ACTION_PAYDAY_9 = "com.example.slk_verniy.ACTION_PAYDAY_9"

    private const val REQUEST_CODE_SHIFT_START = 1001
    private const val REQUEST_CODE_SHIFT_END = 1002
    private const val REQUEST_CODE_SHIFT_REMINDER = 1003
    private const val REQUEST_CODE_PAYDAY = 1004

    fun scheduleAll(context: Context) {
        val values = try {
            runBlocking(Dispatchers.IO) {
                val repo = PreferencesRepository(context)
                val sh = repo.shiftStartHour.first()
                val sm = repo.shiftStartMinute.first()
                val eh = repo.shiftEndHour.first()
                val em = repo.shiftEndMinute.first()
                val rh = repo.notificationHour.first()
                val rm = repo.notificationMinute.first()
                intArrayOf(sh, sm, eh, em, rh, rm)
            }
        } catch (_: Exception) {
            intArrayOf(11, 0, 23, 0, 20, 0)
        }

        scheduleAlarm(context, values[0], values[1], ACTION_SHIFT_START_11, REQUEST_CODE_SHIFT_START)
        scheduleAlarm(context, values[2], values[3], ACTION_SHIFT_END_23, REQUEST_CODE_SHIFT_END)
        scheduleAlarm(context, values[4], values[5], ACTION_SHIFT_REMINDER_20, REQUEST_CODE_SHIFT_REMINDER)
        scheduleAlarm(context, 9, 0, ACTION_PAYDAY_9, REQUEST_CODE_PAYDAY)
    }

    private fun scheduleAlarm(context: Context, hour: Int, minute: Int, action: String, requestCode: Int) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

            val intent = Intent(context, AlarmReceiver::class.java).apply {
                this.action = action
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            val now = Calendar.getInstance()
            if (calendar.before(now) || calendar == now) {
                calendar.add(Calendar.DAY_OF_MONTH, 1)
            }

            val triggerTimeMillis = calendar.timeInMillis

            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTimeMillis,
                pendingIntent
            )
        } catch (_: Exception) {
            // Handle security or scheduling exceptions
        }
    }
}
