package ru.ilyaushenin.nnracing.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import ru.ilyaushenin.nnracing.data.Track
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/** Контур трассы из track.json: «M x y C … Z» → кубические кривые. */
class TrackGeometry(path: String) {
    private val start: Pair<Float, Float>
    private val segments: List<List<Float>>   // c1x c1y c2x c2y x y

    init {
        val n = Regex("-?\\d+\\.?\\d*").findAll(path).map { it.value.toFloat() }.toList()
        start = n[0] to n[1]
        val segs = ArrayList<List<Float>>()
        var i = 2
        while (i + 5 < n.size) { segs.add(n.subList(i, i + 6)); i += 6 }
        segments = segs
    }

    /** Путь в пикселях; [reversed] — те же кривые в обратном порядке (управляющие точки меняются местами). */
    fun toPath(reversed: Boolean, map: (Float, Float) -> Offset): Path {
        val p = Path()
        fun o(x: Float, y: Float) = map(x, y)
        if (!reversed) {
            o(start.first, start.second).let { p.moveTo(it.x, it.y) }
            for (s in segments) {
                val a = o(s[0], s[1]); val b = o(s[2], s[3]); val c = o(s[4], s[5])
                p.cubicTo(a.x, a.y, b.x, b.y, c.x, c.y)
            }
        } else {
            val anchors = listOf(start) + segments.map { it[4] to it[5] }
            o(anchors.last().first, anchors.last().second).let { p.moveTo(it.x, it.y) }
            for (k in segments.indices.reversed()) {
                val s = segments[k]
                val a = o(s[2], s[3]); val b = o(s[0], s[1]); val c = o(anchors[k].first, anchors[k].second)
                p.cubicTo(a.x, a.y, b.x, b.y, c.x, c.y)
            }
        }
        p.close()
        return p
    }
}

/**
 * Схема трассы: подсветка, асфальт, линия с градиентом цвета трассы, старт с шахматкой и стрелкой, машинка.
 * [reversed] — вариант «Реверс»: стрелка, линия и машинка идут в обратную сторону.
 */
@Composable
fun TrackMap(
    track: Track,
    modifier: Modifier = Modifier,
    animateDraw: Boolean = false,
    car: Boolean = false,
    corners: Boolean = false,
    reversed: Boolean = false,
    lineScale: Float = 1f,
) {
    val color = Rc.track(track.id)
    val geometry = remember(track.path) { TrackGeometry(track.path) }
    val draw = remember(track.id) { Animatable(if (animateDraw) 0f else 1f) }
    LaunchedEffect(track.id, animateDraw) { if (animateDraw) draw.animateTo(1f, tween(1800)) }
    val carT = if (car) rememberInfiniteTransition().animateFloat(0f, 1f, infiniteRepeatable(tween(9000, easing = LinearEasing), RepeatMode.Restart)) else null
    val measurer = rememberTextMeasurer()
    // линия на схеме может быть нарисована против «прямого» направления: тогда машинке и линии нужен разворот
    val travelReversed = reversed != (track.start?.pathAgainst == true)

    val (vx, vy, vw, vh) = track.viewBox.map { it.toFloat() }
    Box(
        modifier.fillMaxWidth().aspectRatio(vw / vh).drawWithCache {
            val scale = size.width / vw
            val map = { x: Float, y: Float -> Offset((x - vx) * scale, (y - vy) * scale) }
            val lineW = max(vw, vh) * 0.02f * lineScale * scale
            val full = geometry.toPath(false, map)
            val travel = geometry.toPath(travelReversed, map)
            val measure = PathMeasure().also { it.setPath(travel, true) }
            val brush = Brush.linearGradient(listOf(color, lighten(color, 0.3f)), Offset.Zero, Offset(size.width, size.height))
            val cap = StrokeCap.Round
            val join = StrokeJoin.Round
            onDrawBehind {
                drawPath(full, color.copy(alpha = 0.10f), style = Stroke(lineW * 3.4f, cap = cap, join = join))
                drawPath(full, color.copy(alpha = 0.18f), style = Stroke(lineW * 2.6f, cap = cap, join = join))
                drawPath(full, Color(0xFF2B2B36), style = Stroke(lineW * 1.7f, cap = cap, join = join))
                if (draw.value >= 0.999f) {
                    drawPath(travel, brush, style = Stroke(lineW, cap = cap, join = join))
                } else {
                    val seg = Path()
                    measure.getSegment(0f, measure.length * draw.value, seg, true)
                    drawPath(seg, brush, style = Stroke(lineW, cap = cap, join = join))
                }
                track.start?.let { drawStart(map(it.x.toFloat(), it.y.toFloat()), it.angle.toFloat(), reversed, lineW) }
                if (corners) track.corners.forEach { c ->
                    val p = map(c.x.toFloat(), c.y.toFloat())
                    val layout = measurer.measure("T${c.n}", TextStyle(color = Color(0xC7FFFFFF), fontSize = (lineW * 0.9f / density).sp, fontWeight = FontWeight.Bold))
                    drawText(layout, topLeft = Offset(p.x - layout.size.width / 2f, p.y - layout.size.height / 2f))
                }
                carT?.let {
                    val pos = measure.getPosition(measure.length * it.value)
                    drawCircle(Color.White.copy(alpha = 0.35f), lineW * 0.8f, pos)
                    drawCircle(Color.White, lineW * 0.42f, pos)
                }
            }
        },
    ) {}
}

private fun lighten(c: Color, amt: Float) = Color(c.red + (1 - c.red) * amt, c.green + (1 - c.green) * amt, c.blue + (1 - c.blue) * amt, c.alpha)

private fun DrawScope.drawStart(center: Offset, angle: Float, reversed: Boolean, lineW: Float) {
    val cell = lineW * 0.5f
    rotate(angle + 90f, center) {
        for (r in 0 until 2) for (c in -3 until 3) {
            drawRect(if ((r + c) % 2 != 0) Color.White else Color(0xFF111111), Offset(center.x + c * cell, center.y + (r - 1) * cell), Size(cell, cell))
        }
    }
    // стрелка направления проезда
    val turn = (angle + if (reversed) 180f else 0f) * (PI.toFloat() / 180f)
    val ax = center.x + cos(turn) * lineW * 2.6f
    val ay = center.y + sin(turn) * lineW * 2.6f
    fun pt(dx: Float, dy: Float) = Offset(ax + dx * cos(turn) - dy * sin(turn), ay + dx * sin(turn) + dy * cos(turn))
    val p = Path()
    pt(-lineW * .7f, -lineW * .8f).let { p.moveTo(it.x, it.y) }
    pt(lineW * .9f, 0f).let { p.lineTo(it.x, it.y) }
    pt(-lineW * .7f, lineW * .8f).let { p.lineTo(it.x, it.y) }
    p.close()
    drawPath(p, Color.White)
}
