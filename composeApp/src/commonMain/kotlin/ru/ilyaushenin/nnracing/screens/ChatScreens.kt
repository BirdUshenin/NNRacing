package ru.ilyaushenin.nnracing.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import ru.ilyaushenin.nnracing.AppState
import ru.ilyaushenin.nnracing.Route
import ru.ilyaushenin.nnracing.data.ApiException
import ru.ilyaushenin.nnracing.data.Chat
import ru.ilyaushenin.nnracing.data.Message
import ru.ilyaushenin.nnracing.data.Planned
import ru.ilyaushenin.nnracing.data.Track
import ru.ilyaushenin.nnracing.data.UserBrief
import ru.ilyaushenin.nnracing.ui.AccentBrush
import ru.ilyaushenin.nnracing.ui.AppButton
import ru.ilyaushenin.nnracing.ui.AppField
import ru.ilyaushenin.nnracing.ui.BtnKind
import ru.ilyaushenin.nnracing.ui.Avatar
import ru.ilyaushenin.nnracing.ui.Empty
import ru.ilyaushenin.nnracing.ui.Ic
import ru.ilyaushenin.nnracing.ui.Icon24
import ru.ilyaushenin.nnracing.ui.IconBtn
import ru.ilyaushenin.nnracing.ui.LoadingLogo
import ru.ilyaushenin.nnracing.ui.LoadedContent
import ru.ilyaushenin.nnracing.ui.Page
import ru.ilyaushenin.nnracing.ui.PageTitle
import ru.ilyaushenin.nnracing.ui.Rc
import ru.ilyaushenin.nnracing.ui.Sheet
import ru.ilyaushenin.nnracing.ui.TrackIconBtn
import ru.ilyaushenin.nnracing.ui.Txt
import ru.ilyaushenin.nnracing.ui.fmtClock
import ru.ilyaushenin.nnracing.ui.dayKey
import ru.ilyaushenin.nnracing.ui.fmtDayLabel
import ru.ilyaushenin.nnracing.ui.fmtLong
import ru.ilyaushenin.nnracing.ui.isOnline
import ru.ilyaushenin.nnracing.ui.load
import ru.ilyaushenin.nnracing.ui.par

/** Личный чат с другом: находим существующий или создаём и открываем. */
fun AppState.openDirectChat(userId: Int) {
    act { val chat = api.createChat(listOf(userId), null); push(Route.Chat(chat.id)) }
}

