package ru.ilyaushenin.nnracing

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.network.ktor3.KtorNetworkFetcherFactory
import ru.ilyaushenin.nnracing.data.Api
import ru.ilyaushenin.nnracing.screens.AuthScreen
import ru.ilyaushenin.nnracing.screens.ChatScreen
import ru.ilyaushenin.nnracing.screens.ChatsScreen
import ru.ilyaushenin.nnracing.screens.HomeScreen
import ru.ilyaushenin.nnracing.screens.ProfileScreen
import ru.ilyaushenin.nnracing.screens.RunScreen
import ru.ilyaushenin.nnracing.screens.TrackScreen
import ru.ilyaushenin.nnracing.screens.UserScreen
import ru.ilyaushenin.nnracing.ui.SplashTrack
import ru.ilyaushenin.nnracing.ui.Ic
import ru.ilyaushenin.nnracing.ui.Icon24
import ru.ilyaushenin.nnracing.ui.NNTheme
import ru.ilyaushenin.nnracing.ui.PlatformBackHandler
import ru.ilyaushenin.nnracing.ui.Rc
import ru.ilyaushenin.nnracing.ui.Txt
import ru.ilyaushenin.nnracing.ui.clickableNoRipple
import ru.ilyaushenin.nnracing.ui.SwipeBackHost

@Composable
fun App() {
    setSingletonImageLoaderFactory { context -> ImageLoader.Builder(context).components { add(KtorNetworkFetcherFactory()) }.build() }
    val scope = rememberCoroutineScope()
    val app = rememberAppState(remember { Api(settings()) }, scope)
    // вошли в аккаунт: просим разрешение на уведомления и сообщаем серверу токен устройства
    LaunchedEffect(app.user?.id) {
        if (app.user != null) ru.ilyaushenin.nnracing.ui.PushBridge.enable { token -> app.act { app.api.registerPush(token) } }
    }
    // нажали на уведомление о сообщении — открываем этот чат
    LaunchedEffect(app.user?.id, ru.ilyaushenin.nnracing.ui.PushNav.chatId) {
        val chat = ru.ilyaushenin.nnracing.ui.PushNav.chatId
        if (chat != null && app.user != null) {
            ru.ilyaushenin.nnracing.ui.PushNav.chatId = null
            app.goTab(Route.Chats)
            app.push(Route.Chat(chat))
        }
    }
    NNTheme {
        Box(Modifier.fillMaxSize().background(Rc.Bg)) {
            when {
                app.booting -> SplashTrack()
                app.user == null -> AuthScreen(app)
                else -> MainShell(app)
            }
            app.toast?.let { msg ->
                Box(Modifier.fillMaxSize().navigationBarsPadding().padding(bottom = 96.dp), contentAlignment = Alignment.BottomCenter) {
                    Box(Modifier.padding(horizontal = 24.dp).clip(RoundedCornerShape(14.dp)).background(Rc.Surface3).border(1.dp, Rc.LineStrong, RoundedCornerShape(14.dp)).padding(horizontal = 18.dp, vertical = 12.dp)) {
                        Txt(msg, weight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun MainShell(app: AppState) {
    PlatformBackHandler(enabled = app.stack.size > 1) { app.pop() }
    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f).fillMaxWidth()) { SwipeBackHost(app) { route -> RouteContent(app, route) } }
        if (app.current !is Route.Chat) BottomBar(app)
    }
}

@Composable
private fun RouteContent(app: AppState, route: Route) {
    when (route) {
        Route.Home -> HomeScreen(app)
        is Route.Track -> TrackScreen(app, route)
        is Route.Run -> RunScreen(app, route)
        is Route.Me -> ProfileScreen(app, route)
        is Route.UserProfile -> UserScreen(app, route)
        Route.Chats -> ChatsScreen(app)
        is Route.Chat -> ChatScreen(app, route)
    }
}

@Composable
private fun BottomBar(app: AppState) {
    val root = app.stack.first()
    Row(
        Modifier.fillMaxWidth().background(Rc.Bg).border(1.dp, Rc.Line).navigationBarsPadding().padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Tab("Главная", Ic.Flag, root is Route.Home, 0, Modifier.weight(1f)) { app.goTab(Route.Home) }
        Tab("Чат", Ic.Chat, root is Route.Chats, app.unread, Modifier.weight(1f)) { app.goTab(Route.Chats) }
        Tab("Профиль", Ic.User, root is Route.Me, 0, Modifier.weight(1f)) { app.goTab(Route.Me()) }
    }
}

@Composable
private fun Tab(label: String, icon: String, active: Boolean, badge: Int, modifier: Modifier, onClick: () -> Unit) {
    Column(modifier.clip(RoundedCornerShape(16.dp)).clickableNoRipple(onClick).padding(vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Box {
            Icon24(icon, if (active) Rc.Accent else Rc.Muted, 24.dp)
            if (badge > 0) Box(Modifier.align(Alignment.TopEnd).padding(start = 18.dp).clip(RoundedCornerShape(9.dp)).background(Rc.Accent).padding(horizontal = 5.dp)) {
                Txt(if (badge > 99) "99+" else badge.toString(), color = Color.White, size = 10.sp, weight = FontWeight.ExtraBold)
            }
        }
        Txt(label, color = if (active) Rc.Text else Rc.Muted, size = 11.sp, weight = FontWeight.SemiBold)
    }
}
