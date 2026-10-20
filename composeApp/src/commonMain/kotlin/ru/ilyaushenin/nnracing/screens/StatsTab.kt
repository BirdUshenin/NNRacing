package ru.ilyaushenin.nnracing.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.ilyaushenin.nnracing.AppState
import ru.ilyaushenin.nnracing.data.HistoryPoint
import ru.ilyaushenin.nnracing.data.Stats
import ru.ilyaushenin.nnracing.data.Track
import ru.ilyaushenin.nnracing.data.TrackStats
import ru.ilyaushenin.nnracing.ui.Card
import ru.ilyaushenin.nnracing.ui.Chip
import ru.ilyaushenin.nnracing.ui.Empty
import ru.ilyaushenin.nnracing.ui.LoadedContent
import ru.ilyaushenin.nnracing.ui.Rc
import ru.ilyaushenin.nnracing.ui.Txt
import ru.ilyaushenin.nnracing.ui.fmtDate
import ru.ilyaushenin.nnracing.ui.fmtDelta
import ru.ilyaushenin.nnracing.ui.fmtLap
import ru.ilyaushenin.nnracing.ui.load
import ru.ilyaushenin.nnracing.ui.par
import ru.ilyaushenin.nnracing.ui.plural
import kotlin.math.abs
import kotlin.math.roundToInt

private data class StatsData(val stats: Stats, val tracks: List<Track>)

@Composable
fun StatsTab(app: AppState) {
    val data = load(app, cacheKey = "stats") { par({ app.api.stats() }, { app.api.tracks() }).let { (s, t) -> StatsData(s, t) } }
    LoadedContent(data) { d ->
        val entries = ArrayList<Triple<Track?, TrackStats, String?>>()
        for ((trackId, variants) in d.stats.tracks) {
            val track = d.tracks.find { it.id == trackId }
            val names = track?.variants?.associate { it.id to it.name }.orEmpty()
            for ((variant, s) in variants) if (s != null) entries.add(Triple(track, s, names[variant]))
        }
        if (entries.isEmpty()) { Empty("Статистика появится после первого заезда."); return@LoadedContent }
        val o = d.stats.overall
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Metric(o.sessions.toString(), plural(o.sessions, "заезд", "заезда", "заездов"), Modifier.weight(1f))
                Metric(o.laps.toString(), plural(o.laps, "круг", "круга", "кругов"), Modifier.weight(1f))
                Metric(o.avgImprovementPct?.let { (if (it > 0) "+" else "") + trim(it) + "%" } ?: "—", "прогресс", Modifier.weight(1f))
            }
            entries.forEach { (track, s, variantName) -> TrackStatsCard(track, s, variantName) }
        }
    }
}

@Composable
private fun Metric(value: String, label: String, modifier: Modifier) {
    Card(modifier, padding = 12.dp) {
        Txt(value, size = 24.sp, weight = FontWeight.ExtraBold, mono = true, align = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Txt(label.uppercase(), color = Rc.Muted, size = 11.sp, weight = FontWeight.Bold, letterSpacing = 1.sp, align = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun TrackStatsCard(track: Track?, s: TrackStats, variantName: String?) {
    val name = (track?.name ?: "Трасса") + (variantName?.let { " · $it" } ?: "")
    val trend = s.trendMsPerSession
    val rows = listOf(
        Triple("Рекорд", fmtLap(s.personalBestMs), fmtDate(s.personalBestDate)),
        Triple("Средний круг", fmtLap(s.avgLapMs), "${s.laps} ${plural(s.laps, "круг", "круга", "кругов")}"),
        Triple("Стабильность", s.consistencyCvPct?.let { "${trim(it)}%" } ?: "—", "разброс кругов, меньше — лучше"),
        Triple("Тренд", trend?.let { "${fmtDelta(it)} с" } ?: "—", "в среднем за заезд"),
        Triple("С начала", s.improvementPct?.let { (if (it > 0) "−" else "+") + trim(abs(it)) + "%" } ?: "—", s.improvementMs?.let { "${fmtDelta(-it)} с" } ?: ""),
        Triple("Прогноз", s.forecastNextMs?.let { fmtLap(it.roundToInt()) } ?: "—", s.learningRate?.let { "learning rate ${trim(it)}" } ?: "нужно 3 заезда"),
    )
    Card {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Txt(name, size = 19.sp, weight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
            Chip(
                "${s.sessions} ${plural(s.sessions, "заезд", "заезда", "заездов")}",
                color = if (trend != null && trend < 0) Rc.Good else if (trend != null && trend > 0) Rc.Bad else Rc.Muted,
                background = if (trend != null && trend < 0) Rc.Good.copy(alpha = 0.14f) else if (trend != null && trend > 0) Rc.Bad.copy(alpha = 0.14f) else Rc.Surface3,
            )
        }
        if (s.history.size > 1) Sparkline(s.history) else Txt("График появится после второго заезда.", color = Rc.Muted, size = 12.sp, modifier = Modifier.padding(top = 12.dp))
        Column(Modifier.padding(top = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            rows.chunked(2).forEach { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    pair.forEach { (label, value, note) ->
                        Column(Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(Color(0x09FFFFFF)).padding(12.dp)) {
                            Txt(label.uppercase(), color = Rc.Muted, size = 11.sp, weight = FontWeight.Bold, letterSpacing = 0.6.sp)
                            Txt(value, size = 19.sp, weight = FontWeight.ExtraBold, mono = true, modifier = Modifier.padding(top = 3.dp))
                            Txt(note, color = Rc.Muted, size = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

/** Лучший круг каждого заезда: чем быстрее, тем выше точка. */
@Composable
private fun Sparkline(history: List<HistoryPoint>) {
    val values = history.map { it.bestMs.toFloat() }
    val min = values.min()
    val span = maxOf(values.max() - min, 1f)
    Canvas(Modifier.fillMaxWidth().height(86.dp).padding(top = 12.dp)) {
        val pad = 10.dp.toPx()
        val pts = values.mapIndexed { i, v -> Offset(pad + i * (size.width - 2 * pad) / (values.size - 1), pad + (v - min) / span * (size.height - 2 * pad)) }
        val line = Path().also { p -> pts.forEachIndexed { i, o -> if (i == 0) p.moveTo(o.x, o.y) else p.lineTo(o.x, o.y) } }
        val fill = Path().also { p -> p.addPath(line); p.lineTo(pts.last().x, size.height); p.lineTo(pts.first().x, size.height); p.close() }
        drawPath(fill, Brush.verticalGradient(listOf(Rc.Accent.copy(alpha = 0.35f), Color.Transparent)))
        drawPath(line, Color(0xFFFF5A3A), style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        val bestIdx = values.indexOf(min)
        pts.forEachIndexed { i, o -> drawCircle(if (i == bestIdx) Rc.Best else Color(0xFFFF5A3A), if (i == bestIdx) 4.5.dp.toPx() else 3.dp.toPx(), o) }
    }
}
