package ru.ilyaushenin.nnracing

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.russhwolf.settings.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ru.ilyaushenin.nnracing.data.Api

/** Приём push от Firebase. В фоне уведомление показывает система; здесь только обновление токена и приём в открытом приложении. */
class NNMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        // токен устройства поменялся (переустановка, очистка данных): отправляем новый на сервер, если мы вошли в аккаунт
        val api = Api(Settings())
        if (api.isSignedIn) CoroutineScope(Dispatchers.IO).launch { runCatching { api.registerPush(token) } }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        // приложение открыто: в нём свои индикаторы (бейдж чата, список), системное уведомление не нужно
    }
}
