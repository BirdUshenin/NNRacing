package ru.ilyaushenin.nnracing

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class NNApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // канал для уведомлений создаём заранее: сообщение может прийти, когда приложение не запущено
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel("messages", "Сообщения и заезды", NotificationManager.IMPORTANCE_HIGH)
            channel.description = "Новые сообщения, приглашения на заезды и заявки в друзья"
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }
}
