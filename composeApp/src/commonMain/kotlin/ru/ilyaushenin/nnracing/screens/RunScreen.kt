package ru.ilyaushenin.nnracing.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.ilyaushenin.nnracing.AppState
import ru.ilyaushenin.nnracing.Route
import ru.ilyaushenin.nnracing.data.Run
import ru.ilyaushenin.nnracing.data.RunResult
import ru.ilyaushenin.nnracing.data.Track
import ru.ilyaushenin.nnracing.data.Weather
import ru.ilyaushenin.nnracing.ui.AppButton
import ru.ilyaushenin.nnracing.ui.Avatar
import ru.ilyaushenin.nnracing.ui.BackLink
import ru.ilyaushenin.nnracing.ui.BtnKind
import ru.ilyaushenin.nnracing.ui.Card
import ru.ilyaushenin.nnracing.ui.Chip
import ru.ilyaushenin.nnracing.ui.ConfirmSheet
import ru.ilyaushenin.nnracing.ui.Eyebrow
import ru.ilyaushenin.nnracing.ui.Ic
import ru.ilyaushenin.nnracing.ui.Icon24
import ru.ilyaushenin.nnracing.ui.LoadedContent
import ru.ilyaushenin.nnracing.ui.Page
import ru.ilyaushenin.nnracing.ui.PageTitle
import ru.ilyaushenin.nnracing.ui.Rc
import ru.ilyaushenin.nnracing.ui.Txt
import ru.ilyaushenin.nnracing.ui.clickableNoRipple
import ru.ilyaushenin.nnracing.ui.fmtDate
import ru.ilyaushenin.nnracing.ui.fmtDelta
import ru.ilyaushenin.nnracing.ui.fmtLap
import ru.ilyaushenin.nnracing.ui.load
import ru.ilyaushenin.nnracing.ui.par
import ru.ilyaushenin.nnracing.ui.plural

private data class RunData(val run: Run, val tracks: List<Track>)

@Composable
fun LapRow(n: Int, ms: Int, bestMs: Int, isBest: Boolean) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        Modifier.fillMaxWidth().clip(shape)
            .background(if (isBest) Rc.Best.copy(alpha = 0.13f) else Color(0x08FFFFFF))
            .then(if (isBest) Modifier.border(1.dp, Rc.Best.copy(alpha = 0.35f), shape) else Modifier)
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Txt(n.toString(), color = Rc.Muted, size = 13.sp, weight = FontWeight.Bold, mono = true, modifier = Modifier.width(34.dp))
        Txt(fmtLap(ms), color = if (isBest) Rc.Best else Rc.Text, size = 17.sp, weight = FontWeight.Bold, mono = true, modifier = Modifier.weight(1f))
        Txt(if (isBest) "лучший" else fmtDelta(ms - bestMs), color = Rc.Muted, size = 13.sp, mono = true)
        Box(Modifier.width(26.dp), contentAlignment = Alignment.CenterEnd) {
            if (isBest) Icon24(Ic.Star, Rc.Best, 16.dp, filled = true)
        }
    }
}

@Composable
fun RunScreen(app: AppState, route: Route.Run) {
    val me = app.user ?: return
    val data = load(app, route.id, cacheKey = "run:${route.id}") { par({ app.api.run(route.id) }, { app.api.tracks() }).let { (r, t) -> RunData(r, t) } }
    var confirmDelete by remember { mutableStateOf(false) }

    Page {
        BackLink("Назад") { app.pop() }
        LoadedContent(data) { d ->
            val run = d.run
            val track = d.tracks.find { it.id == run.trackId }
            val variant = track?.variants?.find { it.id == run.variant }
            val results = run.results
            val bestOverall = results.minOfOrNull { it.bestMs } ?: 0
            val mine = results.find { it.userId == me.id }
            val others = results.filter { it !== mine && it.userId != null }

            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Eyebrow(listOfNotNull(fmtDate(run.date), run.time).joinToString(" · "))
                    PageTitle(track?.name ?: "Заезд")
                    if (variant != null) Row { Chip(variant.name) }
                }
                run.weather?.let { WeatherRow(it) }
                TimingTable(app, results, mine, bestOverall)
                if (mine != null) FullResultCard(app, mine, "Я")
                others.forEach { FriendCard(app, it) }
                if (run.notes.isNotBlank()) Card { Txt(run.notes, color = Rc.Muted) }
                if (run.creatorId == me.id) AppButton("Удалить заезд", { confirmDelete = true }, Modifier.fillMaxWidth(), BtnKind.Danger, icon = Ic.Trash)
            }
            if (confirmDelete) ConfirmSheet(
                "Удалить заезд?", "Заезд пропадёт у всех участников. Это нельзя отменить.", "Удалить",
                onConfirm = { confirmDelete = false; app.act { app.api.deleteRun(run.id); app.showToast("Заезд удалён"); app.pop(); app.refresh() } },
                onClose = { confirmDelete = false },
            )
        }
    }
}

