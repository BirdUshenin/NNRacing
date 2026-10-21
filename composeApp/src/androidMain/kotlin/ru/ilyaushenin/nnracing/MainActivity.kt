package ru.ilyaushenin.nnracing

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import com.google.firebase.messaging.FirebaseMessaging
import ru.ilyaushenin.nnracing.ui.PushBridge
import ru.ilyaushenin.nnracing.ui.PushNav

class MainActivity : ComponentActivity() {
    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) fetchToken() else PushBridge.denied()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        PushBridge.requester = ::enablePush
        PushBridge.permissionCheck = { NotificationManagerCompat.from(this).areNotificationsEnabled() }
        PushBridge.settingsOpener = ::openNotificationSettings
        readNotificationIntent(intent)
        setContent { App() }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        readNotificationIntent(intent)
    }

    /** Нажатие на уведомление: Firebase кладёт данные сообщения в extras запускаемой активности. */
    private fun readNotificationIntent(intent: Intent?) {
        intent?.extras?.getString("chatId")?.toIntOrNull()?.let { PushNav.chatId = it }
    }

    private fun enablePush() {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            fetchToken()
        }
    }

    /** Системные настройки уведомлений приложения: после запрета разрешение можно вернуть только здесь. */
    private fun openNotificationSettings() {
        startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    private fun fetchToken() {
        FirebaseMessaging.getInstance().token.addOnSuccessListener { PushBridge.deliver(it) }
    }
}
