package ru.ilyaushenin.nnracing

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import com.google.firebase.messaging.FirebaseMessaging
import ru.ilyaushenin.nnracing.ui.PushBridge
import ru.ilyaushenin.nnracing.ui.PushNav

class MainActivity : ComponentActivity() {
    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { fetchToken() }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        PushBridge.requester = ::enablePush
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

    private fun fetchToken() {
        FirebaseMessaging.getInstance().token.addOnSuccessListener { PushBridge.deliver(it) }
    }
}