@Composable
fun ChatsScreen(app: AppState) {
    val data = load(app, cacheKey = "chats") { app.api.chats() }
    var newChat by remember { mutableStateOf(false) }
    // пока открыта вкладка «Чат», список обновляется раз в 10 секунд (без «мигания»: старые данные остаются на экране)
    LaunchedEffect(Unit) { while (true) { delay(10_000); app.refresh() } }
    Page {
        Row(Modifier.fillMaxWidth().padding(bottom = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            PageTitle("Чат")
            ru.ilyaushenin.nnracing.ui.Chip("+ Новый чат", onClick = { newChat = true })
        }
        LoadedContent(data) { d ->
            if (d.chats.isEmpty()) Empty("Чатов пока нет. Напишите другу или создайте группу — писать можно только друзьям.")
            else Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { d.chats.forEach { ChatRow(app, it, app.user?.id ?: 0) } }
        }
    }
    if (newChat) NewChatSheet(app) { newChat = false }
}

@Composable
private fun ChatRow(app: AppState, c: Chat, meId: Int) {
    val shape = RoundedCornerShape(18.dp)
    Row(
        Modifier.fillMaxWidth().clip(shape).background(Rc.Surface2).border(1.dp, Rc.Line, shape).clickable { app.push(Route.Chat(c.id)) }.padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (c.kind == "direct") Box {
            Avatar(c.title ?: "?", c.avatarUrl, 44.dp)
            // зелёный кружок: собеседник сейчас в сети
            if (isOnline(c.members.firstOrNull { it.id != meId }?.lastSeen)) Box(
                Modifier.align(Alignment.BottomEnd).offset(3.dp, 3.dp).size(15.dp).clip(CircleShape).background(Rc.Surface2).padding(2.5.dp).clip(CircleShape).background(Rc.Good),
            )
        } else Box(Modifier.size(44.dp).clip(RoundedCornerShape(15.dp)).background(Rc.Surface3), contentAlignment = Alignment.Center) { Icon24(Ic.Users, Rc.Muted, 22.dp) }
        Column(Modifier.weight(1f)) {
            Txt(c.title ?: "Чат", weight = FontWeight.ExtraBold, maxLines = 1)
            Txt(c.last?.body ?: "Сообщений пока нет", color = Rc.Muted, size = 13.sp, maxLines = 1)
        }
        c.last?.let { Txt(fmtClock(it.createdAt), color = Rc.Muted, size = 12.sp) }
        if (c.unread > 0) Box(Modifier.clip(CircleShape).background(Rc.Accent).padding(horizontal = 7.dp, vertical = 2.dp)) { Txt(c.unread.toString(), color = Color.White, size = 12.sp, weight = FontWeight.ExtraBold) }
    }
}

@Composable
private fun NewChatSheet(app: AppState, onClose: () -> Unit) {
    var friends by remember { mutableStateOf<List<UserBrief>?>(null) }
    val picked = remember { mutableStateListOf<Int>() }
    var title by remember { mutableStateOf("") }
    LaunchedEffect(Unit) { friends = runCatching { app.api.friends().friends }.getOrDefault(emptyList()) }
    Sheet("Новый чат", onClose) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            val list = friends
            if (list != null && list.isEmpty()) Txt("Писать можно только друзьям — сначала добавьте их во вкладке «Друзья».", color = Rc.Muted)
            else {
                Txt("Выберите одного друга для личного чата или нескольких — для группы.", color = Rc.Muted, size = 13.sp)
                list?.forEach { f ->
                    val on = f.id in picked
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(if (on) Rc.Accent.copy(alpha = 0.10f) else Rc.Surface2)
                            .border(1.dp, if (on) Rc.Accent.copy(alpha = 0.7f) else Rc.Line, RoundedCornerShape(16.dp))
                            .clickable { if (on) picked.remove(f.id) else picked.add(f.id) }.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) { Avatar(f.name, f.avatarUrl, 40.dp); Txt(f.name, weight = FontWeight.Bold, modifier = Modifier.weight(1f)); if (on) Icon24(Ic.Check, Rc.Accent, 18.dp) }
                }
                if (picked.size > 1) AppField(title, { title = it }, placeholder = "Название группы", maxLength = 40)
                AppButton(if (picked.size > 1) "Создать группу" else "Написать", {
                    app.act { val chat = app.api.createChat(picked.toList(), if (picked.size > 1) title else null); onClose(); app.push(Route.Chat(chat.id)) }
                }, Modifier.fillMaxWidth(), enabled = picked.isNotEmpty())
            }
        }
    }
}

@Composable
fun ChatScreen(app: AppState, route: Route.Chat) {
    val me = app.user ?: return
    val chats = load(app, route.id, cacheKey = "chat:${route.id}") { par({ app.api.chats() }, { app.api.tracks() }) }
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        LoadedContent(chats) { (all, tracks) ->
            val chat = all.chats.find { it.id == route.id }
            if (chat == null) Empty("Чат не найден", Modifier.padding(18.dp)) else ChatBody(app, chat, tracks, me.id)
        }
    }
}

