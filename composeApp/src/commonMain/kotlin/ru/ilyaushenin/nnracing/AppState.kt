package ru.ilyaushenin.nnracing

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.LaunchedEffect
import com.russhwolf.settings.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ru.ilyaushenin.nnracing.data.Api
import ru.ilyaushenin.nnracing.data.ApiException
import ru.ilyaushenin.nnracing.data.User

sealed interface Route {
    data object Home : Route
    data class Track(val id: String, val variant: String? = null) : Route
    data class Run(val id: Int) : Route
    data class Me(val tab: Int = 0) : Route
    data class UserProfile(val id: Int) : Route
    data object Chats : Route
    data class Chat(val id: Int) : Route
}

/** Состояние всего приложения: сессия, стек экранов, бейдж чата, всплывающее сообщение. */
@Stable
class AppState(val api: Api, val scope: CoroutineScope) {
    var user by mutableStateOf<User?>(null)
    var booting by mutableStateOf(true)
    var inviteRequired by mutableStateOf(false)
    var unread by mutableIntStateOf(0)
    var toast by mutableStateOf<String?>(null)
    /** Увеличивается, когда экранам нужно перезагрузить данные. */
    var refreshTick by mutableIntStateOf(0)
    val stack: SnapshotStateList<Route> = mutableStateListOf(Route.Home)
    /** Последние загруженные данные экранов: показываем сразу, а свежие подгружаем в фоне. Очищается при смене аккаунта. */
    val cache = HashMap<String, Any?>()

    val current: Route get() = stack.last()

    suspend fun boot() {
        if (api.isSignedIn) user = runCatching { api.me() }.getOrNull()
        if (user == null) inviteRequired = runCatching { api.config().inviteRequired }.getOrDefault(false)
        booting = false
    }

    fun signedIn(u: User) {
        cache.clear()
        user = u
        stack.clear(); stack.add(Route.Home)
    }

    fun signOut() {
        scope.launch { runCatching { api.unregisterPush() }; api.logout() }
        user = null; unread = 0; cache.clear()
        stack.clear(); stack.add(Route.Home)
    }

    fun push(route: Route) { stack.add(route) }

    fun pop(): Boolean {
        if (stack.size <= 1) return false
        stack.removeAt(stack.lastIndex)
        return true
    }

    /** Переход на корневой экран вкладки: стек сбрасывается. */
    fun goTab(route: Route) { stack.clear(); stack.add(route) }

    fun refresh() { refreshTick++ }

    fun showToast(message: String) {
        toast = message
        scope.launch { delay(2700); if (toast == message) toast = null }
    }

    /** Выполняет запрос и показывает ошибку тостом; 401 — выход. */
    fun act(onError: ((String) -> Unit)? = null, block: suspend () -> Unit) {
        scope.launch {
            try { block() } catch (e: ApiException) {
                if (e.status == 401) { user = null } else (onError ?: ::showToast)(e.message)
            }
        }
    }

    suspend fun pollUnread() {
        while (true) {
            if (user != null) runCatching { api.unread() }.onSuccess { unread = it }
            delay(15_000)
        }
    }
}

@Composable
fun rememberAppState(api: Api, scope: CoroutineScope): AppState {
    val state = remember { AppState(api, scope) }
    LaunchedEffect(Unit) { state.boot() }
    LaunchedEffect(Unit) { state.pollUnread() }
    return state
}

fun settings(): Settings = Settings()
