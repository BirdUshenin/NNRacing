package ru.ilyaushenin.nnracing.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import ru.ilyaushenin.nnracing.AppState
import ru.ilyaushenin.nnracing.Route
import ru.ilyaushenin.nnracing.data.ApiException
import ru.ilyaushenin.nnracing.data.FriendsResponse
import ru.ilyaushenin.nnracing.data.ProfileResponse
import ru.ilyaushenin.nnracing.data.Track
import ru.ilyaushenin.nnracing.data.UserBrief
import ru.ilyaushenin.nnracing.ui.AppButton
import ru.ilyaushenin.nnracing.ui.AppField
import ru.ilyaushenin.nnracing.ui.Avatar
import ru.ilyaushenin.nnracing.ui.BackLink
import ru.ilyaushenin.nnracing.ui.BtnKind
import ru.ilyaushenin.nnracing.ui.Chip
import ru.ilyaushenin.nnracing.ui.Empty
import ru.ilyaushenin.nnracing.ui.Ic
import ru.ilyaushenin.nnracing.ui.neonGlow
import ru.ilyaushenin.nnracing.ui.Icon24
import ru.ilyaushenin.nnracing.ui.IconBtn
import ru.ilyaushenin.nnracing.ui.LoadedContent
import ru.ilyaushenin.nnracing.ui.Page
import ru.ilyaushenin.nnracing.ui.Rc
import ru.ilyaushenin.nnracing.ui.SectionTitle
import ru.ilyaushenin.nnracing.ui.Txt
import ru.ilyaushenin.nnracing.ui.fmtSeen
import ru.ilyaushenin.nnracing.ui.isOnline
import ru.ilyaushenin.nnracing.ui.load
import ru.ilyaushenin.nnracing.ui.par
import ru.ilyaushenin.nnracing.ui.plural

@Composable
private fun PersonRow(app: AppState, u: UserBrief, actions: @Composable () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Rc.Surface2).border(1.dp, Rc.Line, RoundedCornerShape(18.dp)).padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(Modifier.weight(1f).clickable { app.push(Route.UserProfile(u.id)) }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Avatar(u.name, u.avatarUrl, 44.dp)
            Column {
                ru.ilyaushenin.nnracing.ui.NameWithBadge(u.name, u.badge, weight = FontWeight.ExtraBold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Txt("@${u.username}", color = Rc.Muted, size = 13.sp, maxLines = 1)
                    if (u.friendStatus != null && u.isPublic == false) Icon24(Ic.Lock, Rc.Muted, 13.dp, modifier = Modifier.padding(start = 6.dp))
                }
                SeenLine(u.lastSeen)
            }
        }
        actions()
    }
}

@Composable
fun FriendsTab(app: AppState) {
    val data = load(app, cacheKey = "friends") { app.api.friends() }
    var query by remember { mutableStateOf("") }
    var found by remember { mutableStateOf<List<UserBrief>?>(null) }
    var searchError by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) { while (true) { delay(30_000); app.refresh() } }   // «в сети» обновляется без ручного обновления экрана

    LaunchedEffect(query, app.refreshTick) {
        val q = query.trim()
        if (q.length < 2) { found = null; searchError = null; return@LaunchedEffect }
        delay(250)
        try { found = app.api.searchUsers(q); searchError = null } catch (e: ApiException) { searchError = e.message }
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        AppField(query, { query = it }, placeholder = "Найти по нику")
        searchError?.let { Txt(it, color = Rc.Bad, size = 14.sp) }
        found?.let { list ->
            if (list.isEmpty()) Txt("Никого не нашли. Проверьте ник.", color = Rc.Muted, size = 13.sp)
            list.forEach { u ->
                PersonRow(app, u) {
                    val label = mapOf("friends" to "Друзья", "outgoing" to "Отправлено", "incoming" to "Принять")[u.friendStatus] ?: "Добавить"
                    AppButton(label, { app.act { app.api.friendRequest(u.id); app.showToast("Готово"); app.refresh() } }, small = true, enabled = u.friendStatus != "friends" && u.friendStatus != "outgoing", kind = if (u.friendStatus == "none" || u.friendStatus == "incoming") BtnKind.Primary else BtnKind.Ghost)
                }
            }
        }
        LoadedContent(data) { d -> FriendsLists(app, d) }
    }
}

