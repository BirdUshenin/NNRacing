package ru.ilyaushenin.nnracing.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import ru.ilyaushenin.nnracing.AppState
import ru.ilyaushenin.nnracing.data.Achievement
import ru.ilyaushenin.nnracing.data.BASE_URL
import ru.ilyaushenin.nnracing.ui.Rc
import ru.ilyaushenin.nnracing.ui.Txt
import ru.ilyaushenin.nnracing.ui.neonColor
import kotlin.math.PI
import kotlin.math.sin

/** Свечение за наградой: золото, серебро, бронза для кубков по месту; для медалей за заезды — цвет неона профиля. */
private fun glowFor(a: Achievement, neon: String?): Color = when {
    a.kind == "podium" && a.place == 1 -> Color(0xFFFFC83A)
    a.kind == "podium" && a.place == 2 -> Color(0xFFC9D1DE)
    a.kind == "podium" -> Color(0xFFE08A45)
    else -> neonColor(neon)
}

/**
 * Экран-поздравление: размытая копия приложения, за наградой — мягкий неон, сама награда «выпрыгивает» и покачивается.
 * Награды идут по одной: нажатие показывает следующую.
 */
@Composable
fun CelebrationLayer(app: AppState, backdrop: GraphicsLayer) {
    val current = app.celebrations.firstOrNull() ?: return
    val glow = glowFor(current, app.user?.neon?.firstOrNull())
    val blurLayer = rememberGraphicsLayer()
    val blurred = remember { runCatching { blurLayer.renderEffect = BlurEffect(48f, 48f, TileMode.Clamp); true }.getOrDefault(false) }
    val pop = remember(current.id) { Animatable(0f) }
    val pulse = remember { Animatable(0f) }
    LaunchedEffect(current.id) { pop.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessLow)) }
    LaunchedEffect(Unit) {
        while (true) { pulse.animateTo(1f, tween(1400)); pulse.animateTo(0f, tween(1400)) }
    }
    Box(
        Modifier.fillMaxSize()
            .drawBehind {
                drawRect(Rc.Bg)
                if (blurred && backdrop.size.width > 0) {
                    blurLayer.record(androidx.compose.ui.unit.IntSize(backdrop.size.width, backdrop.size.height)) { drawRect(Rc.Bg); drawLayer(backdrop) }
                    drawLayer(blurLayer)
                }
            }
            .background(Color(0xFF05050A).copy(alpha = if (blurred) 0.55f else 0.92f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { app.celebrations.removeAt(0) },
        contentAlignment = Alignment.Center,
    ) {
        // неон за наградой
        Box(
            Modifier.size(420.dp).graphicsLayer { val s = 1f + 0.1f * sin(pulse.value * PI.toFloat()); scaleX = s; scaleY = s }
                .drawBehind {
                    val r = size.minDimension / 2
                    drawCircle(Brush.radialGradient(listOf(glow.copy(alpha = 0.55f), glow.copy(alpha = 0.22f), glow.copy(alpha = 0.06f), Color.Transparent), center = Offset(r, r), radius = r), radius = r, center = Offset(r, r))
                },
        )
        Column(Modifier.padding(horizontal = 32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            val more = app.celebrations.size - 1
            Txt(if (more > 0) "НОВАЯ НАГРАДА · ЕЩЁ $more" else "НОВАЯ НАГРАДА", color = Rc.Muted, size = 13.sp, weight = FontWeight.ExtraBold)
            AsyncImage(
                model = BASE_URL + current.image, contentDescription = current.title, contentScale = ContentScale.Fit,
                modifier = Modifier.size(width = 220.dp, height = 280.dp).graphicsLayer {
                    scaleX = 0.4f + 0.6f * pop.value; scaleY = scaleX; alpha = pop.value.coerceIn(0f, 1f)
                    translationY = (1f - pop.value) * 120f - 6f * sin(pulse.value * PI.toFloat())
                },
            )
            Txt(current.title, size = 24.sp, weight = FontWeight.ExtraBold, align = TextAlign.Center)
            Txt(current.description, color = Rc.Muted, size = 15.sp, align = TextAlign.Center)
            // кнопка без фона, с белой обводкой
            Txt(
                "Далее", color = Color.White, size = 18.sp, weight = FontWeight.Bold,
                modifier = Modifier.padding(top = 14.dp)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                    .border(1.5.dp, Color.White, androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { app.celebrations.removeAt(0) }
                    .padding(horizontal = 36.dp, vertical = 13.dp),
            )
        }
    }
}
