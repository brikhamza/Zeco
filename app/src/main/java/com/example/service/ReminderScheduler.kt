package com.example.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.data.model.Reminder

object ReminderScheduler {

    fun scheduleReminder(
        context: Context,
        reminder: Reminder,
        archiveNumber: String,
        subject: String
    ): Boolean {
        return try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
                putExtra(ReminderBroadcastReceiver.EXTRA_REMINDER_ID, reminder.id)
                putExtra(ReminderBroadcastReceiver.EXTRA_CORRESPONDENCE_ID, reminder.correspondenceId)
                putExtra(ReminderBroadcastReceiver.EXTRA_TITLE, reminder.title)
                putExtra(ReminderBroadcastReceiver.EXTRA_ARCHIVE_NUMBER, archiveNumber)
                putExtra(ReminderBroadcastReceiver.EXTRA_SUBJECT, subject)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                reminder.id.toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Adjust trigger time if advance notice days specified
            val targetTime = if (reminder.advanceNoticeDays > 0) {
                reminder.reminderTime - (reminder.advanceNoticeDays * 24 * 60 * 60 * 1000L)
            } else {
                reminder.reminderTime
            }

            // Only schedule if in future
            if (targetTime > System.currentTimeMillis()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, targetTime, pendingIntent)
                    } else {
                        alarmManager.set(AlarmManager.RTC_WAKEUP, targetTime, pendingIntent)
                    }
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, targetTime, pendingIntent)
                } else {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, targetTime, pendingIntent)
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun cancelReminder(context: Context, reminderId: Long) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, ReminderBroadcastReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                reminderId.toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
