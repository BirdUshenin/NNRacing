package ru.ilyaushenin.nnracing.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import ru.ilyaushenin.nnracing.AppState
import ru.ilyaushenin.nnracing.data.Planned
import ru.ilyaushenin.nnracing.data.Track
import ru.ilyaushenin.nnracing.ui.AppButton
import ru.ilyaushenin.nnracing.ui.Avatar
import ru.ilyaushenin.nnracing.ui.BtnKind
import ru.ilyaushenin.nnracing.ui.Card
import ru.ilyaushenin.nnracing.ui.Chip
import ru.ilyaushenin.nnracing.ui.Rc
import ru.ilyaushenin.nnracing.ui.TrackMap
import ru.ilyaushenin.nnracing.ui.Txt
import ru.ilyaushenin.nnracing.ui.fmtClock
import ru.ilyaushenin.nnracing.ui.fmtLong
import ru.ilyaushenin.nnracing.ui.nowSeconds

/** Секунды до старта, обновляются раз в секунду. */
@Composable
fun rememberSecondsLeft(startsAt: Long): Long {
    var left by remember(startsAt) { mutableLongStateOf((startsAt - nowSeconds()).coerceAtLeast(0)) }
    LaunchedEffect(startsAt) {
        while (true) { left = (startsAt - nowSeconds()).coerceAtLeast(0); delay(1000) }
    }
    return left
}

@Composable
fun CountdownTiles(startsAt: Long) {
    val left = rememberSecondsLeft(startsAt)
    val parts = listOf(left / 86400, left / 3600 % 24, left / 60 % 60, left % 60)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("дн", "ч", "мин", "сек").forEachIndexed { i, label ->
            Column(
                Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(Color(0x0DFFFFFF)).padding(vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Txt(parts[i].toString().padStart(2, '0'), size = 28.sp, weight = FontWeight.ExtraBold, mono = true)
                Txt(label.uppercase(), color = Rc.Muted, size = 11.sp, weight = FontWeight.Bold, letterSpacing = 1.sp)
            }
        }
    }
}

@Composable
fun CountdownText(startsAt: Long, color: Color) {
    val left = rememberSecondsLeft(startsAt)
    val hms = listOf(left / 3600 % 24, left / 60 % 60, left % 60).joinToString(":") { it.toString().padStart(2, '0') }
    val days = left / 86400
    Txt(if (left == 0L) "стартуем!" else (if (days > 0) "$days дн " else "") + hms, color = color, size = 15.sp, weight = FontWeight.ExtraBold, mono = true, maxLines = 1)
}

/** Кнопки «Еду / Не еду» для приглашённого; ничего, если отвечать уже не нужно. */
@Composable
fun InviteButtons(app: AppState, plan: Planned) {
    if (plan.myStatus != "invited") return
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AppButton("Еду", { app.act { app.api.respondPlanned(plan.id, true); app.showToast("Вы едете"); app.refresh() } }, small = true)
        AppButton("Не еду", { app.act { app.api.respondPlanned(plan.id, false); app.showToast("Вы отказались"); app.refresh() } }, kind = BtnKind.Ghost, small = true)
    }
}

/** Точный состав: кто едет, и отдельно — от кого ждём ответ. */
@Composable
fun Roster(plan: Planned) {
    val going = plan.members.filter { it.status == "going" }
    val waiting = plan.members.filter { it.status == "invited" }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            going.forEach { Avatar(it.name, it.avatarUrl, 44.dp) }
            waiting.forEach { Avatar(it.name, it.avatarUrl, 44.dp, Modifier.alpha(0.4f)) }
        }
        Txt("едут (${going.size}): " + going.joinToString(", ") { it.name }, color = Rc.Muted, size = 13.sp)
        if (waiting.isNotEmpty()) Txt("ждём ответа: " + waiting.joinToString(", ") { it.name }, color = Rc.Muted, size = 13.sp)
    }
}

@Composable
fun NextPlanCard(app: AppState, plan: Planned, tracks: List<Track>, onFinished: (Planned) -> Unit) {
    val track = tracks.find { it.id == plan.trackId }
    val variant = track?.variants?.find { it.id == plan.variant }
    val me = app.user
    Card(border = Rc.Accent.copy(alpha = 0.3f)) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.clip(RoundedCornerShape(50)).background(Color.White).padding(horizontal = 12.dp, vertical = 6.dp)) {
                    Txt("${fmtLong(plan.startsAt)} · ${fmtClock(plan.startsAt)}", color = Color(0xFF111111), size = 13.sp, weight = FontWeight.ExtraBold)
                }
                if (variant != null) Chip(variant.name)
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f).padding(end = 10.dp)) {
                    Txt(track?.name ?: "Заезд", size = 28.sp, weight = FontWeight.ExtraBold)
                    if (plan.notes.isNotBlank()) Txt(plan.notes, color = Rc.Muted, size = 14.sp, modifier = Modifier.padding(top = 8.dp))
                }
                if (track != null) Box(Modifier.width(120.dp)) { TrackMap(track, reversed = variant?.reversed == true, lineScale = 1.3f) }
            }
            CountdownTiles(plan.startsAt)
            Roster(plan)
            if (plan.myStatus == "invited") InviteButtons(app, plan) else Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AppButton("Заезд состоялся", { onFinished(plan) }, kind = BtnKind.Ghost, small = true)
                if (plan.isCreator) AppButton("Отменить", { app.act { app.api.cancelPlanned(plan.id); app.showToast("Готово"); app.refresh() } }, kind = BtnKind.Danger, small = true)
                else AppButton("Не поеду", { app.act { app.api.respondPlanned(plan.id, false); app.showToast("Готово"); app.refresh() } }, kind = BtnKind.Danger, small = true)
            }
        }
    }
}

@Composable
fun MiniPlan(app: AppState, plan: Planned, tracks: List<Track>) {
    val track = tracks.find { it.id == plan.trackId }
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Rc.Surface).border(1.dp, Rc.Line, RoundedCornerShape(16.dp)).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Txt(track?.name ?: "Заезд", weight = FontWeight.Bold, size = 14.sp)
            Txt("${fmtLong(plan.startsAt)} · ${fmtClock(plan.startsAt)}", color = Rc.Muted, size = 14.sp)
        }
        InviteButtons(app, plan)
    }
}