@Composable
private fun ChatBody(app: AppState, chat: Chat, tracks: List<Track>, meId: Int) {
    // последняя открытая версия чата берётся из кеша: переписка видна сразу, свежее подгружается в фоне
    val cached = remember(chat.id) { app.cache["msgs:${chat.id}"] as? ChatCache }
    val messages = remember(chat.id) { mutableStateListOf<Message>().also { list -> cached?.let { list.addAll(it.messages) } } }
    var pinned by remember { mutableStateOf<List<Planned>>(emptyList()) }
    var text by remember { mutableStateOf("") }
    var readUpTo by remember(chat.id) { mutableStateOf(cached?.readUpTo ?: 0) }   // до какого сообщения дочитали собеседники
    var hasMore by remember(chat.id) { mutableStateOf(cached?.hasMore ?: false) }   // есть ли история старше загруженной
    var loadingOlder by remember(chat.id) { mutableStateOf(false) }
    var planSheet by remember { mutableStateOf(false) }
    var membersSheet by remember { mutableStateOf(false) }
    var inviteSheet by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val names = remember(chat) { chat.members.associate { it.id to it.name } }

    // статусы «в сети» и закреп обновляются раз в 20 секунд, пока чат открыт
    LaunchedEffect(chat.id) { while (true) { delay(20_000); app.refresh() } }
    LaunchedEffect(chat.id, app.refreshTick) { pinned = runCatching { app.api.chatPlanned(chat.id) }.getOrDefault(emptyList()) }
    // при возврате в приложение окно снова в фокусе, и длинный опрос начинается заново: «зависший» в фоне запрос ждать не нужно
    val windowFocused = androidx.compose.ui.platform.LocalWindowInfo.current.isWindowFocused
    var loaded by remember(chat.id) { mutableStateOf(false) }
    LaunchedEffect(messages.size, readUpTo, hasMore) { app.cache["msgs:${chat.id}"] = ChatCache(messages.toList(), readUpTo, hasMore) }
    LaunchedEffect(chat.id, windowFocused) {
        if (!loaded) {
            runCatching { app.api.messages(chat.id) }.onSuccess { res ->
                val fresh = res.messages
                if (messages.isEmpty() || (fresh.isNotEmpty() && fresh.first().id > messages.last().id)) {
                    // кеша нет или между ним и новыми сообщениями «дыра»: показываем свежую страницу целиком
                    messages.clear(); messages.addAll(fresh); hasMore = res.hasMore
                } else messages.addAll(fresh.filter { m -> messages.none { it.id == m.id } })
                readUpTo = res.readUpTo
            }
            loaded = true
        }
        // «длинный опрос»: сообщение приходит сразу, как только оно отправлено; цикл живёт, пока открыт экран чата
        var fails = 0
        while (true) {
            val last = messages.lastOrNull()?.id ?: 0
            try {
                val res = app.api.waitMessages(chat.id, last, readUpTo)
                fails = 0
                messages.addAll(res.messages.filter { m -> messages.none { it.id == m.id } })
                readUpTo = res.readUpTo
            } catch (e: ApiException) {
                if (e.status == 404 || e.status == 401) break
                delay(if (e.status == 0 && ++fails <= 2) 300 else 3000)   // тайм-аут запроса — сразу повторяем, повторные сбои — реже
            }
        }
    }
    LaunchedEffect(chat.id) { while (true) { delay(15_000); pinned = runCatching { app.api.chatPlanned(chat.id) }.getOrDefault(pinned) } }
    // новое сообщение: если читаем низ ленты или написали сами — прокручиваем к нему (лента перевёрнута: низ — это индекс 0)
    LaunchedEffect(messages.lastOrNull()?.id) {
        val newest = messages.lastOrNull() ?: return@LaunchedEffect
        if (listState.firstVisibleItemIndex <= 2 || newest.userId == meId) listState.animateScrollToItem(0)
    }
    // история подгружается по 50 сообщений, когда долистали до верха загруженной части
    LaunchedEffect(chat.id) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }.collect { lastVisible ->
            if (hasMore && !loadingOlder && messages.isNotEmpty() && lastVisible >= messages.size - 6) {
                loadingOlder = true
                try {
                    val res = app.api.messages(chat.id, before = messages.first().id)
                    val older = res.messages.filter { m -> messages.none { it.id == m.id } }
                    messages.addAll(0, older)
                    hasMore = res.hasMore && older.isNotEmpty()
                } catch (e: ApiException) {
                    // повторим при следующей прокрутке
                } finally { loadingOlder = false }
            }
        }
    }

    fun send() {
        val body = text.trim()
        if (body.isEmpty()) return
        text = ""
        app.act(onError = { text = body; app.showToast(it) }) {
            // ответ на отправку и длинный опрос могут принести одно и то же сообщение: добавляем, только если его ещё нет
            val sent = app.api.send(chat.id, body)
            if (messages.none { it.id == sent.id }) messages.add(sent)
        }
    }

    Column(Modifier.fillMaxSize().imePadding()) {
        Column(Modifier.fillMaxWidth().background(Rc.Bg)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                IconBtn(Ic.Back, { app.pop() }, size = 46.dp)
                // нажатие на шапку: у личного чата — профиль собеседника, у группы — список участников
                Row(
                    Modifier.weight(1f).clickable {
                        if (chat.kind == "direct") chat.members.firstOrNull { it.id != meId }?.let { app.push(Route.UserProfile(it.id)) } else membersSheet = true
                    },
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (chat.kind == "direct") Avatar(chat.title ?: "?", chat.avatarUrl, 40.dp)
                    else Box(Modifier.size(40.dp).clip(RoundedCornerShape(14.dp)).background(Rc.Surface3), contentAlignment = Alignment.Center) { Icon24(Ic.Users, Rc.Muted, 20.dp) }
                    Column(Modifier.weight(1f)) {
                        Txt(chat.title ?: "Чат", weight = FontWeight.ExtraBold, maxLines = 1)
                        if (chat.kind == "group") Txt(chat.members.joinToString(", ") { it.name }, color = Rc.Muted, size = 12.sp, maxLines = 1)
                        else SeenLine(chat.members.firstOrNull { it.id != meId }?.lastSeen)
                    }
                }
                TrackIconBtn({ planSheet = true })
                if (chat.kind == "group") IconBtn(Ic.Logout, { app.act { app.api.leaveChat(chat.id); app.pop(); app.refresh() } }, size = 46.dp)
            }
            pinned.forEach { PinnedPlan(app, it, tracks) }
            Box(Modifier.fillMaxWidth().size(1.dp).background(Rc.Line))
        }
        // лента перевёрнута (reverseLayout): сразу открывается на последних сообщениях без прыжка, а старые догружаются сверху
        Box(Modifier.weight(1f).fillMaxWidth()) {
            LazyColumn(
                Modifier.fillMaxSize(), state = listState, reverseLayout = true,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(messages.size, key = { i -> messages[messages.lastIndex - i].id }) { i ->
                    val idx = messages.lastIndex - i
                    val m = messages[idx]
                    val prev = messages.getOrNull(idx - 1)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (prev == null || dayKey(prev.createdAt) != dayKey(m.createdAt)) DaySeparator(fmtDayLabel(m.createdAt))
                        Bubble(m, mine = m.userId == meId, read = m.id <= readUpTo, from = if (chat.kind == "group" && m.userId != meId) names[m.userId] ?: "Бывший участник" else null)
                    }
                }
            }
            // пока первая страница не загрузилась и кеша нет — значок загрузки вместо чёрного экрана
            if (!loaded && messages.isEmpty()) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { LoadingLogo(64.dp) }
            // плавающая дата, как в Telegram: видна во время прокрутки и гаснет через секунду после остановки
            val topLabel by remember {
                androidx.compose.runtime.derivedStateOf {
                    val top = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index
                    top?.let { messages.getOrNull(messages.lastIndex - it) }?.let { fmtDayLabel(it.createdAt) }
                }
            }
            var dateVisible by remember { mutableStateOf(false) }
            LaunchedEffect(listState.isScrollInProgress) {
                if (listState.isScrollInProgress) dateVisible = true else { delay(1200); dateVisible = false }
            }
            androidx.compose.animation.AnimatedVisibility(
                visible = dateVisible && topLabel != null,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 10.dp),
                enter = androidx.compose.animation.fadeIn(), exit = androidx.compose.animation.fadeOut(),
            ) {
                Box(Modifier.clip(RoundedCornerShape(50)).background(Color(0xEB20202A)).border(1.dp, Rc.Line, RoundedCornerShape(50)).padding(horizontal = 14.dp, vertical = 5.dp)) {
                    Txt(topLabel ?: "", size = 13.sp, weight = FontWeight.Bold)
                }
            }
        }
        Row(Modifier.fillMaxWidth().background(Rc.Bg).navigationBarsPadding().padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AppField(text, { text = it }, Modifier.weight(1f), placeholder = "Сообщение", singleLine = false, height = 46.dp, maxLength = 2000, imeAction = ImeAction.Default)
            Box(Modifier.size(46.dp).clip(CircleShape).background(AccentBrush).clickable(onClick = ::send), contentAlignment = Alignment.Center) { Icon24(Ic.Send, Color.White, 24.dp, strokeWidth = 2.6f) }
        }
    }
    if (planSheet) PlanSheet(app, tracks, onClose = { planSheet = false }, chatId = chat.id, onSaved = { app.refresh() })
    if (membersSheet) Sheet("Участники", { membersSheet = false }) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            chat.members.forEach { m ->
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Rc.Surface2).border(1.dp, Rc.Line, RoundedCornerShape(16.dp))
                        .clickable { membersSheet = false; if (m.id != meId) app.push(Route.UserProfile(m.id)) }.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Avatar(m.name, m.avatarUrl, 40.dp)
                    Column(Modifier.weight(1f)) { Txt(m.name, weight = FontWeight.Bold); Txt("@${m.username}", color = Rc.Muted, size = 13.sp) }
                }
            }
            AppButton("Добавить участника", { membersSheet = false; inviteSheet = true }, Modifier.fillMaxWidth().padding(top = 6.dp), BtnKind.Ghost, icon = Ic.Plus)
        }
    }
    if (inviteSheet) InviteSheet(app, chat) { inviteSheet = false }
}

