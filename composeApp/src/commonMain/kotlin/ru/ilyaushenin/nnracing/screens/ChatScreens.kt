package ru.ilyaushenin.nnracing.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import coil3.compose.AsyncImage
import ru.ilyaushenin.nnracing.data.BASE_URL
import ru.ilyaushenin.nnracing.data.ReplyRef
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.ui.layout.ContentScale
import ru.ilyaushenin.nnracing.data.Poll
import ru.ilyaushenin.nnracing.ui.PickedPhoto
import ru.ilyaushenin.nnracing.ui.rememberImagePicker
import ru.ilyaushenin.nnracing.ui.rememberPhotoPicker
import kotlinx.coroutines.launch
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import kotlin.math.roundToInt
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
    // шапка закреплена, как в самом чате: прокручивается только список
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Column(Modifier.fillMaxWidth().background(Rc.Bg)) {
            Row(Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, top = (18 + ru.ilyaushenin.nnracing.ui.pageTopExtraDp).dp, bottom = 14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                PageTitle("Чат")
                IconBtn(Ic.Compose, { newChat = true })
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(Rc.Line))
        }
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(androidx.compose.foundation.rememberScrollState()).padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = ru.ilyaushenin.nnracing.ui.bottomBarClearance())) {
            LoadedContent(data) { d ->
                if (d.chats.isEmpty()) Empty("Чатов пока нет. Напишите другу или создайте группу — писать можно только друзьям.")
                else Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { d.chats.forEach { ChatRow(app, it, app.user?.id ?: 0) } }
            }
        }
    }
    if (newChat) NewChatSheet(app) { newChat = false }
}

