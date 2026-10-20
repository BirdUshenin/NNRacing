package ru.ilyaushenin.nnracing.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import ru.ilyaushenin.nnracing.AppState
import ru.ilyaushenin.nnracing.data.ApiException

/** Результат загрузки экрана: данные, ошибка или «грузим». */
@Stable
class Loaded<T>(val value: T?, val error: String?, val reload: () -> Unit) {
    val loading get() = value == null && error == null
}

/**
 * Загружает данные при открытии экрана и при каждом [AppState.refresh].
 * Если для [cacheKey] уже есть данные, они показываются сразу, а свежие подгружаются в фоне без «мигания».
 * 401 разлогинивает; ошибка сети при наличии старых данных их не стирает.
 */
@Suppress("UNCHECKED_CAST")
@Composable
fun <T> load(app: AppState, vararg keys: Any?, cacheKey: String? = null, block: suspend () -> T): Loaded<T> {
    var value by remember(cacheKey, *keys) { mutableStateOf(cacheKey?.let { app.cache[it] } as T?) }
    var error by remember(cacheKey, *keys) { mutableStateOf<String?>(null) }
    var attempt by remember(cacheKey, *keys) { mutableStateOf(0) }
    LaunchedEffect(cacheKey, *keys, app.refreshTick, attempt) {
        error = null
        try {
            val fresh = block()
            value = fresh
            if (cacheKey != null) app.cache[cacheKey] = fresh
        } catch (e: ApiException) {
            if (e.status == 401) app.user = null else if (value == null) error = e.message
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (value == null) error = "Не удалось загрузить"
        }
    }
    return Loaded(value, error) { attempt++ }
}

/** Параллельные запросы: экран ждёт самый медленный, а не сумму всех. */
suspend fun <A, B> par(a: suspend () -> A, b: suspend () -> B): Pair<A, B> = coroutineScope {
    val x = async { a() }
    val y = async { b() }
    x.await() to y.await()
}

suspend fun <A, B, C> par(a: suspend () -> A, b: suspend () -> B, c: suspend () -> C): Triple<A, B, C> = coroutineScope {
    val x = async { a() }
    val y = async { b() }
    val z = async { c() }
    Triple(x.await(), y.await(), z.await())
}