@Composable
private fun WeatherRow(w: Weather) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        w.condition?.let { WeatherCell("Погода", it) }
        w.airC?.let { WeatherCell("Воздух", "${trim(it)}°C") }
        w.trackC?.let { WeatherCell("Трасса", "${trim(it)}°C") }
    }
}

@Composable
private fun WeatherCell(label: String, value: String) {
    val shape = RoundedCornerShape(16.dp)
    Column(Modifier.clip(shape).background(Rc.Surface2).border(1.dp, Rc.Line, shape).padding(horizontal = 14.dp, vertical = 10.dp)) {
        Txt(label.uppercase(), color = Rc.Muted, size = 11.sp, weight = FontWeight.Bold, letterSpacing = 1.sp)
        Txt(value, size = 17.sp, weight = FontWeight.Bold)
    }
}

/** Общая таблица заезда: позиция, пилот, лучший круг и отрыв от лидера. */
@Composable
private fun TimingTable(app: AppState, results: List<RunResult>, mine: RunResult?, bestOverall: Int) {
    Card(padding = 0.dp) {
        TimingRow("ПОЗ", "ПИЛОТ", "ЛУЧШИЙ", "ОТРЫВ", header = true)
        results.forEach { r ->
            Box(Modifier.fillMaxWidth().height(1.dp).background(Rc.Line))
            TimingRow(
                r.position?.toString() ?: "–", (r.kart?.let { "$it  " } ?: "") + r.name, fmtLap(r.bestMs),
                if (r.bestMs == bestOverall) "лидер" else fmtDelta(r.bestMs - bestOverall),
                highlight = r === mine, purple = r.bestMs == bestOverall, position = r.position,
                onClick = r.userId?.let { id -> { app.push(Route.UserProfile(id)) } },
            )
        }
    }
}

@Composable
private fun TimingRow(pos: String, name: String, best: String, gap: String, header: Boolean = false, highlight: Boolean = false, purple: Boolean = false, position: Int? = null, onClick: (() -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().background(if (highlight) Rc.Accent.copy(alpha = 0.10f) else Color.Transparent)
            .then(if (onClick != null) Modifier.clickableNoRipple(onClick) else Modifier)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val muted = if (header) Rc.Muted else Rc.Text
        val size = if (header) 11.sp else 14.sp
        Txt(pos, Modifier.width(34.dp), color = Rc.medal(position) ?: Rc.Muted, size = size, weight = FontWeight.ExtraBold, mono = true)
        Txt(name, Modifier.weight(1f), color = muted, size = size, weight = if (header) FontWeight.Bold else FontWeight.Bold, maxLines = 1)
        Txt(best, Modifier.width(86.dp), color = if (purple) Rc.Best else muted, size = size, weight = FontWeight.ExtraBold, mono = true, align = TextAlign.End)
        Txt(gap, Modifier.width(66.dp), color = Rc.Muted, size = if (header) 11.sp else 12.sp, mono = true, align = TextAlign.End)
    }
}

@Composable
private fun ResultHeader(app: AppState, r: RunResult, title: String? = null) {
    // у зарегистрированного пилота (кроме меня) нажатие на аватарку и имя открывает его профиль
    val open = r.userId?.takeIf { it != app.user?.id }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Row(
            Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).then(if (open != null) Modifier.clickable { app.push(Route.UserProfile(open)) } else Modifier),
            verticalAlignment = Alignment.CenterVertically,
        ) {
        r.user?.let { Avatar(it.name, it.avatarUrl, 44.dp); Box(Modifier.width(10.dp)) }
        Column(Modifier.weight(1f)) {
            Txt(title ?: r.name, weight = FontWeight.ExtraBold, maxLines = 1)
            Txt(listOfNotNull(r.kart?.let { "карт $it" }, "${r.lapCount} ${plural(r.lapCount, "круг", "круга", "кругов")}").joinToString(" · "), color = Rc.Muted, size = 12.sp)
        }
        }
        if (r.position != null) Txt("P${r.position}", color = Rc.medal(r.position) ?: Rc.Text, size = 26.sp, weight = FontWeight.ExtraBold, mono = true)
    }
}

@Composable
private fun FullResultCard(app: AppState, r: RunResult, title: String) {
    Card {
        ResultHeader(app, r, title)
        Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            r.laps.forEachIndexed { i, lap -> LapRow(i + 1, lap, r.bestMs, i == r.bestIndex) }
        }
        Row(Modifier.padding(top = 12.dp)) { Chip("среднее ${fmtLap(r.avgMs)}") }
    }
}

/** Друг: три лучших круга, лучший и позиция. */
@Composable
private fun FriendCard(app: AppState, r: RunResult) {
    Card {
        ResultHeader(app, r)
        val top = r.laps.withIndex().sortedBy { it.value }.take(3)
        Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            top.forEachIndexed { k, (i, lap) -> LapRow(k + 1, lap, r.bestMs, i == r.bestIndex) }
        }
    }
}
