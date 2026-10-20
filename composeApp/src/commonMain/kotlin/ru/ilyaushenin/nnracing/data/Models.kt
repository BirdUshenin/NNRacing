package ru.ilyaushenin.nnracing.data

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: Int,
    val username: String,
    val name: String,
    val avatarUrl: String? = null,
    val isPublic: Boolean = false,
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
)

@Serializable data class UserEnvelope(val user: User)

@Serializable data class Config(val inviteRequired: Boolean = false)

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
)

@Serializable
data class ChatLast(val body: String = "", val userId: Int = 0, val createdAt: Long = 0)

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
@Serializable data class UnreadEnvelope(val unread: Int = 0)

@Serializable
data class Message(val id: Int, val userId: Int, val body: String, val createdAt: Long)

@Serializable data class MessagesResponse(val messages: List<Message> = emptyList(), val hasMore: Boolean = false, val readUpTo: Int = 0)
@Serializable data class MessageEnvelope(val message: Message)
