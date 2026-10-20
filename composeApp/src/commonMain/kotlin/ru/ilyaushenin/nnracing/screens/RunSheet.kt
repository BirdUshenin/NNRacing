package ru.ilyaushenin.nnracing.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import ru.ilyaushenin.nnracing.AppState
import ru.ilyaushenin.nnracing.Route
import ru.ilyaushenin.nnracing.data.Track
import ru.ilyaushenin.nnracing.data.UserBrief
import ru.ilyaushenin.nnracing.data.obj
import ru.ilyaushenin.nnracing.ui.AppButton
import ru.ilyaushenin.nnracing.ui.AppField
import ru.ilyaushenin.nnracing.ui.Avatar
import ru.ilyaushenin.nnracing.ui.BtnKind
import ru.ilyaushenin.nnracing.ui.Card
import ru.ilyaushenin.nnracing.ui.DatePickerSheet
import ru.ilyaushenin.nnracing.ui.FieldLabel
import ru.ilyaushenin.nnracing.ui.Ic
import ru.ilyaushenin.nnracing.ui.Icon24
import ru.ilyaushenin.nnracing.ui.IconBtn
import ru.ilyaushenin.nnracing.ui.LapInput
import ru.ilyaushenin.nnracing.ui.LapTimeField
import ru.ilyaushenin.nnracing.ui.PickChip
import ru.ilyaushenin.nnracing.ui.PickerField
import ru.ilyaushenin.nnracing.ui.Rc
import ru.ilyaushenin.nnracing.ui.Segmented
import ru.ilyaushenin.nnracing.ui.Sheet
import ru.ilyaushenin.nnracing.ui.TimePickerSheet
import ru.ilyaushenin.nnracing.ui.Txt
import ru.ilyaushenin.nnracing.ui.fmtDate
import ru.ilyaushenin.nnracing.ui.fmtLap
import ru.ilyaushenin.nnracing.ui.parseLap
import ru.ilyaushenin.nnracing.ui.todayIso

/** Предзаполнение формы заезда: трасса, вариант, время старта, друзья и запланированный заезд, который закрывается. */
data class RunPrefill(
    val trackId: String? = null,
    val variant: String? = null,
    val startsAt: Long? = null,
    val memberIds: List<Int> = emptyList(),
    val plannedId: Int? = null,
)

/** Один участник заезда: я, друг или гость, и его круги. */
@Stable
class Participant(val user: UserBrief?, guestName: String = "") {
    var name by mutableStateOf(guestName)
    var kart by mutableStateOf("")
    var position by mutableStateOf("")
    val laps = mutableStateListOf(LapInput())
    var bestManual by mutableStateOf<Int?>(null)

    /** Индекс лучшего круга среди заполненных (в списке, где пустые не считаются). */
    fun bestAmongFilled(): Int {
        val filled = laps.mapNotNull { it.toMs() }
        val manualMs = bestManual?.let { laps.getOrNull(it)?.toMs() }
        return if (manualMs != null) filled.indexOf(manualMs).coerceAtLeast(0) else filled.indices.minByOrNull { filled[it] } ?: 0
    }

    fun bestLapIndex(): Int? {
        val manual = bestManual?.takeIf { laps.getOrNull(it)?.toMs() != null }
        if (manual != null) return manual
        return laps.indices.filter { laps[it].toMs() != null }.minByOrNull { laps[it].toMs()!! }
    }
}

