package ru.ilyaushenin.nnracing.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.ilyaushenin.nnracing.AppState
import ru.ilyaushenin.nnracing.Route
import ru.ilyaushenin.nnracing.data.Run
import ru.ilyaushenin.nnracing.data.Track
import ru.ilyaushenin.nnracing.ui.AppButton
import ru.ilyaushenin.nnracing.ui.AppField
import ru.ilyaushenin.nnracing.ui.Avatar
import ru.ilyaushenin.nnracing.ui.BtnKind
import ru.ilyaushenin.nnracing.ui.Empty
import ru.ilyaushenin.nnracing.ui.FieldLabel
import ru.ilyaushenin.nnracing.ui.Ic
import ru.ilyaushenin.nnracing.ui.NEON_COLORS
import ru.ilyaushenin.nnracing.ui.neonColor
import ru.ilyaushenin.nnracing.ui.neonGlow
import ru.ilyaushenin.nnracing.ui.IconBtn
import ru.ilyaushenin.nnracing.ui.LoadedContent
import ru.ilyaushenin.nnracing.ui.Page
import ru.ilyaushenin.nnracing.ui.Rc
import ru.ilyaushenin.nnracing.ui.Segmented
import ru.ilyaushenin.nnracing.ui.Sheet
import ru.ilyaushenin.nnracing.ui.Txt
import ru.ilyaushenin.nnracing.ui.load
import ru.ilyaushenin.nnracing.ui.par
import ru.ilyaushenin.nnracing.ui.rememberImagePicker

private data class RunsData(val runs: List<Run>, val tracks: List<Track>)

@Composable
fun ProfileScreen(app: AppState, route: Route.Me) {
    val user = app.user ?: return
    var settings by remember { mutableStateOf(false) }
    var runSheet by remember { mutableStateOf(false) }
    var planSheet by remember { mutableStateOf(false) }
    var tab by remember(route.tab) { mutableStateOf(route.tab) }

    Page {
        val neon = neonColor(user.neon.firstOrNull())
        // шапка и достижения в одном блоке со свечением: пятно цвета неона уходит вниз и частично накрывает награды
        Column(Modifier.neonGlow(user.neon, enabled = user.avatarUrl != null && user.neon.isNotEmpty())) {
        Row(Modifier.fillMaxWidth().padding(bottom = 18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Avatar(user.name, user.avatarUrl, 76.dp, glow = user.avatarUrl != null && user.neon.isNotEmpty(), glowColor = neon)
            Column(Modifier.weight(1f)) {
                ru.ilyaushenin.nnracing.ui.NameWithBadge(user.name, user.badge, size = 24.sp, weight = FontWeight.ExtraBold)
                Txt("@${user.username} · ${if (user.isPublic) "открытый" else "закрытый"}", color = Rc.Muted)
            }
            IconBtn(Ic.Edit, { settings = true })
        }
        val achievements = load(app, cacheKey = "achievements") { app.api.achievements() }
        achievements.value?.let { AchievementsStrip(it) }
        }
        Segmented(listOf("Заезды", "Друзья", "Статистика"), tab, { tab = it }, Modifier.padding(bottom = 18.dp), badges = listOf(0, app.requests, 0))
        when (tab) {
            0 -> RunsTab(app, onCreate = { runSheet = true }, onPlan = { planSheet = true })
            1 -> FriendsTab(app)
            else -> StatsTab(app)
        }
    }
    if (settings) ProfileSheet(app) { settings = false }
    if (runSheet || planSheet) {
        val tracks = load(app, cacheKey = "tracks") { app.api.tracks() }
        tracks.value?.let { t ->
            if (runSheet) RunSheet(app, t, onClose = { runSheet = false })
            if (planSheet) PlanSheet(app, t, onClose = { planSheet = false })
        }
    }
}

@Composable
private fun RunsTab(app: AppState, onCreate: () -> Unit, onPlan: () -> Unit) {
    val me = app.user ?: return
    val data = load(app, cacheKey = "myruns") { par({ app.api.runs() }, { app.api.tracks() }).let { (r, t) -> RunsData(r, t) } }
    LoadedContent(data) { d ->
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            AppButton("Создать заезд", onCreate, Modifier.fillMaxWidth(), icon = Ic.Plus)
            if (d.runs.isEmpty()) Empty("Заездов пока нет. Создайте первый — круги и лучшее время сохранятся на трассе.") {
                AppButton("Запланировать", onPlan, kind = BtnKind.Ghost, icon = Ic.Clock)
            } else RunList(app, d.runs, d.tracks, me.id)
        }
    }
}