@Composable
private fun ChatRow(app: AppState, c: Chat, meId: Int) {
    val shape = RoundedCornerShape(18.dp)
    Row(
        Modifier.fillMaxWidth().clip(shape).background(Rc.Surface2).border(1.dp, Rc.Line, shape).clickable { app.push(Route.Chat(c.id)) }.padding(horizontal = 14.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (c.kind == "direct" || c.avatarUrl != null) Box {
            Avatar(c.title ?: "?", c.avatarUrl, 50.dp)
            // зелёный кружок: собеседник сейчас в сети
            // зелёный кружок только у личного чата: у группы с фото он принадлежал бы случайному участнику
            if (c.kind == "direct" && isOnline(c.members.firstOrNull { it.id != meId }?.lastSeen)) Box(
                Modifier.align(Alignment.BottomEnd).offset(3.dp, 3.dp).size(15.dp).clip(CircleShape).background(Rc.Surface2).padding(2.5.dp).clip(CircleShape).background(Rc.Good),
            )
        } else Box(Modifier.size(50.dp).clip(RoundedCornerShape(17.dp)).background(Rc.Surface3), contentAlignment = Alignment.Center) { Icon24(Ic.Users, Rc.Muted, 24.dp) }
        Column(Modifier.weight(1f)) {
            ru.ilyaushenin.nnracing.ui.NameWithBadge(c.title ?: "Чат", if (c.kind == "direct") c.members.firstOrNull { it.id != meId }?.badge else null, weight = FontWeight.ExtraBold)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                // у фото и опроса свой значок перед текстом, а не эмодзи
                when (c.last?.kind) {
                    "poll" -> Icon24(Ic.Poll, Rc.Muted, 15.dp)
                    "photo" -> Icon24(Ic.Camera, Rc.Muted, 15.dp)
                }
                Txt(c.last?.body ?: "Сообщений пока нет", color = Rc.Muted, size = 13.sp, maxLines = 1)
            }
        }
        c.last?.let { Txt(ru.ilyaushenin.nnracing.ui.fmtChatListTime(it.createdAt), color = Rc.Muted, size = 12.sp) }
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
    var evCursor by remember(chat.id) { mutableStateOf(0) }   // до какого изменения (правка, удаление, реакция) всё известно
    var replyTo by remember(chat.id) { mutableStateOf<Message?>(null) }
    var editing by remember(chat.id) { mutableStateOf<Message?>(null) }
    var menuMsg by remember(chat.id) { mutableStateOf<Message?>(null) }
    var seenFor by remember(chat.id) { mutableStateOf<Message?>(null) }
    val selected = remember(chat.id) { mutableStateListOf<Int>() }
    val clipboard = LocalClipboardManager.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var flashId by remember(chat.id) { mutableStateOf<Int?>(null) }   // сообщение, к которому перешли по цитате
    var attachSheet by remember { mutableStateOf(false) }
    var pollSheet by remember { mutableStateOf(false) }
    var stickerSheet by remember { mutableStateOf(false) }
    var photoDraft by remember(chat.id) { mutableStateOf<PickedPhoto?>(null) }
    var viewerUrl by remember(chat.id) { mutableStateOf<String?>(null) }
    val pickPhoto = rememberPhotoPicker { photoDraft = it }
    var planSheet by remember { mutableStateOf(false) }
    var membersSheet by remember { mutableStateOf(false) }
    var inviteSheet by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val names = remember(chat) { chat.members.associate { it.id to it.name } }
    val members = remember(chat) { chat.members.associateBy { it.id } }

    // статусы «в сети» и закреп обновляются раз в 20 секунд, пока чат открыт
    LaunchedEffect(chat.id) { while (true) { delay(20_000); app.refresh() } }
    LaunchedEffect(chat.id, app.refreshTick) { pinned = runCatching { app.api.chatPlanned(chat.id) }.getOrDefault(emptyList()) }
    // при возврате в приложение окно снова в фокусе, и длинный опрос начинается заново: «зависший» в фоне запрос ждать не нужно
    val windowFocused = androidx.compose.ui.platform.LocalWindowInfo.current.isWindowFocused
    var loaded by remember(chat.id) { mutableStateOf(false) }
    LaunchedEffect(messages.size, readUpTo, hasMore) { app.cache["msgs:${chat.id}"] = ChatCache(messages.toList(), readUpTo, hasMore) }
    // при выходе из чата сохраняем актуальное состояние целиком: реакции и правки не меняют число сообщений, и без этого в кеше оставалась старая копия
    androidx.compose.runtime.DisposableEffect(chat.id) {
        onDispose { app.cache["msgs:${chat.id}"] = ChatCache(messages.toList(), readUpTo, hasMore) }
    }
    LaunchedEffect(chat.id, windowFocused) {
        if (!loaded) {
            runCatching { app.api.messages(chat.id) }.onSuccess { res ->
                val fresh = res.messages
                if (messages.isEmpty() || (fresh.isNotEmpty() && fresh.first().id > messages.last().id)) {
                    // кеша нет или между ним и новыми сообщениями «дыра»: показываем свежую страницу целиком
                    messages.clear(); messages.addAll(fresh); hasMore = res.hasMore
                } else {
                    // кеш мог устареть: реакции, правки и удаления, случившиеся пока чат был закрыт, берём из свежей страницы
                    val freshIds = fresh.map { it.id }.toSet()
                    val from = fresh.firstOrNull()?.id ?: Int.MAX_VALUE
                    messages.removeAll { it.id >= from && it.id !in freshIds }
                    fresh.forEach { m ->
                        val i = messages.indexOfFirst { it.id == m.id }
                        if (i >= 0) messages[i] = m else messages.add(m)
                    }
                    messages.sortBy { it.id }
                }
                readUpTo = res.readUpTo
                evCursor = res.ev
            }
            loaded = true
        }
        // «длинный опрос»: сообщение приходит сразу, как только оно отправлено; цикл живёт, пока открыт экран чата
        var fails = 0
        while (true) {
            val last = messages.lastOrNull()?.id ?: 0
            try {
                val res = app.api.waitMessages(chat.id, last, readUpTo, evCursor)
                fails = 0
                messages.addAll(res.messages.filter { m -> messages.none { it.id == m.id } })
                evCursor = res.ev
                // правки, удаления и реакции, сделанные другими
                res.events.forEach { e ->
                    val i = messages.indexOfFirst { it.id == e.id }
                    if (i >= 0) { if (e.deleted) { messages.removeAt(i); selected.remove(e.id) } else messages[i] = e }
                }
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

    fun replace(m: Message) { val i = messages.indexOfFirst { it.id == m.id }; if (i >= 0) messages[i] = m }

    fun send() {
        val body = text.trim()
        if (body.isEmpty()) return
        val edit = editing
        val reply = replyTo
        text = ""; editing = null; replyTo = null
        app.act(onError = { text = body; editing = edit; replyTo = reply; app.showToast(it) }) {
            if (edit != null) {
                if (body != edit.body) replace(app.api.editMessage(chat.id, edit.id, body))
            } else {
                // ответ на отправку и длинный опрос могут принести одно и то же сообщение: добавляем, только если его ещё нет
                val sent = app.api.send(chat.id, body, reply?.id)
                if (messages.none { it.id == sent.id }) messages.add(sent)
            }
        }
    }

    fun deleteSelected(ids: List<Int>) {
        app.act {
            app.api.deleteMessages(chat.id, ids)
            messages.removeAll { it.id in ids }
            selected.clear()
        }
    }

    // переход к исходному сообщению цитаты; если оно старше загруженного, история догружается страницами
    fun jumpTo(id: Int) {
        scope.launch {
            var pages = 0
            while (messages.none { it.id == id } && hasMore && pages++ < 40) {
                val res = runCatching { app.api.messages(chat.id, before = messages.first().id) }.getOrNull() ?: break
                val older = res.messages.filter { m -> messages.none { it.id == m.id } }
                messages.addAll(0, older)
                hasMore = res.hasMore && older.isNotEmpty()
            }
            val idx = messages.indexOfFirst { it.id == id }
            if (idx < 0) { app.showToast("Сообщение удалено или слишком старое"); return@launch }
            listState.animateScrollToItem(messages.lastIndex - idx)
            flashId = id; delay(1200); flashId = null
        }
    }

    fun react(m: Message, key: String) { app.act { replace(app.api.react(chat.id, m.id, key)) } }

    Box(Modifier.fillMaxSize()) {
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
                    if (chat.kind == "direct" || chat.avatarUrl != null) Avatar(chat.title ?: "?", chat.avatarUrl, 40.dp)
                    else Box(Modifier.size(40.dp).clip(RoundedCornerShape(14.dp)).background(Rc.Surface3), contentAlignment = Alignment.Center) { Icon24(Ic.Users, Rc.Muted, 20.dp) }
                    Column(Modifier.weight(1f)) {
                        ru.ilyaushenin.nnracing.ui.NameWithBadge(chat.title ?: "Чат", if (chat.kind == "direct") chat.members.firstOrNull { it.id != meId }?.badge else null, weight = FontWeight.ExtraBold)
                        if (chat.kind == "group") Txt(chat.members.joinToString(", ") { it.name }, color = Rc.Muted, size = 12.sp, maxLines = 1)
                        else SeenLine(chat.members.firstOrNull { it.id != meId }?.lastSeen)
                    }
                }
                TrackIconBtn({ planSheet = true })
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
                        val picking = selected.isNotEmpty()
                        Bubble(
                            m, mine = m.userId == meId, read = m.id <= readUpTo, meId = meId, names = names,
                            from = if (chat.kind == "group" && m.userId != meId) names[m.userId] ?: "Бывший участник" else null,
                            selected = m.id in selected, picking = picking, flash = m.id == flashId,
                            avatarSlot = if (chat.kind == "group" && m.userId != meId) members[m.userId] ?: UserBrief(0, "", "?") else null,
                            showAvatar = messages.getOrNull(idx + 1)?.userId != m.userId,
                            onAvatarClick = { if (m.userId in names) app.push(Route.UserProfile(m.userId)) },
                            onPhotoClick = { m.image?.let { viewerUrl = it } },
                            onStickerClick = { stickerSheet = true },
                            onVote = { ids -> app.act { replace(app.api.vote(chat.id, m.id, ids)) } },
                            onQuoteClick = { m.replyTo?.takeIf { !it.deleted }?.let { jumpTo(it.id) } },
                            onClick = { if (picking) { if (m.id in selected) selected.remove(m.id) else selected.add(m.id) } },
                            onLongClick = { if (!picking) menuMsg = m },
                            onReact = { key -> react(m, key) },
                            // двойной тап ставит ❤️, если своего сердца на сообщении ещё нет
                            onDoubleTap = { if (!picking && meId !in (m.reactions["❤️"] ?: emptyList())) react(m, "❤️") },
                            onReply = { if (!picking) { editing = null; replyTo = m } },
                            onFromClick = { if (picking) { if (m.id in selected) selected.remove(m.id) else selected.add(m.id) } else if (m.userId in names) app.push(Route.UserProfile(m.userId)) },
                        )
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
        if (selected.isNotEmpty()) {
            // режим выбора: удалить можно только свои сообщения
            Row(Modifier.fillMaxWidth().background(Rc.Bg).navigationBarsPadding().padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                IconBtn(Ic.X, { selected.clear() })
                Txt("Выбрано: ${selected.size}", weight = FontWeight.Bold, modifier = Modifier.weight(1f))
                AppButton("Удалить", {
                    val foreign = selected.any { id -> messages.find { it.id == id }?.userId != meId }
                    if (foreign) app.showToast("Чужие сообщения удалить нельзя") else deleteSelected(selected.toList())
                }, kind = BtnKind.Danger, icon = Ic.Trash)
            }
        } else Column(Modifier.fillMaxWidth().background(Rc.Bg).navigationBarsPadding()) {
            val ctx = editing ?: replyTo
            if (ctx != null) Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 6.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon24(if (editing != null) Ic.Edit else Ic.Reply, Rc.Accent2, 20.dp)
                Box(Modifier.size(width = 3.dp, height = 34.dp).clip(RoundedCornerShape(2.dp)).background(Rc.Accent2))
                Column(Modifier.weight(1f)) {
                    Txt(if (editing != null) "Изменить сообщение" else "Ответ: ${names[ctx.userId] ?: if (ctx.userId == meId) "Вы" else "Участник"}", color = Rc.Accent2, size = 12.sp, weight = FontWeight.ExtraBold, maxLines = 1)
                    Txt(previewOf(ctx), color = Rc.Muted, size = 13.sp, maxLines = 1)
                }
                IconBtn(Ic.X, { if (editing != null) { editing = null; text = "" }; replyTo = null })
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (editing == null) Box(Modifier.size(46.dp).clip(CircleShape).background(Rc.Surface3).clickable { attachSheet = true }, contentAlignment = Alignment.Center) { Icon24(Ic.Plus, Rc.Text, 22.dp) }
                AppField(text, { text = it }, Modifier.weight(1f), placeholder = "Сообщение", singleLine = false, height = 46.dp, maxLength = 2000, imeAction = ImeAction.Default)
                Box(Modifier.size(46.dp).clip(CircleShape).background(AccentBrush).clickable(onClick = ::send), contentAlignment = Alignment.Center) { Icon24(if (editing != null) Ic.Check else Ic.Send, Color.White, 24.dp, strokeWidth = 2.6f) }
            }
        }
    }
        // фото на весь экран рисуется прямо в экране чата, а не системным окном: оно исчезает вместе с экраном и не может «мелькнуть» поверх другого экрана
        viewerUrl?.let { PhotoViewer(it) { viewerUrl = null } }
    }
    menuMsg?.let { m ->
        val own = m.userId == meId
        var seenCount by remember(m.id) { mutableStateOf<Int?>(null) }
        LaunchedEffect(m.id) { if (own && chat.kind == "group") seenCount = runCatching { app.api.messageSeen(chat.id, m.id).size }.getOrNull() }
        Sheet("Сообщение", { menuMsg = null }) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                // строка реакций выходит за боковые поля шита и доходит до краёв экрана
                val edge = 18.dp
                Row(
                    Modifier.layout { measurable, c ->
                        val extra = (edge * 2).roundToPx()
                        val p = measurable.measure(c.copy(minWidth = c.maxWidth + extra, maxWidth = c.maxWidth + extra))
                        layout(c.maxWidth, p.height) { p.place(-edge.roundToPx(), 0) }
                    }.horizontalScroll(rememberScrollState()).padding(horizontal = edge),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    REACTION_KEYS.forEach { key ->
                        val on = meId in (m.reactions[key] ?: emptyList())
                        Box(
                            Modifier.size(52.dp).clip(CircleShape).background(if (on) Rc.Accent.copy(alpha = 0.25f) else Rc.Surface2)
                                .clickable { menuMsg = null; react(m, key) },
                            contentAlignment = Alignment.Center,
                        ) { ReactionIcon(key, 30.dp) }
                    }
                }
                if (own && chat.kind == "group") MenuItem(Ic.Eye, "Просмотрено" + (seenCount?.let { " · $it" } ?: "")) { menuMsg = null; seenFor = m }
                MenuItem(Ic.Reply, "Ответить") { menuMsg = null; editing = null; replyTo = m }
                MenuItem(Ic.Copy, "Копировать") {
                    menuMsg = null
                    clipboard.setText(AnnotatedString(m.poll?.let { p -> (listOf(p.question) + p.options.map { "• ${it.text}" }).joinToString("\n") } ?: m.body))
                    app.showToast("Скопировано")
                }
                if (m.poll != null && m.poll.options.any { meId in it.voters }) MenuItem(Ic.X, "Отменить мой голос") { menuMsg = null; app.act { replace(app.api.vote(chat.id, m.id, emptyList())) } }
                if (own && m.poll == null) MenuItem(Ic.Edit, "Изменить") { menuMsg = null; replyTo = null; editing = m; text = m.body }
                MenuItem(Ic.Check, "Выбрать") { menuMsg = null; selected.add(m.id) }
                if (own) MenuItem(Ic.Trash, "Удалить", danger = true) { menuMsg = null; deleteSelected(listOf(m.id)) }
            }
        }
    }
    if (attachSheet) Sheet("Прикрепить", { attachSheet = false }) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            MenuItem(Ic.Sticker, "Стикеры") { attachSheet = false; stickerSheet = true }
            MenuItem(Ic.Camera, "Фото") { attachSheet = false; pickPhoto() }
            MenuItem(Ic.Poll, "Опрос") { attachSheet = false; pollSheet = true }
        }
    }
    photoDraft?.let { photo ->
        PhotoSheet(photo, text.trim(), onSend = { caption ->
            val reply = replyTo
            app.act(onError = { app.showToast(it) }) {
                val sent = app.api.sendPhoto(chat.id, photo.bytes, photo.width, photo.height, caption, reply?.id)
                if (messages.none { it.id == sent.id }) messages.add(sent)
                if (caption == text.trim()) text = ""
                replyTo = null; photoDraft = null
            }
        }, onClose = { photoDraft = null })
    }
    if (stickerSheet) StickerSheet(onPick = { name ->
        val reply = replyTo
        stickerSheet = false
        app.act(onError = { app.showToast(it) }) {
            val sent = app.api.sendSticker(chat.id, name, reply?.id)
            if (messages.none { it.id == sent.id }) messages.add(sent)
            replyTo = null
        }
    }, onClose = { stickerSheet = false })
    if (pollSheet) PollSheet(onCreate = { question, options, multiple ->
        val reply = replyTo
        pollSheet = false
        app.act(onError = { app.showToast(it) }) {
            val sent = app.api.sendPoll(chat.id, question, options, multiple, reply?.id)
            if (messages.none { it.id == sent.id }) messages.add(sent)
            replyTo = null
        }
    }, onClose = { pollSheet = false })
    seenFor?.let { m ->
        var users by remember(m.id) { mutableStateOf<List<UserBrief>?>(null) }
        LaunchedEffect(m.id) { users = runCatching { app.api.messageSeen(chat.id, m.id) }.getOrDefault(emptyList()) }
        Sheet("Просмотрено", { seenFor = null }) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (users?.isEmpty() == true) Txt("Пока никто не видел", color = Rc.Muted)
                users?.forEach { u ->
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Rc.Surface2).padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Avatar(u.name, u.avatarUrl, 40.dp)
                        Column(Modifier.weight(1f)) { Txt(u.name, weight = FontWeight.Bold); Txt("@${u.username}", color = Rc.Muted, size = 13.sp) }
                    }
                }
            }
        }
    }
    if (planSheet) PlanSheet(app, tracks, onClose = { planSheet = false }, chatId = chat.id, onSaved = { app.refresh() })
    val pickGroupPhoto = rememberImagePicker { bytes ->
        app.act { app.api.setChatAvatar(chat.id, bytes); app.showToast("Фото группы обновлено"); app.refresh() }
    }
    if (membersSheet) Sheet("Участники", { membersSheet = false }) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // фото группы: любой участник может сменить или убрать
            if (chat.kind == "group") Row(Modifier.padding(bottom = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                if (chat.avatarUrl != null) Avatar(chat.title ?: "?", chat.avatarUrl, 76.dp)
                else Box(Modifier.size(76.dp).clip(RoundedCornerShape(24.dp)).background(Rc.Surface3), contentAlignment = Alignment.Center) { Icon24(Ic.Users, Rc.Muted, 32.dp) }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppButton(if (chat.avatarUrl != null) "Сменить фото" else "Поставить фото", pickGroupPhoto, kind = BtnKind.Ghost, small = true, icon = Ic.Camera)
                    if (chat.avatarUrl != null) AppButton("Убрать", { app.act { app.api.deleteChatAvatar(chat.id); membersSheet = false; app.refresh() } }, kind = BtnKind.Ghost, small = true)
                }
                // выход из группы: красная кнопка справа от фото
                Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                    IconBtn(Ic.Logout, { membersSheet = false; app.act { app.api.leaveChat(chat.id); app.pop(); app.refresh() } }, tint = Rc.Bad, background = Rc.Bad.copy(alpha = 0.14f), size = 46.dp)
                }
            }
            chat.members.forEach { m ->
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 84.dp).clip(RoundedCornerShape(16.dp)).background(Rc.Surface2).border(1.dp, Rc.Line, RoundedCornerShape(16.dp))
                        .clickable { membersSheet = false; if (m.id != meId) app.push(Route.UserProfile(m.id)) }.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // зелёный кружок на аватарке, если участник сейчас в сети; под ником — «в сети» или когда был(а)
                    Box {
                        Avatar(m.name, m.avatarUrl, 40.dp)
                        if (isOnline(m.lastSeen)) Box(
                            Modifier.align(Alignment.BottomEnd).offset(3.dp, 3.dp).size(14.dp).clip(CircleShape).background(Rc.Surface2).padding(2.5.dp).clip(CircleShape).background(Rc.Good),
                        )
                    }
                    Column(Modifier.weight(1f)) {
                        ru.ilyaushenin.nnracing.ui.NameWithBadge(m.name, m.badge, weight = FontWeight.Bold)
                        Txt("@${m.username}", color = Rc.Muted, size = 13.sp)
                        if (!isOnline(m.lastSeen)) SeenLine(m.lastSeen)   // в сети — достаточно зелёного кружка на аватарке
                    }
                }
            }
            AppButton("Добавить участника", { membersSheet = false; inviteSheet = true }, Modifier.fillMaxWidth().padding(top = 6.dp), BtnKind.Ghost, icon = Ic.Plus)
        }
    }
    if (inviteSheet) InviteSheet(app, chat) { inviteSheet = false }
}