@Composable
private fun Bubble(m: Message, mine: Boolean, read: Boolean, from: String?) {
    Box(Modifier.fillMaxWidth(), contentAlignment = if (mine) Alignment.CenterEnd else Alignment.CenterStart) {
        Column(
            Modifier.widthIn(max = 300.dp).clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = if (mine) 18.dp else 6.dp, bottomEnd = if (mine) 6.dp else 18.dp))
                .background(if (mine) AccentBrush else androidx.compose.ui.graphics.Brush.linearGradient(listOf(Rc.Surface3, Rc.Surface3))).padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            if (from != null) Txt(from, color = Rc.Accent2, size = 12.sp, weight = FontWeight.ExtraBold)
            Txt(m.body)
            Row(Modifier.align(Alignment.End), verticalAlignment = Alignment.CenterVertically) {
                Txt(fmtClock(m.createdAt), color = Color.White.copy(alpha = 0.6f), size = 11.sp)
                // одна галочка — отправлено, две — прочитано (как в Telegram)
                if (mine) Box(Modifier.padding(start = 4.dp).width(if (read) 20.dp else 14.dp)) {
                    val tint = if (read) Color.White else Color.White.copy(alpha = 0.65f)
                    Icon24(Ic.Check, tint, 14.dp, strokeWidth = 2.4f)
                    if (read) Icon24(Ic.Check, tint, 14.dp, strokeWidth = 2.4f, modifier = Modifier.padding(start = 6.dp))
                }
            }
        }
    }
}