private val CONDITIONS = listOf("Ясно", "Облачно", "Пасмурно", "Дождь", "Мокрая трасса")

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RunSheet(app: AppState, tracks: List<Track>, onClose: () -> Unit, prefill: RunPrefill = RunPrefill()) {
    val me = app.user ?: return
    val meBrief = remember { UserBrief(me.id, me.username, me.name, me.avatarUrl) }
    var trackId by remember { mutableStateOf(prefill.trackId) }
    var variant by remember { mutableStateOf(prefill.variant) }
    val startLocal = remember { prefill.startsAt?.let { Instant.fromEpochSeconds(it).toLocalDateTime(TimeZone.currentSystemDefault()) } }
    var date by remember { mutableStateOf(startLocal?.date?.toString() ?: todayIso()) }
    var time by remember { mutableStateOf(startLocal?.let { it.hour to it.minute }) }
    var condition by remember { mutableStateOf<String?>(null) }
    var air by remember { mutableStateOf("") }
    var road by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var friends by remember { mutableStateOf<List<UserBrief>>(emptyList()) }
    val parts = remember { mutableStateListOf(Participant(meBrief)) }
    var dateDialog by remember { mutableStateOf(false) }
    var timeDialog by remember { mutableStateOf(false) }
    var addSheet by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val list = runCatching { app.api.friends().friends }.getOrDefault(emptyList())
        friends = list
        for (id in prefill.memberIds) list.find { it.id == id }?.let { f -> if (parts.none { it.user?.id == f.id }) parts.add(Participant(f)) }
    }
    val track = tracks.find { it.id == trackId }
    LaunchedEffect(trackId) {
        val vs = track?.variants.orEmpty()
        if (vs.isNotEmpty() && vs.none { it.id == variant }) variant = vs.first().id
        if (vs.isEmpty()) variant = null
    }

    fun save() {
        error = ""
        val tid = trackId ?: run { error = "Выберите трассу"; return }
        val results = ArrayList<JsonObject>()
        for (p in parts) {
            val label = if (p.user?.id == me.id) "Мои круги" else p.user?.name ?: p.name.ifBlank { "Гость" }
            if (p.user == null && p.name.isBlank()) { error = "Укажите имя гостя"; return }
            val filled = p.laps.filter { !it.isEmpty }
            if (filled.isEmpty()) { error = "$label: добавьте хотя бы один круг"; return }
            for ((i, l) in p.laps.withIndex()) l.error()?.let { error = "$label, круг ${i + 1}: $it"; return }
            val ms = filled.map { it.toMs()!! }
            results.add(buildJsonObject {
                if (p.user != null) put("userId", p.user.id) else put("guestName", p.name.trim())
                if (p.kart.isNotBlank()) put("kart", p.kart)
                p.position.toIntOrNull()?.let { put("position", it) }
                put("laps", JsonArray(ms.map { kotlinx.serialization.json.JsonPrimitive(it) }))
                put("bestIndex", p.bestAmongFilled())
            })
        }
        val body = obj(
            "trackId" to tid, "variant" to variant, "date" to date,
            "time" to time?.let { "${it.first.toString().padStart(2, '0')}:${it.second.toString().padStart(2, '0')}" },
            "weather" to obj("condition" to condition, "airC" to air.replace(',', '.').toDoubleOrNull(), "trackC" to road.replace(',', '.').toDoubleOrNull()),
            "results" to JsonArray(results), "notes" to notes, "plannedId" to prefill.plannedId,
        )
        busy = true
        app.act(onError = { error = it; busy = false }) {
            val run = app.api.createRun(body)
            val mine = run.results.find { it.userId == me.id }
            app.showToast(if (mine != null) "Заезд сохранён · лучший ${fmtLap(mine.bestMs)}" else "Заезд сохранён")
            onClose(); app.refresh(); app.push(Route.Run(run.id))
        }
    }

    Sheet("Новый заезд", onClose) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Column {
                FieldLabel("Трасса")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    tracks.forEach { t -> PickChip(t.name, t.id == trackId, { trackId = t.id }) }
                }
            }
            track?.variants?.takeIf { it.size > 1 }?.let { vs ->
                Column { FieldLabel("Вариант трассы"); Segmented(vs.map { it.name }, vs.indexOfFirst { it.id == variant }.coerceAtLeast(0), { variant = vs[it].id }) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(Modifier.weight(1f)) { FieldLabel("Дата"); PickerField(fmtDate(date), { dateDialog = true }) }
                Column(Modifier.weight(1f)) {
                    FieldLabel("Время старта")
                    PickerField(time?.let { "${it.first.toString().padStart(2, '0')}:${it.second.toString().padStart(2, '0')}" } ?: "не указано", { timeDialog = true }, placeholder = time == null)
                }
            }
            Column {
                FieldLabel("Погода")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CONDITIONS.forEach { c -> PickChip(c, c == condition, { condition = if (condition == c) null else c }) }
                }
                Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column(Modifier.weight(1f)) { FieldLabel("Воздух, °C"); AppField(air, { air = it }, placeholder = "12", keyboard = KeyboardType.Decimal) }
                    Column(Modifier.weight(1f)) { FieldLabel("Трасса, °C"); AppField(road, { road = it }, placeholder = "15", keyboard = KeyboardType.Decimal) }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                FieldLabel("Участники")
                parts.forEach { p -> ParticipantCard(p, meId = me.id, canRemove = parts.size > 1, onRemove = { parts.remove(p) }) }
                AppButton("Добавить участника", { addSheet = true }, Modifier.fillMaxWidth(), BtnKind.Ghost, icon = Ic.Users)
            }
            Column { FieldLabel("Заметка"); AppField(notes, { notes = it }, placeholder = "Настроение, шины, что запомнилось…", singleLine = false, height = 84.dp, maxLength = 300) }
            if (error.isNotEmpty()) Txt(error, color = Rc.Bad, size = 14.sp)
            AppButton("Сохранить заезд", ::save, Modifier.fillMaxWidth(), enabled = !busy)
        }
    }
    if (dateDialog) DatePickerSheet(date, { date = it }, { dateDialog = false })
    if (timeDialog) TimePickerSheet(time?.first ?: 12, time?.second ?: 0, { h, m -> time = h to m }, { timeDialog = false })
    if (addSheet) AddParticipantSheet(
        free = friends.filter { f -> parts.none { it.user?.id == f.id } },
        onFriend = { parts.add(Participant(it)); addSheet = false },
        onGuest = { parts.add(Participant(null, it)); addSheet = false },
        onClose = { addSheet = false },
    )
}

