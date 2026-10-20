package ru.ilyaushenin.nnracing.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.ilyaushenin.nnracing.AppState
import ru.ilyaushenin.nnracing.Route
import ru.ilyaushenin.nnracing.data.Run
import ru.ilyaushenin.nnracing.data.Track
import ru.ilyaushenin.nnracing.ui.AppButton
import ru.ilyaushenin.nnracing.ui.BackLink
import ru.ilyaushenin.nnracing.ui.BestChip
import ru.ilyaushenin.nnracing.ui.Card
import ru.ilyaushenin.nnracing.ui.Chip
import ru.ilyaushenin.nnracing.ui.Empty
import ru.ilyaushenin.nnracing.ui.Eyebrow
import ru.ilyaushenin.nnracing.ui.Ic
import ru.ilyaushenin.nnracing.ui.Icon24
import ru.ilyaushenin.nnracing.ui.LoadedContent
import ru.ilyaushenin.nnracing.ui.Page
import ru.ilyaushenin.nnracing.ui.PageTitle
import ru.ilyaushenin.nnracing.ui.Rc
import ru.ilyaushenin.nnracing.ui.SectionTitle
import ru.ilyaushenin.nnracing.ui.Segmented
import ru.ilyaushenin.nnracing.ui.TrackMap
import ru.ilyaushenin.nnracing.ui.Txt
import ru.ilyaushenin.nnracing.ui.fmtDate
import ru.ilyaushenin.nnracing.ui.fmtLap
import ru.ilyaushenin.nnracing.ui.load
import ru.ilyaushenin.nnracing.ui.par
import ru.ilyaushenin.nnracing.ui.plural

private data class TrackData(val tracks: List<Track>, val runs: List<Run>)

@Composable
fun TrackScreen(app: AppState, route: Route.Track) {
    val me = app.user ?: return
    val data = load(app, route.id, cacheKey = "track:${route.id}") { par({ app.api.tracks() }, { app.api.runs(route.id) }).let { (t, r) -> TrackData(t, r) } }
    var variantId by remember(route.id) { mutableStateOf(route.variant) }
    var runSheet by remember { mutableStateOf(false) }

    Page {
        BackLink("Трассы") { app.pop() }
        LoadedContent(data) { d ->
            val track = d.tracks.find { it.id == route.id }
            if (track == null) { Empty("Такой трассы нет"); return@LoadedContent }
            val variants = track.variants
            val current = variants.find { it.id == variantId } ?: variants.firstOrNull()
            val shown = if (current != null) d.runs.filter { (it.variant ?: variants.first().id) == current.id } else d.runs
            val best = shown.mapNotNull { it.resultOf(me.id)?.bestMs }.minOrNull()

            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) { Eyebrow("Трасса"); PageTitle(track.name) }
                if (variants.size > 1 && current != null) Segmented(variants.map { it.name }, variants.indexOf(current), { variantId = variants[it].id })
                Card(padding = 20.dp) {
                    TrackMap(track, animateDraw = true, car = true, corners = true, reversed = current?.reversed == true)
                    Row(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (best != null) BestChip("Рекорд ${fmtLap(best)}")
                        Chip("${shown.size} ${plural(shown.size, "заезд", "заезда", "заездов")}")
                    }
                }
                SectionTitle("История заездов")
                if (shown.isEmpty()) {
                    Empty("Здесь появится история ваших заездов") {
                        AppButton("Создать заезд", { runSheet = true }, icon = Ic.Plus)
                    }
                } else {
                    LatestRunCard(app, shown.first(), me.id)
                    if (shown.size > 1) RunList(app, shown.drop(1), d.tracks, me.id)
                }
            }
            if (runSheet) RunSheet(app, d.tracks, onClose = { runSheet = false }, prefill = RunPrefill(track.id, current?.id))
        }
    }
}

/** Последний заезд — сразу в полном виде: мои круги, среднее и разброс. */
@Composable
private fun LatestRunCard(app: AppState, run: Run, userId: Int) {
    val r = run.resultOf(userId) ?: return
    val avg = r.laps.average()
    val cv = if (r.laps.size >= 3) 100 * kotlin.math.sqrt(r.laps.sumOf { (it - avg) * (it - avg) } / (r.laps.size - 1)) / avg else null
    Card(border = Rc.Accent.copy(alpha = 0.35f), onClick = { app.push(Route.Run(run.id)) }) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Eyebrow("Последний заезд")
                Txt(fmtDate(run.date), weight = FontWeight.Bold)
                Txt(listOfNotNull(r.position?.let { "позиция $it" }, "${r.laps.size} ${plural(r.laps.size, "круг", "круга", "кругов")}").joinToString(" · "), color = Rc.Muted, size = 13.sp)
            }
            Txt(fmtLap(r.bestMs), color = Rc.Best, size = 20.sp, weight = FontWeight.ExtraBold, mono = true)
            Icon24(Ic.Chevron, Rc.Muted, 18.dp, modifier = Modifier.padding(start = 8.dp))
        }
        Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            r.laps.forEachIndexed { i, lap -> LapRow(i + 1, lap, r.bestMs, i == r.bestIndex) }
        }
        Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip("среднее ${fmtLap(avg.toInt())}")
            if (cv != null) Chip("разброс ${((cv * 10).toInt() / 10.0)}%", color = if (cv < 1.5) Rc.Good else Rc.Muted, background = if (cv < 1.5) Rc.Good.copy(alpha = 0.14f) else Rc.Surface3)
        }
    }
}

