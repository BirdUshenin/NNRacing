package ru.ilyaushenin.nnracing.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Значок приложения: неоновая трасса на тёмной плашке. */
@Composable
fun AppLogo(size: Dp = 72.dp, modifier: Modifier = Modifier) {
    val geometry = remember { TrackGeometry(AuthArt.PATH) }
    val shape = RoundedCornerShape(size * 0.3f)
    Box(modifier.size(size).clip(shape).background(Color(0xFF0D0D12)).border(1.dp, Rc.LineStrong, shape).padding(size * 0.14f)) {
        Canvas(Modifier.size(size * 0.72f)) {
            val (vx, vy, vw, vh) = AuthArt.viewBox.map { it.toFloat() }
            val k = minOf(this.size.width / vw, this.size.height / vh)
            val ox = (this.size.width - vw * k) / 2
            val oy = (this.size.height - vh * k) / 2
            val path = geometry.toPath(false) { x, y -> Offset(ox + (x - vx) * k, oy + (y - vy) * k) }
            drawPath(path, Rc.Accent.copy(alpha = 0.35f), style = Stroke(this.size.width * 0.14f, cap = StrokeCap.Round, join = StrokeJoin.Round))
            drawPath(path, Rc.Accent, style = Stroke(this.size.width * 0.06f, cap = StrokeCap.Round, join = StrokeJoin.Round))
            drawPath(path, Color(0xFFFFB199), style = Stroke(this.size.width * 0.02f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}

/** Контур трассы одной линией: значок заезда (запланировать). */
@Composable
fun TrackIcon(tint: Color, size: Dp = 24.dp, modifier: Modifier = Modifier) {
    val geometry = remember { TrackGeometry(AuthArt.PATH) }
    Canvas(modifier.size(size)) {
        val (vx, vy, vw, vh) = AuthArt.viewBox.map { it.toFloat() }
        val k = minOf(this.size.width / vw, this.size.height / vh)
        val ox = (this.size.width - vw * k) / 2
        val oy = (this.size.height - vh * k) / 2
        val path = geometry.toPath(false) { x, y -> Offset(ox + (x - vx) * k, oy + (y - vy) * k) }
        drawPath(path, tint, style = Stroke(this.size.width * 0.07f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

/** Кнопка 46×46 со значком трассы. */
@Composable
fun TrackIconBtn(onClick: () -> Unit, modifier: Modifier = Modifier, size: Dp = 46.dp) {
    Box(modifier.size(size).clip(RoundedCornerShape(14.dp)).background(Rc.Surface3).clickableNoRipple(onClick), contentAlignment = androidx.compose.ui.Alignment.Center) {
        TrackIcon(Color.White, 28.dp)
    }
}
