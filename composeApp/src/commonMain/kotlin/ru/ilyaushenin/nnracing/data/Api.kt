package ru.ilyaushenin.nnracing.data

import com.russhwolf.settings.Settings
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.timeout
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

const val BASE_URL = "https://racing.ilyaushenin.ru"

class ApiException(val status: Int, override val message: String) : Exception(message)

val AppJson = Json { ignoreUnknownKeys = true; explicitNulls = false; coerceInputValues = true }

/** Клиент сервера racing.ilyaushenin.ru: тот же API, что у сайта. Сессия хранится в настройках устройства. */
class Api(private val settings: Settings) {
    val client = HttpClient {
        install(ContentNegotiation) { json(AppJson) }
        install(HttpTimeout) { requestTimeoutMillis = 30_000; connectTimeoutMillis = 15_000; socketTimeoutMillis = 30_000 }
    }

    private var token: String? = settings.getStringOrNull(KEY_TOKEN)
    val isSignedIn get() = token != null

    /** Запросы на чтение при сбое сети или перезапуске сервера (502–504) повторяются до двух раз: «вечной загрузки» не будет. */
    suspend fun call(
        method: HttpMethod,
        path: String,
        body: JsonElement? = null,
        query: Map<String, Any?> = emptyMap(),
        raw: ByteArray? = null,
        rawType: ContentType? = null,
        timeoutMs: Long? = null,
    ): HttpResponse {
        var attempt = 0
        while (true) {
            try {
                return callOnce(method, path, body, query, raw, rawType, timeoutMs)
            } catch (e: ApiException) {
                val retriable = method == HttpMethod.Get && timeoutMs == null && attempt < 2 && (e.status == 0 || e.status in 502..504)
                if (!retriable) throw e
                attempt++
                kotlinx.coroutines.delay(800L * attempt)
            }
        }
    }

