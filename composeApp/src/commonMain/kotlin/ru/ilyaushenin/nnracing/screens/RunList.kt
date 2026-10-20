package ru.ilyaushenin.nnracing.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
import ru.ilyaushenin.nnracing.ui.Rc
import ru.ilyaushenin.nnracing.ui.SectionTitle
import ru.ilyaushenin.nnracing.ui.Txt
import ru.ilyaushenin.nnracing.ui.dateParts
import ru.ilyaushenin.nnracing.ui.fmtLap
import ru.ilyaushenin.nnracing.ui.plural

fun Run.resultOf(userId: Int): RunResult? = results.firstOrNull { it.userId == userId }

/** Список заездов: квадратик с датой цвета трассы, слева трасса и погода, справа моя позиция. Над группой — год. */
@Composable
fun RunList(app: AppState, runs: List<Run>, tracks: List<Track>, userId: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        var year: String? = null
        for (run in runs) {
            val y = run.date.take(4)
            if (y != year) { year = y; SectionTitle(y, Modifier.padding(start = 2.dp, top = 8.dp)) }
            RunRow(app, run, tracks, userId)
        }
    }
}

@Composable
private fun RunRow(app: AppState, run: Run, tracks: List<Track>, userId: Int) {
    val track = tracks.find { it.id == run.trackId }
    val variant = track?.variants?.find { it.id == run.variant }
    val parts = dateParts(run.date)
    val mine = run.resultOf(userId)
    val w = run.weather
    val sub = listOfNotNull(
        variant?.name, w?.condition, w?.airC?.let { "${trim(it)}°" },
        "${run.results.size} ${plural(run.results.size, "пилот", "пилота", "пилотов")}",
    ).joinToString(" · ")
    val shape = RoundedCornerShape(20.dp)
    Row(
        Modifier.fillMaxWidth().clip(shape).background(Brush.verticalGradient(listOf(Rc.Surface2, Rc.Surface))).border(1.dp, Rc.Line, shape)
            .clickable { app.push(Route.Run(run.id)) }.padding(start = 10.dp, end = 14.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Column(Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).background(Rc.track(run.trackId)), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Txt(parts.day.toString(), color = Color.White, size = 22.sp, weight = FontWeight.ExtraBold, mono = true)
            Txt(parts.month, color = Color.White.copy(alpha = 0.92f), size = 11.sp, weight = FontWeight.ExtraBold)
        }
        Column(Modifier.weight(1f)) {
            Txt(track?.name ?: "Трасса", weight = FontWeight.ExtraBold, maxLines = 1)
            Txt(sub, color = Rc.Muted, size = 13.sp, maxLines = 1)
        }
        if (mine != null) Column(horizontalAlignment = Alignment.End) {
            if (mine.position != null) Txt("P${mine.position}", color = Rc.medal(mine.position) ?: Rc.Text, size = 20.sp, weight = FontWeight.ExtraBold, mono = true)
            Txt(fmtLap(mine.bestMs), color = Rc.Muted, size = 12.sp, mono = true, align = TextAlign.End)
        }
    }
}

fun trim(v: Double): String = if (v == v.toLong().toDouble()) v.toLong().toString() else v.toString()