/** Ключи реакций: пять эмодзи и картинки с сайта (`:имя:` → /icons/reactions/имя.png). */
private val REACTION_KEYS = listOf("❤️", "👍", "👎", "🔥", "🤡", "🖕", ":gauge:", ":ferrari:", ":mercedes:", ":helmet-ferrari:", ":redbull:", ":flags:", ":helmet-hamilton:")

@Composable
private fun ReactionIcon(key: String, size: androidx.compose.ui.unit.Dp) {
    if (key.startsWith(":")) AsyncImage(model = "$BASE_URL/icons/reactions/${key.trim(':')}.png?v=3", contentDescription = null, modifier = Modifier.size(size))
    else Txt(key, size = (size.value * 0.8f).sp)
}

@Composable
private fun MenuItem(icon: String, label: String, danger: Boolean = false, onClick: () -> Unit) {
    val color = if (danger) Color(0xFFFF6B6B) else Rc.Text
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable(onClick = onClick).padding(horizontal = 8.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) { Icon24(icon, color, 22.dp); Txt(label, color = color, weight = FontWeight.Bold) }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
private fun Bubble(
    m: Message, mine: Boolean, read: Boolean, from: String?, meId: Int, names: Map<Int, String>,
    selected: Boolean, picking: Boolean, flash: Boolean, onDoubleTap: () -> Unit, avatarSlot: UserBrief?, showAvatar: Boolean, onAvatarClick: () -> Unit,
    onPhotoClick: () -> Unit, onStickerClick: () -> Unit, onVote: (List<Int>) -> Unit, onQuoteClick: () -> Unit, onReply: () -> Unit, onFromClick: () -> Unit, onClick: () -> Unit, onLongClick: () -> Unit, onReact: (String) -> Unit,
) {
    // свайп влево — ответить (у всех сообщений, как в Telegram): пузырь сдвигается вбок, слева проявляется значок ответа; после отпускания возвращается на место
    val density = androidx.compose.ui.platform.LocalDensity.current
    val trigger = with(density) { 64.dp.toPx() }
    val shift = remember { androidx.compose.animation.core.Animatable(0f) }
    val swipeScope = androidx.compose.runtime.rememberCoroutineScope()
    Box(
        Modifier.fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick, onDoubleClick = onDoubleTap)
            .pointerInput(Unit) {
                var fired = false
                detectHorizontalDragGestures(
                    onDragStart = { fired = false },
                    onDragEnd = { swipeScope.launch { shift.animateTo(0f, androidx.compose.animation.core.spring(dampingRatio = 0.7f)) } },
                    onDragCancel = { swipeScope.launch { shift.animateTo(0f) } },
                ) { change, amount ->
                    change.consume()
                    val next = (shift.value - amount * 0.6f).coerceIn(0f, trigger * 1.4f)
                    swipeScope.launch { shift.snapTo(next) }
                    if (next >= trigger && !fired) { fired = true; onReply() }
                }
            },
        contentAlignment = if (mine) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        // режим выбора, как в Telegram: слева у каждого сообщения кружок-чекбокс, чужие пузыри сдвигаются вправо
        if (picking) Box(
            Modifier.align(Alignment.CenterStart).padding(start = 2.dp).size(26.dp).clip(CircleShape)
                .then(if (selected) Modifier.background(Rc.Accent) else Modifier.border(2.dp, Color.White.copy(alpha = 0.75f), CircleShape)),
            contentAlignment = Alignment.Center,
        ) { if (selected) Icon24(Ic.Check, Color.White, 16.dp, strokeWidth = 3.2f) }
        val flashAlpha by androidx.compose.animation.core.animateFloatAsState(if (flash) 0.3f else 0f, androidx.compose.animation.core.tween(if (flash) 150 else 700))
        val pickShift by androidx.compose.animation.core.animateDpAsState(if (picking && !mine) 34.dp else if (avatarSlot != null) 38.dp else 0.dp)
        // в группе у последнего сообщения серии одного автора слева его аватарка; у остальных — пустое место, чтобы пузыри стояли ровно
        if (avatarSlot != null && showAvatar && !picking) Box(Modifier.align(Alignment.BottomStart).offset { androidx.compose.ui.unit.IntOffset((-shift.value).roundToInt(), 0) }.clip(CircleShape).clickable(onClick = onAvatarClick)) {
            Avatar(avatarSlot.name, avatarSlot.avatarUrl, 32.dp)
        }
        Box(
            Modifier.align(Alignment.CenterEnd).padding(horizontal = 10.dp).alpha((shift.value / trigger).coerceIn(0f, 1f)).size(34.dp).clip(CircleShape)
                .background(Rc.Surface3),
            contentAlignment = Alignment.Center,
        ) { Icon24(Ic.Reply, Rc.Accent2, 20.dp) }
        Column(
            Modifier.padding(start = pickShift).offset { androidx.compose.ui.unit.IntOffset((-shift.value).roundToInt(), 0) }.widthIn(max = if (picking) 270.dp else 300.dp).clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = if (mine) 18.dp else 6.dp, bottomEnd = if (mine) 6.dp else 18.dp))
                .then(if (m.sticker != null) Modifier else Modifier.background(if (mine) AccentBrush else androidx.compose.ui.graphics.Brush.linearGradient(listOf(Rc.Surface3, Rc.Surface3))))
                .background(Color.White.copy(alpha = if (m.sticker != null) flashAlpha / 2 else flashAlpha)).padding(horizontal = if (m.sticker != null) 4.dp else 12.dp, vertical = if (m.sticker != null) 2.dp else 8.dp),
        ) {
            if (from != null) ru.ilyaushenin.nnracing.ui.NameWithBadge(from, avatarSlot?.badge, size = 12.sp, weight = FontWeight.ExtraBold, color = Rc.Accent2, modifier = Modifier.clickable(onClick = onFromClick))
            m.replyTo?.let { r ->
                Row(Modifier.padding(vertical = 4.dp).clip(RoundedCornerShape(6.dp)).clickable(onClick = onQuoteClick).height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.width(3.dp).fillMaxSize().clip(RoundedCornerShape(2.dp)).background(if (mine) Color.White else Rc.Accent2))
                    Column {
                        Txt(if (r.deleted) "Сообщение удалено" else if (r.userId == meId) "Вы" else names[r.userId] ?: "Участник", color = if (mine) Color.White else Rc.Accent2, size = 12.sp, weight = FontWeight.ExtraBold, maxLines = 1)
                        if (!r.deleted) Txt(r.body, color = Color.White.copy(alpha = 0.75f), size = 13.sp, maxLines = 2)
                    }
                }
            }
            m.sticker?.let { StickerView(it, 168.dp, Modifier.clickable { if (!picking) onStickerClick() }) }
            if (m.imageGone) Row(
                Modifier.padding(vertical = 2.dp).clip(RoundedCornerShape(12.dp)).background(Color.Black.copy(alpha = 0.22f)).padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) { Icon24(Ic.Camera, Color.White.copy(alpha = 0.8f), 18.dp); Txt("Фото удалено", color = Color.White.copy(alpha = 0.8f), size = 14.sp) }
            if (m.image != null) {
                val ratio = if (m.imageW != null && m.imageH != null && m.imageH > 0) m.imageW.toFloat() / m.imageH else 4f / 3f
                AsyncImage(
                    model = BASE_URL + m.image, contentDescription = "Фото", contentScale = ContentScale.Crop,
                    modifier = Modifier.padding(vertical = 2.dp).width(240.dp).aspectRatio(ratio.coerceIn(0.5f, 2f)).clip(RoundedCornerShape(12.dp)).background(Color.Black.copy(alpha = 0.25f)).clickable(onClick = { if (!picking) onPhotoClick() }),
                )
            }
            m.poll?.let { PollBlock(it, meId, mine, enabled = !picking, onVote = onVote) }
            if (m.body.isNotEmpty()) Txt(m.body)
            if (m.reactions.isNotEmpty()) FlowRow(Modifier.padding(top = 4.dp).then(if (m.sticker != null && mine) Modifier.align(Alignment.End) else Modifier), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                m.reactions.forEach { (key, users) ->
                    if (users.isEmpty()) return@forEach
                    val on = meId in users
                    Row(
                        Modifier.clip(RoundedCornerShape(50)).background(if (on) Color.White.copy(alpha = 0.32f) else Color.Black.copy(alpha = 0.22f)).clickable { onReact(key) }.padding(horizontal = 7.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) { ReactionIcon(key, 18.dp); Txt(users.size.toString(), size = 12.sp, weight = FontWeight.Bold) }
                }
            }
            Row(Modifier.align(Alignment.End), verticalAlignment = Alignment.CenterVertically) {
                if (m.edited) Txt("изм. ", color = Color.White.copy(alpha = 0.6f), size = 11.sp)
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

/** Опрос в пузыре: вопрос, тип и варианты с долей голосов; нажатие на вариант голосует, повторное — снимает. */
@Composable
private fun PollBlock(poll: Poll, meId: Int, mine: Boolean, enabled: Boolean, onVote: (List<Int>) -> Unit) {
    val total = poll.options.flatMap { it.voters }.toSet().size
    val picked = poll.options.filter { meId in it.voters }.map { it.id }
    Column(Modifier.widthIn(min = 230.dp).padding(vertical = 2.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Txt(poll.question, weight = FontWeight.ExtraBold, size = 16.sp)
        Txt(if (poll.multiple) "Можно выбрать несколько" else "Один ответ", color = Color.White.copy(alpha = 0.65f), size = 12.sp)
        poll.options.forEach { o ->
            val on = o.id in picked
            val pct = if (total == 0) 0f else o.voters.size.toFloat() / total
            val shape = RoundedCornerShape(12.dp)
            Box(
                Modifier.fillMaxWidth().clip(shape).background(Color.Black.copy(alpha = 0.22f))
                    .clickable(enabled = enabled) { onVote(if (poll.multiple) (if (on) picked - o.id else picked + o.id) else (if (on) emptyList() else listOf(o.id))) },
            ) {
                Box(Modifier.fillMaxWidth(pct).matchParentSize().background(Color.White.copy(alpha = 0.2f)))
                Row(Modifier.padding(horizontal = 12.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        Modifier.size(20.dp).clip(if (poll.multiple) RoundedCornerShape(6.dp) else CircleShape)
                            .then(if (on) Modifier.background(Color.White) else Modifier.border(2.dp, Color.White.copy(alpha = 0.7f), if (poll.multiple) RoundedCornerShape(6.dp) else CircleShape)),
                        contentAlignment = Alignment.Center,
                    ) { if (on) Icon24(Ic.Check, Color(0xFF111111), 14.dp, strokeWidth = 3.4f) }
                    Txt(o.text, size = 15.sp, modifier = Modifier.weight(1f))
                    if (o.voters.isNotEmpty()) Txt("${(pct * 100).roundToInt()}%", size = 13.sp, weight = FontWeight.ExtraBold)
                }
            }
        }
        Txt(if (total > 0) "Проголосовало: $total" else "Пока никто не голосовал", color = Color.White.copy(alpha = 0.65f), size = 12.sp)
    }
}

/** Фото на весь экран; закрывается нажатием или кнопкой «назад». */
@Composable
private fun PhotoViewer(url: String, onClose: () -> Unit) {
    ru.ilyaushenin.nnracing.ui.PlatformBackHandler(enabled = true, onBack = onClose)
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.96f)).clickable(onClick = onClose), contentAlignment = Alignment.Center) {
        AsyncImage(model = BASE_URL + url, contentDescription = "Фото", contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
        Box(Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(14.dp)) { IconBtn(Ic.X, onClose) }
    }
}

/** Окно отправки фото: предпросмотр и подпись. */
@Composable
private fun PhotoSheet(photo: PickedPhoto, initialCaption: String, onSend: (String) -> Unit, onClose: () -> Unit) {
    var caption by remember { mutableStateOf(initialCaption) }
    var sending by remember { mutableStateOf(false) }
    Sheet("Фото", onClose) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            AsyncImage(
                model = photo.bytes, contentDescription = null, contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxWidth().height(280.dp).clip(RoundedCornerShape(16.dp)).background(Rc.Surface2),
            )
            AppField(caption, { caption = it }, placeholder = "Подпись (необязательно)", singleLine = false, maxLength = 2000)
            AppButton("Отправить", { sending = true; onSend(caption.trim()) }, Modifier.fillMaxWidth(), enabled = !sending, icon = Ic.Send)
        }
    }
}

