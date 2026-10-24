package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.local.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getLongExtra(EXTRA_REMINDER_ID, -1L)
        val correspondenceId = intent.getLongExtra(EXTRA_CORRESPONDENCE_ID, -1L)
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "تذكير إداري بالمراسلة"
        val archiveNumber = intent.getStringExtra(EXTRA_ARCHIVE_NUMBER) ?: ""
        val subject = intent.getStringExtra(EXTRA_SUBJECT) ?: ""

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel(notificationManager)

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_OPEN_CORRESPONDENCE_ID", correspondenceId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            reminderId.toInt(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val contentText = if (archiveNumber.isNotBlank()) {
            "المراسلة رقم: $archiveNumber | $subject"
        } else {
            subject.ifBlank { "حان موعد متابعة مراسلة إدارية هامة" }
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("الأرشيف الإداري: $title")
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(reminderId.toInt(), notification)
    }

    private fun createNotificationChannel(manager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "تذكيرات المراسلات الإدارية",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "إشعارات مواعيد الرد والمتابعة الدورية للمراسلات الإدارية"
                enableVibration(true)
            }
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "administrative_reminders_channel"
        const val EXTRA_REMINDER_ID = "extra_reminder_id"
        const val EXTRA_CORRESPONDENCE_ID = "extra_correspondence_id"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_ARCHIVE_NUMBER = "extra_archive_number"
        const val EXTRA_SUBJECT = "extra_subject"
    }
}
