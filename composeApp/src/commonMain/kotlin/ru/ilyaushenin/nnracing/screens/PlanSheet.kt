package ru.ilyaushenin.nnracing.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import ru.ilyaushenin.nnracing.AppState
import ru.ilyaushenin.nnracing.data.Track
import ru.ilyaushenin.nnracing.data.UserBrief
import ru.ilyaushenin.nnracing.data.obj
import ru.ilyaushenin.nnracing.ui.AppButton
import ru.ilyaushenin.nnracing.ui.AppField
import ru.ilyaushenin.nnracing.ui.Avatar
import ru.ilyaushenin.nnracing.ui.DatePickerSheet
import ru.ilyaushenin.nnracing.ui.FieldLabel
import ru.ilyaushenin.nnracing.ui.Ic
import ru.ilyaushenin.nnracing.ui.Icon24
import ru.ilyaushenin.nnracing.ui.PickChip
import ru.ilyaushenin.nnracing.ui.PickerField
import ru.ilyaushenin.nnracing.ui.Rc
import ru.ilyaushenin.nnracing.ui.Segmented
import ru.ilyaushenin.nnracing.ui.Sheet
import ru.ilyaushenin.nnracing.ui.TimePickerSheet
import ru.ilyaushenin.nnracing.ui.Txt
import ru.ilyaushenin.nnracing.ui.fmtDate
import ru.ilyaushenin.nnracing.ui.toEpochSeconds
import ru.ilyaushenin.nnracing.ui.tomorrowIso

/** Запланировать заезд: трасса, вариант, дата и время, кого пригласить. Из чата ([chatId]) приглашаются все его участники. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlanSheet(app: AppState, tracks: List<Track>, onClose: () -> Unit, chatId: Int? = null, onSaved: () -> Unit = {}) {
    var trackId by remember { mutableStateOf<String?>(null) }
    var variant by remember { mutableStateOf<String?>(null) }
    var date by remember { mutableStateOf(tomorrowIso()) }
    var time by remember { mutableStateOf(18 to 0) }
    var notes by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var friends by remember { mutableStateOf<List<UserBrief>>(emptyList()) }
    val invited = remember { mutableStateListOf<Int>() }
    var dateDialog by remember { mutableStateOf(false) }
    var timeDialog by remember { mutableStateOf(false) }
    val track = tracks.find { it.id == trackId }

    LaunchedEffect(Unit) { if (chatId == null) friends = runCatching { app.api.friends().friends }.getOrDefault(emptyList()) }
    LaunchedEffect(trackId) {
        val vs = track?.variants.orEmpty()
        if (vs.isNotEmpty() && vs.none { it.id == variant }) variant = vs.first().id
        if (vs.isEmpty()) variant = null
    }

    fun save() {
        error = ""
        val tid = trackId ?: run { error = "Выберите трассу"; return }
        val body = obj(
            "trackId" to tid, "variant" to variant, "startsAt" to toEpochSeconds(date, time.first, time.second), "notes" to notes,
            "chatId" to chatId, "memberIds" to if (chatId == null) JsonArray(invited.map { JsonPrimitive(it) }) else null,
        )
        busy = true
        app.act(onError = { error = it; busy = false }) {
            app.api.createPlanned(body)
            app.showToast("Заезд запланирован"); onClose(); onSaved(); app.refresh()
        }
    }

    Sheet("Запланировать заезд", onClose) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Column {
                FieldLabel("Трасса")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    tracks.forEach { t -> PickChip(t.name, t.id == trackId, { trackId = t.id }) }
                }
            }
            track?.variants?.takeIf { it.size > 1 }?.let { vs ->
                Segmented(vs.map { it.name }, vs.indexOfFirst { it.id == variant }.coerceAtLeast(0), { variant = vs[it].id })
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(Modifier.weight(1f)) { FieldLabel("Дата"); PickerField(fmtDate(date), { dateDialog = true }) }
                Column(Modifier.weight(1f)) { FieldLabel("Время"); PickerField("${time.first.toString().padStart(2, '0')}:${time.second.toString().padStart(2, '0')}", { timeDialog = true }) }
            }
            Column { FieldLabel("Заметка"); AppField(notes, { notes = it }, placeholder = "Необязательно", maxLength = 300) }
            Column {
                FieldLabel("Кого пригласить")
                if (chatId != null) Txt("Приглашение получат все участники чата, а едущие подтвердят участие.", color = Rc.Muted)
                else if (friends.isEmpty()) Txt("Пригласить можно друзей — добавьте их во вкладке «Друзья».", color = Rc.Muted)
                else Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    friends.forEach { f ->
                        val on = f.id in invited
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(if (on) Rc.Accent.copy(alpha = 0.10f) else Rc.Surface2)
                                .border(1.dp, if (on) Rc.Accent.copy(alpha = 0.7f) else Rc.Line, RoundedCornerShape(16.dp))
                                .clickable { if (on) invited.remove(f.id) else invited.add(f.id) }.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Avatar(f.name, f.avatarUrl, 40.dp)
                            Txt(f.name, weight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            if (on) Icon24(Ic.Check, Rc.Accent, 18.dp)
                        }
                    }
                }
            }
            if (error.isNotEmpty()) Txt(error, color = Rc.Bad, size = 14.sp)
            AppButton("Запланировать", ::save, Modifier.fillMaxWidth(), enabled = !busy)
        }
    }
    if (dateDialog) DatePickerSheet(date, { date = it }, { dateDialog = false })
    if (timeDialog) TimePickerSheet(time.first, time.second, { h, m -> time = h to m }, { timeDialog = false })
}
