package ru.ilyaushenin.nnracing.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Мост к push-уведомлениям платформы: на Android — Firebase, на iOS пока заглушка. */
expect object PushBridge {
    /** Запрашивает разрешение на уведомления (Android 13+) и сообщает токен устройства. */
    fun enable(onToken: (String) -> Unit)
}

/** Чат, который нужно открыть после нажатия на уведомление. */
object PushNav {
    var chatId by mutableStateOf<Int?>(null)
}
