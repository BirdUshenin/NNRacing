package ru.ilyaushenin.nnracing.data

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: Int,
    val username: String,
    val name: String,
    val avatarUrl: String? = null,
    val isPublic: Boolean = false,
    val neon: List<String> = listOf("red"),   // цвета огней свечения слева направо
    val neonSlots: Int = 1,                   // сколько огней можно настроить сейчас
    val neonUnlock: List<Int> = listOf(0, 1, 5, 10, 50, 100),   // сколько заездов нужно для каждого огня
    val badge: String? = null,                // значок-статус рядом с ником
)

@Serializable
data class UserBrief(
    val id: Int,
    val username: String,
    val name: String,
    val avatarUrl: String? = null,
    val friendStatus: String? = null,
    val isPublic: Boolean? = null,
    val lastSeen: Long? = null,
    val neon: List<String> = listOf("red"),
    val badge: String? = null,
)

@Serializable data class UserEnvelope(val user: User)

@Serializable data class Config(val inviteRequired: Boolean = false, val update: Map<String, UpdateInfo> = emptyMap())

/** Флаг «вышла новая версия»: включается на сервере; required — окно без кнопки «Позже». */
@Serializable data class UpdateInfo(val version: Int = 0, val required: Boolean = false, val message: String = "", val url: String = "/download/nn-racing.apk")

@Serializable data class StartMark(val x: Double, val y: Double, val angle: Double, val pathAgainst: Boolean = false)
@Serializable data class Corner(val n: Int, val x: Double, val y: Double)
@Serializable data class Variant(val id: String, val name: String, val reversed: Boolean = false)

@Serializable
data class Track(
    val id: String,
    val name: String,
    val viewBox: List<Double>,
    val path: String,
    val start: StartMark? = null,
    val corners: List<Corner> = emptyList(),
    val variants: List<Variant> = emptyList(),
)

@Serializable data class TracksEnvelope(val tracks: List<Track>)

@Serializable
data class Weather(val condition: String? = null, val airC: Double? = null, val trackC: Double? = null)

@Serializable
data class RunResult(
    val userId: Int? = null,
    val user: UserBrief? = null,
    val name: String = "",
    val kart: String? = null,
    val position: Int? = null,
    val laps: List<Int> = emptyList(),
    val bestIndex: Int = 0,
    val bestMs: Int = 0,
    val avgMs: Int = 0,
    val lapCount: Int = 0,
)

@Serializable
data class Run(
    val id: Int,
    val trackId: String,
    val variant: String? = null,
    val date: String,
    val time: String? = null,
    val notes: String = "",
    val weather: Weather? = null,
    val creatorId: Int = 0,
    val results: List<RunResult> = emptyList(),
)

@Serializable data class RunsEnvelope(val runs: List<Run>)
@Serializable data class RunEnvelope(val run: Run)
@Serializable data class RunCreated(val run: Run, val newAchievements: List<Achievement> = emptyList())

@Serializable
data class TrackStats(
    val sessions: Int = 0,
    val laps: Int = 0,
    val personalBestMs: Int = 0,
    val personalBestDate: String = "",
    val avgLapMs: Int = 0,
    val consistencyCvPct: Double? = null,
    val history: List<HistoryPoint> = emptyList(),
    val trendMsPerSession: Double? = null,
    val improvementMs: Double? = null,
    val improvementPct: Double? = null,
    val learningRate: Double? = null,
    val forecastNextMs: Double? = null,
)

@Serializable data class HistoryPoint(val date: String = "", val bestMs: Int = 0, val avgMs: Int = 0)

@Serializable
data class OverallStats(
    val tracks: Int = 0,
    val sessions: Int = 0,
    val laps: Int = 0,
    val avgImprovementPct: Double? = null,
)

@Serializable
data class Stats(val overall: OverallStats = OverallStats(), val tracks: Map<String, Map<String, TrackStats?>> = emptyMap())

@Serializable
data class PlannedMember(
    val id: Int,
    val username: String,
    val name: String,
    val avatarUrl: String? = null,
    val status: String = "going",
)

@Serializable
data class Planned(
    val id: Int,
    val trackId: String,
    val variant: String? = null,
    val startsAt: Long,
    val notes: String = "",
    val chatId: Int? = null,
    val creatorId: Int = 0,
    val isCreator: Boolean = false,
    val members: List<PlannedMember> = emptyList(),
    val myStatus: String? = null,
)

@Serializable data class PlannedListEnvelope(val planned: List<Planned>)

@Serializable
data class FriendsResponse(
    val friends: List<UserBrief> = emptyList(),
    val incoming: List<UserBrief> = emptyList(),
    val outgoing: List<UserBrief> = emptyList(),
)

@Serializable data class SearchEnvelope(val users: List<UserBrief>)
@Serializable data class StatusEnvelope(val status: String = "")

@Serializable
data class ProfileResponse(
    val user: UserBrief,
    val friendStatus: String = "none",
    val visible: Boolean = false,
    val runs: List<Run> = emptyList(),
    val achievements: List<Achievement> = emptyList(),
)

/** Награда: кубок за место на трассе или за число заездов; выдаётся сервером автоматически. */
@Serializable
data class Achievement(
    val id: String,
    val kind: String = "",
    val earned: Boolean = false,
    val earnedAt: String? = null,
    val count: Int = 0,
    val need: Int = 0,
    val have: Int = 0,
    val title: String = "",
    val description: String = "",
    val image: String = "",
    val place: Int = 0,
)
@Serializable data class AchievementsResponse(val achievements: List<Achievement> = emptyList())

@Serializable
data class ChatLast(val body: String = "", val kind: String = "text", val userId: Int = 0, val createdAt: Long = 0)

@Serializable
data class Chat(
    val id: Int,
    val kind: String,
    val title: String? = null,
    val avatarUrl: String? = null,
    val members: List<UserBrief> = emptyList(),
    val unread: Int = 0,
    val last: ChatLast? = null,
)

@Serializable data class ChatsResponse(val chats: List<Chat> = emptyList(), val unread: Int = 0)
@Serializable data class ChatEnvelope(val chat: Chat)
@Serializable data class UnreadEnvelope(val unread: Int = 0, val requests: Int = 0)

@Serializable
data class Message(
    val id: Int = 0,
    val userId: Int = 0,
    val body: String = "",
    val createdAt: Long = 0,
    val replyTo: ReplyRef? = null,
    val edited: Boolean = false,
    val reactions: Map<String, List<Int>> = emptyMap(),
    val deleted: Boolean = false,   // только в событиях: сообщение удалено
    val image: String? = null,       // фото: путь на сервере, имя файла случайное
    val imageGone: Boolean = false,  // фото удалено по сроку хранения
    val imageW: Int? = null,
    val imageH: Int? = null,
    val poll: Poll? = null,
    val sticker: String? = null,    // имя анимированного стикера
)

@Serializable
data class PollOption(val id: Int = 0, val text: String = "", val voters: List<Int> = emptyList())

@Serializable
data class Poll(val question: String = "", val multiple: Boolean = false, val options: List<PollOption> = emptyList())

@Serializable
data class ReplyRef(val id: Int = 0, val userId: Int = 0, val body: String = "", val deleted: Boolean = false)

@Serializable
data class MessagesResponse(
    val messages: List<Message> = emptyList(),
    val events: List<Message> = emptyList(),
    val ev: Int = 0,
    val hasMore: Boolean = false,
    val readUpTo: Int = 0,
)
@Serializable data class SeenResponse(val users: List<UserBrief> = emptyList())
@Serializable data class MessageEnvelope(val message: Message)
