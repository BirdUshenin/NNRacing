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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
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
        if (app.user != null && app.api.pushEnabled) ru.ilyaushenin.nnracing.ui.PushBridge.enable { token -> app.act { app.api.registerPush(token) } }
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
    // новая версия: спрашиваем сервер при запуске и каждый раз, когда приложение возвращается на передний план
    val focused = androidx.compose.ui.platform.LocalWindowInfo.current.isWindowFocused
    LaunchedEffect(focused) { if (focused) app.checkUpdate() }
    val sheets = remember { ru.ilyaushenin.nnracing.ui.SheetHost() }
    androidx.compose.runtime.CompositionLocalProvider(ru.ilyaushenin.nnracing.ui.LocalSheetHost provides sheets) {
    NNTheme {
        Box(Modifier.fillMaxSize().background(Rc.Bg)) {
            // всё приложение записывается в слой: поздравление с наградой рисует его размытую копию
            val appLayer = androidx.compose.ui.graphics.rememberGraphicsLayer()
            Box(Modifier.fillMaxSize().drawWithContent { val scope = this; appLayer.record { scope.drawContent() }; drawLayer(appLayer) }) {
                when {
                    app.booting -> SplashTrack()
                    app.user == null -> AuthScreen(app)
                    else -> MainShell(app)
                }
                ru.ilyaushenin.nnracing.ui.SheetLayer(sheets)
            }
            ru.ilyaushenin.nnracing.screens.CelebrationLayer(app, appLayer)
            ru.ilyaushenin.nnracing.screens.OnboardingLayer(app)
            ru.ilyaushenin.nnracing.screens.UpdatePrompt(app)
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
}

@Composable
private fun MainShell(app: AppState) {
    PlatformBackHandler(enabled = app.stack.size > 1) { app.pop() }
    // экран рисуется на всю высоту и записывается в графический слой: нижняя панель прозрачная, а под ней — размытая копия этого слоя (как backdrop-filter на сайте)
    val layer = androidx.compose.ui.graphics.rememberGraphicsLayer()
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier.fillMaxSize().drawWithContent {
                val scope = this
                layer.record { scope.drawContent() }
                drawLayer(layer)
            },
        ) { SwipeBackHost(app) { route -> RouteContent(app, route) } }
        if (app.current !is Route.Chat) BottomBar(app, layer, Modifier.align(Alignment.BottomCenter))
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
private fun BottomBar(app: AppState, backdrop: androidx.compose.ui.graphics.layer.GraphicsLayer, modifier: Modifier = Modifier) {
    val root = app.stack.first()
    val blurLayer = androidx.compose.ui.graphics.rememberGraphicsLayer()
    // на устройствах без аппаратного размытия панель просто почти непрозрачная
    val blurred = remember { runCatching { blurLayer.renderEffect = androidx.compose.ui.graphics.BlurEffect(60f, 60f, androidx.compose.ui.graphics.TileMode.Clamp); true }.getOrDefault(false) }
    Row(
        modifier.fillMaxWidth()
            .drawBehind {
                if (blurred && backdrop.size.height > 0) {
                    // панель стоит у нижнего края: её верхняя точка — высота экрана минус высота панели
                    val top = (backdrop.size.height - size.height).coerceAtLeast(0f)
                    // сначала сплошной фон: размытая копия местами прозрачная, и без него сквозь неё просвечивал бы чёткий оригинал (например, мелкий текст)
                    drawRect(Rc.Bg)
                    blurLayer.record(androidx.compose.ui.unit.IntSize(backdrop.size.width, backdrop.size.height)) { drawLayer(backdrop) }
                    clipRect { translate(0f, -top) { drawLayer(blurLayer) } }
                }
            }
            .background(Rc.Bg.copy(alpha = if (blurred) 0.35f else 0.96f)).drawBehind { drawLine(Rc.Line, androidx.compose.ui.geometry.Offset(0f, 0.5f), androidx.compose.ui.geometry.Offset(size.width, 0.5f), 1.dp.toPx()) }
            .navigationBarsPadding().padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Tab("Главная", null, root is Route.Home, 0, Modifier.weight(1f)) { app.goTab(Route.Home) }
        Tab("Чат", Ic.Chat, root is Route.Chats, app.unread, Modifier.weight(1f)) { app.goTab(Route.Chats) }
        Tab("Профиль", Ic.User, root is Route.Me, app.requests, Modifier.weight(1f)) { app.goTab(Route.Me()) }
    }
}

@Composable
private fun Tab(label: String, icon: String?, active: Boolean, badge: Int, modifier: Modifier, onClick: () -> Unit) {
    Column(modifier.clip(RoundedCornerShape(16.dp)).clickableNoRipple(onClick).padding(vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Box {
            // у «Главной» значок — контур нашей трассы
            if (icon == null) ru.ilyaushenin.nnracing.ui.TrackIcon(if (active) Rc.Accent else Rc.Muted, 24.dp)
            else Icon24(icon, if (active) Rc.Accent else Rc.Muted, 24.dp)
            if (badge > 0) Box(Modifier.align(Alignment.TopEnd).padding(start = 18.dp).clip(RoundedCornerShape(9.dp)).background(Rc.Accent).padding(horizontal = 5.dp)) {
                Txt(if (badge > 99) "99+" else badge.toString(), color = Color.White, size = 10.sp, weight = FontWeight.ExtraBold)
            }
        }
        Txt(label, color = if (active) Rc.Text else Rc.Muted, size = 11.sp, weight = FontWeight.SemiBold)
    }
}