    private suspend fun callOnce(
        method: HttpMethod,
        path: String,
        body: JsonElement? = null,
        query: Map<String, Any?> = emptyMap(),
        raw: ByteArray? = null,
        rawType: ContentType? = null,
        timeoutMs: Long? = null,
    ): HttpResponse {
        val response = try {
            client.request(BASE_URL + "/api" + path) {
                this.method = method
                header("X-Requested-With", "racing")
                if (timeoutMs != null) timeout { requestTimeoutMillis = timeoutMs; socketTimeoutMillis = timeoutMs }
                token?.let { header("Cookie", "racing_session=$it") }
                query.forEach { (k, v) -> if (v != null) parameter(k, v) }
                if (body != null) {
                    contentType(ContentType.Application.Json)
                    setBody(AppJson.encodeToString(JsonElement.serializer(), body))
                } else if (raw != null) {
                    contentType(rawType ?: ContentType.Application.OctetStream)
                    setBody(raw)
                }
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            throw ApiException(0, "Нет соединения. Проверьте интернет")
        }
        if (!response.status.isSuccess()) {
            val text = runCatching { response.bodyAsText() }.getOrDefault("")
            val message = runCatching { AppJson.parseToJsonElement(text).jsonObject["error"]?.jsonPrimitive?.contentOrNull }.getOrNull()
            if (response.status.value == 401 && token != null && !path.startsWith("/auth/")) setToken(null)
            throw ApiException(response.status.value, message ?: "Что-то пошло не так")
        }
        return response
    }

    suspend inline fun <reified T> get(path: String, query: Map<String, Any?> = emptyMap()): T = call(HttpMethod.Get, path, query = query).body()

    suspend inline fun <reified T> send(method: HttpMethod, path: String, body: JsonElement? = null): T = call(method, path, body).body()

    private fun setToken(value: String?) {
        token = value
        if (value == null) settings.remove(KEY_TOKEN) else settings.putString(KEY_TOKEN, value)
    }

    private fun captureToken(response: HttpResponse) {
        val cookie = response.headers.getAll("Set-Cookie").orEmpty().firstOrNull { it.startsWith("racing_session=") } ?: return
        val value = cookie.substringAfter("racing_session=").substringBefore(';')
        setToken(value.ifEmpty { null })
    }

    // ---- вход ----
    suspend fun config(): Config = get("/config")

    suspend fun me(): User = get<UserEnvelope>("/me").user

    suspend fun login(username: String, password: String): User {
        val r = call(HttpMethod.Post, "/auth/login", obj("username" to username, "password" to password))
        captureToken(r)
        return r.body<UserEnvelope>().user
    }

    suspend fun register(username: String, password: String, name: String, invite: String?): User {
        val r = call(HttpMethod.Post, "/auth/register", obj("username" to username, "password" to password, "name" to name, "invite" to invite))
        captureToken(r)
        return r.body<UserEnvelope>().user
    }

    suspend fun logout() {
        runCatching { call(HttpMethod.Post, "/auth/logout") }
        setToken(null)
    }

    suspend fun updateProfile(name: String, isPublic: Boolean): User =
        send<UserEnvelope>(HttpMethod.Patch, "/me", obj("name" to name, "isPublic" to isPublic)).user

    suspend fun setAvatar(jpeg: ByteArray): User =
        call(HttpMethod.Put, "/me/avatar", raw = jpeg, rawType = ContentType.Image.JPEG).body<UserEnvelope>().user

    suspend fun deleteAvatar(): User = send<UserEnvelope>(HttpMethod.Delete, "/me/avatar").user

    // ---- трассы и заезды ----
    private var tracksCache: List<Track>? = null

    /** Трассы почти не меняются: за запуск грузим один раз, копия лежит на устройстве на случай отсутствия сети. */
    suspend fun tracks(): List<Track> {
        tracksCache?.let { return it }
        return try {
            get<TracksEnvelope>("/tracks").tracks.also {
                tracksCache = it
                settings.putString(KEY_TRACKS, AppJson.encodeToString(TracksEnvelope.serializer(), TracksEnvelope(it)))
            }
        } catch (e: ApiException) {
            val saved = settings.getStringOrNull(KEY_TRACKS)?.let { runCatching { AppJson.decodeFromString(TracksEnvelope.serializer(), it).tracks }.getOrNull() }
            if (saved == null || e.status == 401) throw e
            saved.also { tracksCache = it }
        }
    }
    suspend fun runs(trackId: String? = null, userId: Int? = null): List<Run> = get<RunsEnvelope>("/runs", mapOf("track" to trackId, "user" to userId)).runs
    suspend fun run(id: Int): Run = get<RunEnvelope>("/runs/$id").run
    suspend fun createRun(body: JsonObject): Run = send<RunEnvelope>(HttpMethod.Post, "/runs", body).run
    suspend fun deleteRun(id: Int) { call(HttpMethod.Delete, "/runs/$id") }
    suspend fun stats(): Stats = get("/stats")

    // ---- друзья ----
    suspend fun searchUsers(q: String): List<UserBrief> = get<SearchEnvelope>("/users/search", mapOf("q" to q)).users
    suspend fun profile(id: Int): ProfileResponse = get("/users/$id")
    suspend fun friends(): FriendsResponse = get("/friends")
    suspend fun friendRequest(userId: Int): String = send<StatusEnvelope>(HttpMethod.Post, "/friends/request", obj("userId" to userId)).status
    suspend fun friendAccept(id: Int) { call(HttpMethod.Post, "/friends/$id/accept") }
    suspend fun friendRemove(id: Int) { call(HttpMethod.Delete, "/friends/$id") }

    // ---- запланированные заезды ----
    suspend fun planned(): List<Planned> = get<PlannedListEnvelope>("/planned").planned
    suspend fun chatPlanned(chatId: Int): List<Planned> = get<PlannedListEnvelope>("/chats/$chatId/planned").planned
    suspend fun createPlanned(body: JsonObject) { call(HttpMethod.Post, "/planned", body) }
    suspend fun cancelPlanned(id: Int) { call(HttpMethod.Delete, "/planned/$id") }
    suspend fun respondPlanned(id: Int, accept: Boolean) { call(HttpMethod.Post, "/planned/$id/respond", obj("accept" to accept)) }

    // ---- push-уведомления ----
    suspend fun registerPush(token: String) {
        settings.putString(KEY_PUSH, token)
        call(HttpMethod.Post, "/push/register", obj("token" to token))
    }

    /** При выходе: сервер перестаёт слать уведомления на это устройство. */
    suspend fun unregisterPush() {
        val token = settings.getStringOrNull(KEY_PUSH) ?: return
        call(HttpMethod.Post, "/push/unregister", obj("token" to token))
    }

    // ---- чат ----
    suspend fun chats(): ChatsResponse = get("/chats")
    suspend fun unread(): Int = get<UnreadEnvelope>("/chats/unread").unread
    suspend fun createChat(userIds: List<Int>, title: String?): Chat =
        send<ChatEnvelope>(HttpMethod.Post, "/chats", buildJsonObject {
            put("userIds", JsonArray(userIds.map { JsonPrimitive(it) }))
            if (title != null) put("title", JsonPrimitive(title))
        }).chat
    suspend fun messages(chatId: Int, after: Int? = null, before: Int? = null): MessagesResponse =
        get("/chats/$chatId/messages", mapOf("after" to after, "before" to before))

    /** «Длинный опрос»: сервер держит запрос до 20 секунд и отвечает сразу, как только пришло сообщение. */
    suspend fun waitMessages(chatId: Int, after: Int, read: Int): MessagesResponse =
        call(HttpMethod.Get, "/chats/$chatId/messages", query = mapOf("after" to after, "wait" to 20, "read" to read), timeoutMs = 25_000).body()
    suspend fun send(chatId: Int, body: String): Message = send<MessageEnvelope>(HttpMethod.Post, "/chats/$chatId/messages", obj("body" to body)).message
    suspend fun addChatMembers(chatId: Int, userIds: List<Int>): Chat =
        send<ChatEnvelope>(HttpMethod.Post, "/chats/$chatId/members", buildJsonObject { put("userIds", JsonArray(userIds.map { JsonPrimitive(it) })) }).chat
    suspend fun leaveChat(chatId: Int) { call(HttpMethod.Post, "/chats/$chatId/leave") }

    companion object {
        private const val KEY_TOKEN = "session"
        private const val KEY_TRACKS = "tracks"
        private const val KEY_PUSH = "push_token"
    }
}

/** Небольшой помощник для тел запросов: null-значения не отправляются. */
fun obj(vararg pairs: Pair<String, Any?>): JsonObject = buildJsonObject {
    for ((k, v) in pairs) when (v) {
        null -> Unit
        is String -> put(k, JsonPrimitive(v))
        is Boolean -> put(k, JsonPrimitive(v))
        is Number -> put(k, JsonPrimitive(v))
        is JsonElement -> put(k, v)
        else -> put(k, JsonNull)
    }
}