/** Настройки профиля: имя, открытость, аватарка, выход. */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun ProfileSheet(app: AppState, onClose: () -> Unit) {
    val user = app.user ?: return
    var name by remember { mutableStateOf(user.name) }
    var isPublic by remember { mutableStateOf(user.isPublic) }
    // огни свечения: до шести слева направо; число доступных зависит от заездов (1, 5, 10, 50, 100)
    val lights = remember { mutableStateListOf<String>().also { it.addAll(user.neon) } }
    var slot by remember { mutableStateOf(0) }
    var badge by remember { mutableStateOf(user.badge) }
    var error by remember { mutableStateOf("") }
    val pick = rememberImagePicker { bytes ->
        app.act(onError = { error = it }) { app.user = app.api.setAvatar(bytes); app.showToast("Аватарка обновлена") }
    }
    Sheet("Профиль", onClose) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Avatar(user.name, user.avatarUrl, 76.dp)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppButton("Сменить", pick, kind = BtnKind.Ghost, small = true, icon = Ic.Camera)
                    if (user.avatarUrl != null) AppButton("Убрать", { app.act(onError = { error = it }) { app.user = app.api.deleteAvatar() } }, kind = BtnKind.Ghost, small = true)
                }
            }
            Column { FieldLabel("Имя"); AppField(name, { name = it }, maxLength = 40) }
            Column {
                FieldLabel("Эмодзи-статус")
                // значок виден рядом с вашим ником в профиле, чатах и списках; нажмите ещё раз на выбранный, чтобы убрать
                androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // первая клетка — «без статуса»: перечёркнутый круг; выбрана, когда значка нет
                    Box(
                        Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).background(if (badge == null) Rc.Surface3 else Rc.Surface2)
                            .border(if (badge == null) 2.dp else 1.dp, if (badge == null) androidx.compose.ui.graphics.Color.White else Rc.Line, RoundedCornerShape(16.dp))
                            .clickable { badge = null },
                        contentAlignment = Alignment.Center,
                    ) { ru.ilyaushenin.nnracing.ui.Icon24(Ic.Ban, Rc.Muted, 28.dp, strokeWidth = 1.8f) }
                    ru.ilyaushenin.nnracing.ui.BADGES.forEach { (key, _) ->
                        val on = badge == key
                        Box(
                            Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).background(if (on) Rc.Surface3 else Rc.Surface2)
                                .border(if (on) 2.dp else 1.dp, if (on) androidx.compose.ui.graphics.Color.White else Rc.Line, RoundedCornerShape(16.dp))
                                .clickable { badge = if (on) null else key },
                            contentAlignment = Alignment.Center,
                        ) { ru.ilyaushenin.nnracing.ui.BadgeIcon(key, 36.dp) }
                    }
                }
            }
            Column {
                FieldLabel("Свечение профиля")
                // цвет доступен только с фото профиля
                if (user.avatarUrl == null) Txt("Сначала поставьте фото профиля — тогда можно выбрать цвет свечения.", color = Rc.Muted, size = 13.sp, modifier = Modifier.padding(bottom = 8.dp))
                val needs = user.neonUnlock
                // огни слева направо: чем больше заездов, тем больше огней можно включить
                Row(Modifier.fillMaxWidth().then(if (user.avatarUrl == null) Modifier.alpha(0.35f) else Modifier), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    needs.forEachIndexed { i, need ->
                        val open = i < user.neonSlots
                        val set = i < lights.size
                        Column(
                            Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(if (i == slot && open) Rc.Surface3 else Rc.Surface2)
                                .border(1.dp, if (i == slot && open) Rc.LineStrong else Rc.Line, RoundedCornerShape(14.dp))
                                .clickable(enabled = open && user.avatarUrl != null) { slot = i }.padding(vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            androidx.compose.foundation.layout.Box(
                                Modifier.size(28.dp).clip(androidx.compose.foundation.shape.CircleShape)
                                    .then(if (open && set) Modifier.background(neonColor(lights[i])) else Modifier.border(2.dp, Rc.LineStrong, androidx.compose.foundation.shape.CircleShape)),
                                contentAlignment = Alignment.Center,
                            ) { if (!open) ru.ilyaushenin.nnracing.ui.Icon24(Ic.Lock, Rc.Muted, 14.dp) else if (!set) ru.ilyaushenin.nnracing.ui.Icon24(Ic.Plus, Rc.Muted, 14.dp) }
                            Txt(if (need == 0) "всегда" else "$need ${ru.ilyaushenin.nnracing.ui.plural(need, "заезд", "заезда", "заездов")}", color = if (open) Rc.Text else Rc.Muted, size = 10.sp, align = androidx.compose.ui.text.style.TextAlign.Center, maxLines = 2)
                        }
                    }
                }
                Txt(if (lights.isEmpty()) "Свечение выключено. Выберите цвет, чтобы включить его снова." else "Выберите огонь и цвет для него. Огни идут слева направо.", color = Rc.Muted, size = 12.sp, modifier = Modifier.padding(top = 8.dp, bottom = 8.dp))
                androidx.compose.foundation.layout.FlowRow(modifier = Modifier.then(if (user.avatarUrl == null) Modifier.alpha(0.35f) else Modifier), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // первый круг — «выключить»: убирает этот огонь и все правее; выбран, когда огонь не задан
                    val off = slot >= lights.size
                    Box(
                        Modifier.size(40.dp).clip(androidx.compose.foundation.shape.CircleShape).background(Rc.Surface2)
                            .border(if (off) 3.dp else 1.dp, if (off) androidx.compose.ui.graphics.Color.White else Rc.Line, androidx.compose.foundation.shape.CircleShape)
                            .clickable(enabled = user.avatarUrl != null && slot < user.neonSlots) { while (lights.size > slot) lights.removeAt(lights.lastIndex) },
                        contentAlignment = Alignment.Center,
                    ) { ru.ilyaushenin.nnracing.ui.Icon24(Ic.Ban, Rc.Muted, 22.dp, strokeWidth = 1.8f) }
                    NEON_COLORS.forEach { (key, color) ->
                        val on = lights.getOrNull(slot) == key
                        androidx.compose.foundation.layout.Box(
                            Modifier.size(40.dp).clip(androidx.compose.foundation.shape.CircleShape).background(color)
                                .then(if (on) Modifier.border(3.dp, androidx.compose.ui.graphics.Color.White, androidx.compose.foundation.shape.CircleShape) else Modifier)
                                .clickable(enabled = user.avatarUrl != null && slot < user.neonSlots) {
                                    // огни должны идти подряд: заполняем недостающие предыдущие тем же цветом
                                    while (lights.size <= slot) lights.add(key)
                                    lights[slot] = key
                                },
                            contentAlignment = Alignment.Center,
                        ) { if (on) ru.ilyaushenin.nnracing.ui.Icon24(Ic.Check, androidx.compose.ui.graphics.Color(0xFF111111), 20.dp, strokeWidth = 3f) }
                    }
                }
            }
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Rc.Surface2).border(1.dp, Rc.Line, RoundedCornerShape(16.dp)).padding(14.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Column(Modifier.weight(1f)) {
                    Txt("Открытый профиль", weight = FontWeight.Bold)
                    Txt("Заезды и награды видны всем. В закрытом профиле их видят только друзья.", color = Rc.Muted, size = 14.sp)
                }
                Switch(
                    checked = isPublic, onCheckedChange = { isPublic = it },
                    colors = SwitchDefaults.colors(checkedTrackColor = Rc.Accent, checkedThumbColor = androidx.compose.ui.graphics.Color.White, uncheckedTrackColor = Rc.Surface3, uncheckedThumbColor = androidx.compose.ui.graphics.Color.White, uncheckedBorderColor = Rc.Surface3),
                )
            }
            if (ru.ilyaushenin.nnracing.ui.PushBridge.supported) {
                // уведомления: включены, если разрешены в системе и пользователь не выключил их здесь
                var pushOn by remember { mutableStateOf(app.api.pushEnabled && ru.ilyaushenin.nnracing.ui.PushBridge.permissionGranted()) }
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Rc.Surface2).border(1.dp, Rc.Line, RoundedCornerShape(16.dp)).padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Column(Modifier.weight(1f)) {
                        Txt("Уведомления", weight = FontWeight.Bold)
                        Txt(
                            if (!ru.ilyaushenin.nnracing.ui.PushBridge.permissionGranted()) "Выключены в настройках телефона. Нажмите, чтобы открыть их и включить."
                            else "О новых сообщениях, приглашениях на заезды и заявках в друзья.",
                            color = Rc.Muted, size = 14.sp,
                        )
                    }
                    Switch(
                        checked = pushOn,
                        onCheckedChange = { want ->
                            if (!want) {
                                pushOn = false; app.api.pushEnabled = false
                                app.act { app.api.unregisterPush(); app.showToast("Уведомления выключены") }
                            } else if (ru.ilyaushenin.nnracing.ui.PushBridge.permissionGranted()) {
                                pushOn = true; app.api.pushEnabled = true
                                ru.ilyaushenin.nnracing.ui.PushBridge.enable { token -> app.act { app.api.registerPush(token); app.showToast("Уведомления включены") } }
                            } else {
                                // разрешение запрещено в системе: сначала пробуем показать системный запрос, если он уже не показывается — открываем настройки
                                app.api.pushEnabled = true
                                ru.ilyaushenin.nnracing.ui.PushBridge.enable(onDenied = { ru.ilyaushenin.nnracing.ui.PushBridge.openSettings() }) { token ->
                                    pushOn = true; app.act { app.api.registerPush(token); app.showToast("Уведомления включены") }
                                }
                            }
                        },
                        colors = SwitchDefaults.colors(checkedTrackColor = Rc.Accent, checkedThumbColor = androidx.compose.ui.graphics.Color.White, uncheckedTrackColor = Rc.Surface3, uncheckedThumbColor = androidx.compose.ui.graphics.Color.White, uncheckedBorderColor = Rc.Surface3),
                    )
                }
            }
            if (error.isNotEmpty()) Txt(error, color = Rc.Bad, size = 14.sp)
            AppButton("Сохранить", {
                app.act(onError = { error = it }) { app.user = app.api.updateProfile(name, isPublic, if (user.avatarUrl != null) lights.toList() else null, badge, sendBadge = true); app.showToast("Сохранено"); onClose() }
            }, Modifier.fillMaxWidth())
            AppButton("Выйти", { onClose(); app.signOut() }, Modifier.fillMaxWidth(), BtnKind.Danger, icon = Ic.Logout)
        }
    }
}