/** Закреплённый в чате запланированный заезд: трасса, дата, таймер и кнопки ответа. */
@Composable
private fun PinnedPlan(app: AppState, p: Planned, tracks: List<Track>) {
    val track = tracks.find { it.id == p.trackId }
    val variant = track?.variants?.find { it.id == p.variant }
    val color = Rc.track(p.trackId)
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 14.dp).padding(bottom = 10.dp).clip(RoundedCornerShape(14.dp)).background(Rc.Surface2).border(1.dp, Rc.Line, RoundedCornerShape(14.dp)).padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(Modifier.size(width = 3.dp, height = 36.dp).clip(RoundedCornerShape(2.dp)).background(color))
        Column(Modifier.weight(1f)) {
            Txt(listOfNotNull(track?.name ?: "Заезд", variant?.name).joinToString(" · "), weight = FontWeight.Bold, size = 14.sp, maxLines = 1)
            Txt("${fmtLong(p.startsAt)} · ${fmtClock(p.startsAt)}", color = Rc.Muted, size = 12.sp)
            InviteButtons(app, p)
        }
        Column(horizontalAlignment = Alignment.End) {
            CountdownText(p.startsAt, color)
            Txt("едут ${p.members.count { it.status == "going" }} из ${p.members.size}", color = Rc.Muted, size = 12.sp)
        }
    }
}


/** Приглашение друзей в уже существующую группу. */
@Composable
private fun InviteSheet(app: AppState, chat: Chat, onClose: () -> Unit) {
    var friends by remember { mutableStateOf<List<UserBrief>?>(null) }
    val picked = remember { mutableStateListOf<Int>() }
    LaunchedEffect(Unit) { friends = runCatching { app.api.friends().friends }.getOrDefault(emptyList()).filter { f -> chat.members.none { it.id == f.id } } }
    Sheet("Добавить в чат", onClose) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            val list = friends
            if (list != null && list.isEmpty()) Txt("Все ваши друзья уже в этом чате. Новых друзей можно найти по нику во вкладке «Друзья».", color = Rc.Muted)
            list?.forEach { f ->
                val on = f.id in picked
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(if (on) Rc.Accent.copy(alpha = 0.10f) else Rc.Surface2)
                        .border(1.dp, if (on) Rc.Accent.copy(alpha = 0.7f) else Rc.Line, RoundedCornerShape(16.dp))
                        .clickable { if (on) picked.remove(f.id) else picked.add(f.id) }.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) { Avatar(f.name, f.avatarUrl, 40.dp); Txt(f.name, weight = FontWeight.Bold, modifier = Modifier.weight(1f)); if (on) Icon24(Ic.Check, Rc.Accent, 18.dp) }
            }
            if (!list.isNullOrEmpty()) AppButton("Добавить", {
                app.act { app.api.addChatMembers(chat.id, picked.toList()); app.showToast("Участники добавлены"); onClose(); app.refresh() }
            }, Modifier.fillMaxWidth(), enabled = picked.isNotEmpty())
        }
    }
}

/** Плашка даты между сообщениями разных дней, как в Telegram. */
@Composable
private fun DaySeparator(label: String) {
    Box(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 2.dp), contentAlignment = Alignment.Center) {
        Box(Modifier.clip(RoundedCornerShape(50)).background(Color(0x14FFFFFF)).padding(horizontal = 12.dp, vertical = 4.dp)) {
            Txt(label, color = Rc.Muted, size = 12.sp, weight = FontWeight.Bold)
        }
    }
}

/** Кеш переписки одного чата: показывается сразу при повторном открытии. */
private data class ChatCache(val messages: List<Message>, val readUpTo: Int, val hasMore: Boolean)