@Composable
private fun AddParticipantSheet(free: List<UserBrief>, onFriend: (UserBrief) -> Unit, onGuest: (String) -> Unit, onClose: () -> Unit) {
    var guest by remember { mutableStateOf("") }
    Sheet("Добавить участника", onClose) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (free.isEmpty()) Txt("Друзей для отметки нет — добавьте их во вкладке «Друзья» или введите данные вручную.", color = Rc.Muted)
            else {
                FieldLabel("Друзья")
                free.forEach { f ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Rc.Surface2).border(1.dp, Rc.Line, RoundedCornerShape(16.dp))
                            .clickable { onFriend(f) }.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) { Avatar(f.name, f.avatarUrl, 40.dp); Txt(f.name, weight = FontWeight.Bold, modifier = Modifier.weight(1f)); Icon24(Ic.Plus, Rc.Muted, 18.dp) }
                }
            }
            FieldLabel("Ввести вручную")
            AppField(guest, { guest = it }, placeholder = "Имя или номер карта", maxLength = 40)
            AppButton("Добавить гостя", { if (guest.isNotBlank()) onGuest(guest.trim()) }, Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun ParticipantCard(p: Participant, meId: Int, canRemove: Boolean, onRemove: () -> Unit) {
    Card(padding = 14.dp) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Avatar(p.user?.name ?: p.name, p.user?.avatarUrl, 44.dp, guest = p.user == null)
            Column(Modifier.weight(1f)) {
                if (p.user == null) AppField(p.name, { p.name = it }, placeholder = "Имя гостя", maxLength = 40, height = 44.dp)
                else {
                    Txt(if (p.user.id == meId) "Я" else p.user.name, weight = FontWeight.ExtraBold)
                    Txt("@${p.user.username}", color = Rc.Muted, size = 12.sp)
                }
            }
            if (canRemove) IconBtn(Ic.X, onRemove)
        }
        Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AppField(p.kart, { p.kart = it.filter { c -> c.isLetterOrDigit() } }, Modifier.weight(1f), placeholder = "Карт №", keyboard = KeyboardType.Number, maxLength = 6, height = 44.dp)
            AppField(p.position, { p.position = it.filter { c -> c.isDigit() } }, Modifier.weight(1f), placeholder = "Место", keyboard = KeyboardType.Number, maxLength = 2, height = 44.dp)
        }
        LapsEditor(p)
    }
}

@Composable
private fun LapsEditor(p: Participant) {
    var bulk by remember { mutableStateOf(false) }
    var bulkText by remember { mutableStateOf("") }
    Column(Modifier.padding(top = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FieldLabel("Круги · звезда отмечает лучший")
        val best = p.bestLapIndex()
        p.laps.forEachIndexed { i, lap ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Txt("${i + 1}", color = Rc.Muted, weight = FontWeight.Bold, mono = true, modifier = Modifier.width(24.dp), size = 14.sp)
                Box(Modifier.weight(1f)) { LapTimeField(lap, onDone = { }) }
                Box(
                    Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(if (i == best) Rc.Best.copy(alpha = 0.16f) else Rc.Surface)
                        .border(1.dp, if (i == best) Rc.Best.copy(alpha = 0.55f) else Rc.Line, RoundedCornerShape(14.dp))
                        .clickable { if (lap.toMs() != null) p.bestManual = i },
                    contentAlignment = Alignment.Center,
                ) { Icon24(Ic.Star, if (i == best) Rc.Best else Rc.Muted, 20.dp, filled = i == best) }
                Box(Modifier.size(36.dp).clickable {
                    p.laps.removeAt(i); if (p.laps.isEmpty()) p.laps.add(LapInput())
                    p.bestManual = null
                }, contentAlignment = Alignment.Center) { Icon24(Ic.X, Rc.Muted, 18.dp) }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AppButton("Круг", { p.laps.add(LapInput()) }, Modifier.weight(1f), BtnKind.Ghost, small = true, icon = Ic.Plus)
            AppButton(if (bulk) "Применить список" else "Вставить списком", {
                if (!bulk) { bulk = true; return@AppButton }
                val parsed = bulkText.lines().map { it.trim() }.filter { it.isNotEmpty() }.mapNotNull { parseLap(it) }
                if (parsed.isNotEmpty()) { p.laps.clear(); parsed.forEach { p.laps.add(LapInput.fromMs(it)) }; p.bestManual = null }
                bulk = false; bulkText = ""
            }, Modifier.weight(1f), BtnKind.Ghost, small = true)
        }
        if (bulk) AppField(bulkText, { bulkText = it }, placeholder = "Каждый круг с новой строки:\n1:05.43\n1:04.98", singleLine = false, height = 110.dp)
    }
}

