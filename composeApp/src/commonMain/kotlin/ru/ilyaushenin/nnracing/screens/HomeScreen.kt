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
import ru.ilyaushenin.nnracing.data.Planned
import ru.ilyaushenin.nnracing.data.Stats
import ru.ilyaushenin.nnracing.data.Track
import ru.ilyaushenin.nnracing.data.TrackStats
import ru.ilyaushenin.nnracing.ui.BestChip
import ru.ilyaushenin.nnracing.ui.Card
import ru.ilyaushenin.nnracing.ui.Chip
import ru.ilyaushenin.nnracing.ui.Empty
import ru.ilyaushenin.nnracing.ui.Eyebrow
import ru.ilyaushenin.nnracing.ui.LoadedContent
import ru.ilyaushenin.nnracing.ui.Page
import ru.ilyaushenin.nnracing.ui.PageTitle
import ru.ilyaushenin.nnracing.ui.Rc
import ru.ilyaushenin.nnracing.ui.SectionTitle
import ru.ilyaushenin.nnracing.ui.TrackMap
import ru.ilyaushenin.nnracing.ui.Txt
import ru.ilyaushenin.nnracing.ui.fmtLap
import ru.ilyaushenin.nnracing.ui.load
import ru.ilyaushenin.nnracing.ui.par
import ru.ilyaushenin.nnracing.ui.plural

data class HomeData(val tracks: List<Track>, val stats: Stats, val planned: List<Planned>)

/** Сводка по трассе: лучшее время и счётчики по всем вариантам. */
fun bestOf(variants: Map<String, TrackStats?>?): TrackStats? {
    val list = variants?.values?.filterNotNull().orEmpty()
    if (list.isEmpty()) return null
    return TrackStats(sessions = list.sumOf { it.sessions }, laps = list.sumOf { it.laps }, personalBestMs = list.minOf { it.personalBestMs })
}

@Composable
fun HomeScreen(app: AppState) {
    val user = app.user ?: return
    val data = load(app, cacheKey = "home") { par({ app.api.tracks() }, { app.api.stats() }, { app.api.planned() }).let { (t, s, p) -> HomeData(t, s, p) } }
    var planSheet by remember { mutableStateOf(false) }
    var finishPlan by remember { mutableStateOf<Planned?>(null) }

    Page {
        Eyebrow("Привет, ${user.name}", Modifier.padding(bottom = 14.dp))
        LoadedContent(data) { d ->
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    SectionTitle("Ближайшие заезды")
                    Chip("+ Запланировать", color = Rc.Muted, onClick = { planSheet = true })
                }
                if (d.planned.isEmpty()) Empty("Нет запланированных заездов")
                else {
                    NextPlanCard(app, d.planned.first(), d.tracks, onFinished = { finishPlan = it })
                    d.planned.drop(1).forEach { MiniPlan(app, it, d.tracks) }
                }
                PageTitle("Трассы", Modifier.padding(top = 14.dp))
                d.tracks.forEach { track -> TrackCard(app, track, bestOf(d.stats.tracks[track.id])) }
            }
            if (planSheet) PlanSheet(app, d.tracks, onClose = { planSheet = false })
            finishPlan?.let { plan ->
                val goingOthers = plan.members.filter { it.status == "going" && it.id != user.id }.map { it.id }
                RunSheet(
                    app, d.tracks, onClose = { finishPlan = null },
                    prefill = RunPrefill(plan.trackId, plan.variant, plan.startsAt, goingOthers, if (plan.isCreator) plan.id else null),
                )
            }
        }
    }
}

@Composable
fun TrackCard(app: AppState, track: Track, s: TrackStats?) {
    Card(padding = 18.dp, onClick = { app.push(Route.Track(track.id)) }) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Txt(track.name, size = 22.sp, weight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
            if (s != null) BestChip(fmtLap(s.personalBestMs)) else Chip("нет заездов")
        }
        Column(Modifier.padding(vertical = 12.dp, horizontal = 4.dp)) { TrackMap(track, animateDraw = true, car = false) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (s != null) {
                Chip("${s.sessions} ${plural(s.sessions, "заезд", "заезда", "заездов")}")
                Chip("${s.laps} ${plural(s.laps, "круг", "круга", "кругов")}")
            }
        }
    }
}