/** Окно создания опроса: вопрос, 2–10 вариантов, один ответ или несколько. */
@Composable
private fun PollSheet(onCreate: (String, List<String>, Boolean) -> Unit, onClose: () -> Unit) {
    var question by remember { mutableStateOf("") }
    val options = remember { mutableStateListOf("", "") }
    var multiple by remember { mutableStateOf(false) }
    val ready = question.isNotBlank() && options.count { it.isNotBlank() } >= 2
    Sheet("Новый опрос", onClose) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            AppField(question, { question = it }, placeholder = "Вопрос", maxLength = 200)
            options.forEachIndexed { i, text ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppField(text, { options[i] = it }, Modifier.weight(1f), placeholder = "Вариант ${i + 1}", maxLength = 100)
                    if (options.size > 2) IconBtn(Ic.X, { options.removeAt(i) })
                }
            }
            if (options.size < 10) AppButton("Добавить вариант", { options.add("") }, Modifier.fillMaxWidth(), BtnKind.Ghost, icon = Ic.Plus)
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Rc.Surface2).border(1.dp, Rc.Line, RoundedCornerShape(16.dp)).padding(14.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Column(Modifier.weight(1f)) {
                    Txt("Несколько ответов", weight = FontWeight.Bold)
                    Txt("Иначе можно выбрать только один вариант", color = Rc.Muted, size = 13.sp)
                }
                androidx.compose.material3.Switch(
                    checked = multiple, onCheckedChange = { multiple = it },
                    colors = androidx.compose.material3.SwitchDefaults.colors(checkedTrackColor = Rc.Accent, checkedThumbColor = Color.White, uncheckedTrackColor = Rc.Surface3, uncheckedThumbColor = Color.White, uncheckedBorderColor = Rc.Surface3),
                )
            }
            AppButton("Создать опрос", { onCreate(question.trim(), options.map { it.trim() }.filter { it.isNotEmpty() }, multiple) }, Modifier.fillMaxWidth(), enabled = ready)
        }
    }
}

/** Короткий текст сообщения для ответа и списка: у фото и опроса своего текста нет. */
private fun previewOf(m: Message): String = m.body.ifEmpty { if (m.sticker != null) "Стикер" else if (m.imageGone) "Фото удалено" else if (m.image != null) "Фото" else m.poll?.let { "Опрос: ${it.question}" } ?: "" }

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
