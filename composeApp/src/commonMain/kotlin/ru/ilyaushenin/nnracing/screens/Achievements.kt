package ru.ilyaushenin.nnracing.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import ru.ilyaushenin.nnracing.data.Achievement
import ru.ilyaushenin.nnracing.data.BASE_URL
import ru.ilyaushenin.nnracing.ui.AccentBrush
import ru.ilyaushenin.nnracing.ui.Chip
import ru.ilyaushenin.nnracing.ui.Rc
import ru.ilyaushenin.nnracing.ui.SectionTitle
import ru.ilyaushenin.nnracing.ui.Sheet
import ru.ilyaushenin.nnracing.ui.Txt
import ru.ilyaushenin.nnracing.ui.fmtDate

/** Серые «ещё не получено» кубки: без цвета и приглушённые. */
private val GrayFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })

/** Лента кубков над вкладками профиля; выходит за боковые поля страницы до краёв экрана. */
@Composable
fun AchievementsStrip(list: List<Achievement>, own: Boolean = true) {
    if (list.isEmpty()) return
    var open by remember { mutableStateOf<Achievement?>(null) }
    // полученные первыми, затем те, к которым ещё идти
    val sorted = remember(list) { list.filter { it.earned } + list.filter { !it.earned } }
    Column(Modifier.padding(bottom = 16.dp)) {
        Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            SectionTitle("Достижения")
            if (own) Chip("${list.count { it.earned }} из ${list.size}")
        }
        val edge = 18.dp
        Row(
            Modifier.layout { measurable, c ->
                val extra = (edge * 2).roundToPx()
                val p = measurable.measure(c.copy(minWidth = c.maxWidth + extra, maxWidth = c.maxWidth + extra))
                layout(c.maxWidth, p.height) { p.place(-edge.roundToPx(), 0) }
            }.horizontalScroll(rememberScrollState()).padding(horizontal = edge),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) { sorted.forEach { AchievementCard(it) { open = it } } }
    }
    open?.let { a -> AchievementSheet(a) { open = null } }
}

@Composable
private fun AchievementCard(a: Achievement, onClick: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Column(
        Modifier.width(96.dp).clip(shape).background(Rc.Surface2).border(1.dp, Rc.Line, shape).clickable(onClick = onClick).padding(horizontal = 6.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(Modifier.size(width = 76.dp, height = 112.dp)) {
            AsyncImage(
                model = BASE_URL + a.image, contentDescription = a.title, contentScale = ContentScale.Fit,
                colorFilter = if (a.earned) null else GrayFilter, alpha = if (a.earned) 1f else 0.42f, modifier = Modifier.size(width = 76.dp, height = 112.dp),
            )
            if (a.count > 1) Box(Modifier.align(Alignment.TopEnd).clip(CircleShape).background(Rc.Accent).padding(horizontal = 7.dp, vertical = 1.dp)) {
                Txt("×${a.count}", color = Color.White, size = 12.sp, weight = FontWeight.ExtraBold)
            }
        }
        Box(Modifier.height(44.dp), contentAlignment = Alignment.Center) {
            Txt(a.title, color = if (a.earned) Rc.Text else Rc.Muted, size = 11.5.sp, weight = FontWeight.Bold, align = TextAlign.Center, maxLines = 3)
        }
        if (!a.earned && a.kind == "runs") Progress(a, Modifier.fillMaxWidth().height(4.dp))
    }
}

@Composable
private fun Progress(a: Achievement, modifier: Modifier) {
    Box(modifier.clip(RoundedCornerShape(2.dp)).background(Rc.Surface3)) {
        Box(Modifier.fillMaxWidth(a.have.toFloat() / a.need.coerceAtLeast(1)).height(10.dp).background(AccentBrush))
    }
}

@Composable
private fun AchievementSheet(a: Achievement, onClose: () -> Unit) {
    Sheet(a.title, onClose) {
        Column(Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            AsyncImage(
                model = BASE_URL + a.image, contentDescription = a.title, contentScale = ContentScale.Fit,
                colorFilter = if (a.earned) null else GrayFilter, alpha = if (a.earned) 1f else 0.42f, modifier = Modifier.size(width = 200.dp, height = 300.dp),
            )
            Txt(a.description, weight = FontWeight.SemiBold, align = TextAlign.Center)
            if (a.earned && a.earnedAt != null) Txt("Получено ${fmtDate(a.earnedAt)}", color = Rc.Muted, size = 14.sp)
            if (!a.earned && a.kind == "runs") Progress(a, Modifier.fillMaxWidth().height(8.dp))
        }
    }
}
