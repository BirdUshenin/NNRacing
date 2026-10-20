package ru.ilyaushenin.nnracing.screens

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
        Row(Modifier.fillMaxWidth().padding(bottom = 18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Avatar(user.name, user.avatarUrl, 76.dp)
            Column(Modifier.weight(1f)) {
                Txt(user.name, size = 24.sp, weight = FontWeight.ExtraBold, maxLines = 1)
                Txt("@${user.username} · ${if (user.isPublic) "открытый" else "закрытый"}", color = Rc.Muted)
            }
            IconBtn(Ic.Edit, { settings = true })
        }
        Segmented(listOf("Заезды", "Друзья", "Статистика"), tab, { tab = it }, Modifier.padding(bottom = 18.dp))
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
@Composable
private fun ProfileSheet(app: AppState, onClose: () -> Unit) {
    val user = app.user ?: return
    var name by remember { mutableStateOf(user.name) }
    var isPublic by remember { mutableStateOf(user.isPublic) }
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
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Rc.Surface2).border(1.dp, Rc.Line, RoundedCornerShape(16.dp)).padding(14.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Column(Modifier.weight(1f)) {
                    Txt("Открытый профиль", weight = FontWeight.Bold)
                    Txt("Заезды видны всем. В закрытом профиле заезды видят только друзья; найти вас по нику можно в любом случае.", color = Rc.Muted, size = 14.sp)
                }
                Switch(
                    checked = isPublic, onCheckedChange = { isPublic = it },
                    colors = SwitchDefaults.colors(checkedTrackColor = Rc.Accent, checkedThumbColor = androidx.compose.ui.graphics.Color.White, uncheckedTrackColor = Rc.Surface3, uncheckedThumbColor = androidx.compose.ui.graphics.Color.White, uncheckedBorderColor = Rc.Surface3),
                )
            }
            if (error.isNotEmpty()) Txt(error, color = Rc.Bad, size = 14.sp)
            AppButton("Сохранить", {
                app.act(onError = { error = it }) { app.user = app.api.updateProfile(name, isPublic); app.showToast("Сохранено"); onClose() }
            }, Modifier.fillMaxWidth())
            AppButton("Выйти", { onClose(); app.signOut() }, Modifier.fillMaxWidth(), BtnKind.Danger, icon = Ic.Logout)
        }
    }
}

