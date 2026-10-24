package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.example.data.local.AppDatabase
import com.example.data.repository.ArchiveRepository
import com.example.service.ReminderBroadcastReceiver

class ArchiveApplication : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var repository: ArchiveRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        database = AppDatabase.getDatabase(this)
        repository = ArchiveRepository(this, database)

        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                ReminderBroadcastReceiver.CHANNEL_ID,
                "تذكيرات المراسلات الإدارية",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "إشعارات مواعيد الرد والمتابعة الدورية للمراسلات الإدارية"
                enableVibration(true)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        lateinit var instance: ArchiveApplication
            private set
    }
}
