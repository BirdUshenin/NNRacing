package ru.ilyaushenin.nnracing.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private const val GLINT_LAYERS = 9
private const val GLINT_TAIL = 0.22f   // длина хвоста блика — доля длины трассы

private val Rainbow = listOf(Color(0xFFFF3B24), Color(0xFFFF9B3D), Color(0xFF12D6A8), Color(0xFF4A80D8), Color(0xFFFF3B24))

/**
 * Трасса NRing с переливающейся разноцветной линией и бегущим бликом.
 * [cover] — заполнить весь блок (фон), иначе вписать целиком (значок загрузки).
 * [line] — толщина линии в единицах схемы.
 */
@Composable
fun ShimmerTrack(modifier: Modifier = Modifier, cover: Boolean = false, line: Float = 74f) {
    val geometry = remember { TrackGeometry(AuthArt.PATH) }
    val transition = rememberInfiniteTransition()
    val shift = transition.animateFloat(0f, 1f, infiniteRepeatable(tween(if (cover) 14000 else 3500, easing = LinearEasing), RepeatMode.Reverse))
    val glint = transition.animateFloat(0f, 1f, infiniteRepeatable(tween(if (cover) 7000 else 1800, easing = LinearEasing), RepeatMode.Restart))
    val (vx, vy, vw, vh) = AuthArt.viewBox.map { it.toFloat() }
    Box(
        modifier.drawWithCache {
            val k = if (cover) maxOf(size.width / vw, size.height / vh) * 1.15f else minOf(size.width / vw, size.height / vh)
            val ox = (size.width - vw * k) / 2
            val oy = (size.height - vh * k) / 2
            val path = geometry.toPath(false) { x, y -> Offset(ox + (x - vx) * k, oy + (y - vy) * k) }
            val w = line * k
            val pathLen = androidx.compose.ui.graphics.PathMeasure().also { it.setPath(path, true) }.length
            onDrawBehind {
                drawPath(path, Color(0xFF23232D), style = Stroke(w * 1.5f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                val s = shift.value
                val brush = Brush.linearGradient(Rainbow, Offset(-size.width * s, -size.height * s), Offset(size.width * (1.4f - s), size.height * (1.4f - s)))
                drawPath(path, brush, style = Stroke(w, cap = StrokeCap.Round, join = StrokeJoin.Round))
                // мягкий блик: хвост из нескольких наложенных полос разной длины, у «головы» ярче, к хвосту плавно гаснет
                val period = pathLen * 1.25f   // блик проходит трассу целиком и немного «отдыхает» за кадром
                val head = glint.value * period
                for (k in 0 until GLINT_LAYERS) {
                    val len = pathLen * GLINT_TAIL * (k + 1) / GLINT_LAYERS
                    val thick = w * (0.30f + 0.35f * (GLINT_LAYERS - k) / GLINT_LAYERS)
                    drawPath(
                        path, Color.White.copy(alpha = 0.07f),
                        style = Stroke(thick, cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(len, period - len), len - head)),
                    )
                }
            }
        },
    )
}

/** Размытая и затемнённая трасса на весь экран: фон входа и заставка при запуске. */
@Composable
fun TrackBackdrop(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().background(Rc.Bg)) {
        ShimmerTrack(Modifier.fillMaxSize().blur(18.dp), cover = true)
        Box(Modifier.fillMaxSize().background(Brush.radialGradient(listOf(Color(0x59070A0A), Color(0xCC07070A)))))
    }
}

/** Небольшой значок загрузки: та же трасса маленькой, цвета переливаются. */
@Composable
fun LoadingLogo(size: Dp = 56.dp, modifier: Modifier = Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) { ShimmerTrack(Modifier.size(size), cover = false, line = 110f) }
}

/** Заставка при запуске: трасса целиком, чёткая, без размытия; цвета переливаются. */
@Composable
fun SplashTrack(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().background(Rc.Bg).padding(20.dp)) { ShimmerTrack(Modifier.fillMaxSize(), cover = false, line = 46f) }
}
