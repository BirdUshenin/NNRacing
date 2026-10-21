package ru.ilyaushenin.nnracing.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Мост к push-уведомлениям платформы: на Android — Firebase, на iOS пока заглушка. */
expect object PushBridge {
    /** Запрашивает разрешение на уведомления (Android 13+) и сообщает токен устройства. Если разрешение не дали, вызывает [onDenied]. */
    fun enable(onDenied: () -> Unit = {}, onToken: (String) -> Unit)

    /** Есть ли на платформе уведомления: на iPhone пока нет, и переключатель там не показывается. */
    val supported: Boolean

    /** Разрешены ли уведомления у приложения в системе прямо сейчас. */
    fun permissionGranted(): Boolean

    /** Открывает системные настройки уведомлений приложения: после запрета разрешение можно вернуть только там. */
    fun openSettings()
}

/** Чат, который нужно открыть после нажатия на уведомление. */
object PushNav {
    var chatId by mutableStateOf<Int?>(null)
}