@Composable
private fun FriendsLists(app: AppState, d: FriendsResponse) {
    if (d.friends.isEmpty() && d.incoming.isEmpty() && d.outgoing.isEmpty()) {
        Empty("Найдите друзей по нику — они появятся в заездах и чатах.")
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (d.incoming.isNotEmpty()) {
            SectionTitle("Заявки")
            d.incoming.forEach { u ->
                PersonRow(app, u) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        IconBtn(Ic.Check, { app.act { app.api.friendAccept(u.id); app.showToast("Теперь вы друзья"); app.refresh() } }, tint = Rc.Good, background = Rc.Good.copy(alpha = 0.16f))
                        IconBtn(Ic.X, { app.act { app.api.friendRemove(u.id); app.refresh() } })
                    }
                }
            }
        }
        if (d.friends.isNotEmpty()) {
            SectionTitle("Друзья", Modifier.padding(top = 6.dp))
            d.friends.forEach { u -> PersonRow(app, u) { IconBtn(Ic.Chat, { app.openDirectChat(u.id) }) } }
        }
        if (d.outgoing.isNotEmpty()) {
            SectionTitle("Отправленные заявки", Modifier.padding(top = 6.dp))
            d.outgoing.forEach { u -> PersonRow(app, u) { AppButton("Отозвать", { app.act { app.api.friendRemove(u.id); app.showToast("Заявка отозвана"); app.refresh() } }, kind = BtnKind.Ghost, small = true) } }
        }
    }
}

/** Профиль другого пользователя: открытый или друга — с его заездами. */
@Composable
fun UserScreen(app: AppState, route: Route.UserProfile) {
    val data = load(app, route.id, cacheKey = "user:${route.id}") { par({ app.api.profile(route.id) }, { app.api.tracks() }) }
    Page {
        BackLink("Назад") { app.pop() }
        LoadedContent(data) { (p, tracks) -> UserContent(app, p, tracks) }
    }
}

@Composable
private fun UserContent(app: AppState, p: ProfileResponse, tracks: List<Track>) {
    val u = p.user
    LaunchedEffect(Unit) { while (true) { delay(30_000); app.refresh() } }
    val neon = ru.ilyaushenin.nnracing.ui.neonColor(u.neon.firstOrNull())
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(Modifier.neonGlow(u.neon, enabled = u.avatarUrl != null && u.neon.isNotEmpty()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Avatar(u.name, u.avatarUrl, 76.dp, glow = u.avatarUrl != null && u.neon.isNotEmpty(), glowColor = neon)
            Column {
                ru.ilyaushenin.nnracing.ui.NameWithBadge(u.name, u.badge, size = 24.sp, weight = FontWeight.ExtraBold, maxLines = 2)
                Txt("@${u.username}", color = Rc.Muted)
                SeenLine(u.lastSeen, 14)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            when (p.friendStatus) {
                "friends" -> {
                    AppButton("Написать", { app.openDirectChat(u.id) }, icon = Ic.Chat)
                    AppButton("Удалить из друзей", { app.act { app.api.friendRemove(u.id); app.showToast("Удалён из друзей"); app.refresh() } }, kind = BtnKind.Ghost)
                }
                "incoming" -> AppButton("Принять заявку", { app.act { app.api.friendAccept(u.id); app.showToast("Теперь вы друзья"); app.refresh() } })
                "outgoing" -> AppButton("Отозвать заявку", { app.act { app.api.friendRemove(u.id); app.showToast("Заявка отозвана"); app.refresh() } }, kind = BtnKind.Ghost)
                "none" -> AppButton("Добавить в друзья", { app.act { app.api.friendRequest(u.id); app.showToast("Заявка отправлена"); app.refresh() } }, icon = Ic.Plus)
            }
        }
        if (p.visible) AchievementsStrip(p.achievements, own = false)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            SectionTitle("Заезды")
            Chip("${p.runs.size} ${plural(p.runs.size, "заезд", "заезда", "заездов")}")
        }
        if (p.visible) { if (p.runs.isEmpty()) Empty("Заездов пока нет") else RunList(app, p.runs, tracks, u.id) }
        else Empty("Пользователь закрыл свой аккаунт") {
            Icon24(Ic.Lock, Rc.Muted, 28.dp)
            Txt("Заезды видны только друзьям. Добавьте в друзья, чтобы смотреть заезды и писать сообщения.", color = Rc.Muted, size = 13.sp, align = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}

/** «в сети» зелёным с точкой или «был(а) …» серым. */
@Composable
fun SeenLine(lastSeen: Long?, size: Int = 12) {
    if (lastSeen == null) return
    val online = isOnline(lastSeen)
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (online) { ru.ilyaushenin.nnracing.ui.Dot(Rc.Good, 7.dp); androidx.compose.foundation.layout.Spacer(Modifier.padding(start = 5.dp)) }
        Txt(fmtSeen(lastSeen), color = if (online) Rc.Good else Rc.Muted, size = size.sp, maxLines = 1)
    }
}
