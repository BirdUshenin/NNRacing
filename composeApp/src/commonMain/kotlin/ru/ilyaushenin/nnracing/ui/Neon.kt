package ru.ilyaushenin.nnracing.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.dp

/** Цвета неонового свечения профиля: ключ хранится на сервере, оттенок задаёт приложение. */
val NEON_COLORS = listOf(
    "red" to Color(0xFFFF3B24), "orange" to Color(0xFFFF8A1F), "yellow" to Color(0xFFFFD21F), "green" to Color(0xFF2FE07A),
    "teal" to Color(0xFF19D9C0), "blue" to Color(0xFF3D8BFF), "purple" to Color(0xFF9B5CFF), "pink" to Color(0xFFFF4FA3), "white" to Color(0xFFEDEDF5),
)

fun neonColor(key: String?): Color = NEON_COLORS.firstOrNull { it.first == key }?.second ?: NEON_COLORS.first().second

/** Огни равномерно распределены по всей ширине: шесть мест от аватарки до правого края, свободные места просто не светятся. */
private const val NEON_SLOTS = 6

/**
 * Свечение профиля: до шести огней слева направо, у каждого плотное ядро под аватаркой/в начале шапки и длинный мягкий хвост вниз — на шапку и награды.
 * Огни не поднимаются выше аватарки. Второй, очень лёгкий слой рисуется поверх содержимого, чтобы пятна подкрашивали и карточки наград.
 */
fun Modifier.neonGlow(colors: List<String>, enabled: Boolean = true): Modifier = if (!enabled || colors.isEmpty()) this else this
    .drawBehind { drawNeon(colors, 1f) }
    .drawWithContent {
        drawContent()
        drawNeon(colors, 0.30f)
    }

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawNeon(colors: List<String>, k: Float) {
    val first = 40.dp.toPx()
    val step = (size.width - 2 * first) / (NEON_SLOTS - 1)
    // ширина огня подгоняется под шаг, чтобы соседние сливались, а чёрных промежутков не оставалось
    val half = maxOf(95.dp.toPx(), step * 1.55f)
    colors.forEachIndexed { i, key ->
        val color = neonColor(key)
        val x = first + step * i
        val fade = if (i == 0) 1f else 0.85f
        // хвост: верх овала у верхней границы аватарки; ядро: плотное пятно сразу под ней
        oval(color, Offset(x, 250.dp.toPx()), half, 250.dp.toPx(), 0.22f * k * fade)
        oval(color, Offset(x, 105.dp.toPx()), half * 0.78f, 105.dp.toPx(), 0.50f * k * fade)
    }
}

private val FLOAT_STOPS = floatArrayOf(0f, 0.12f, 0.24f, 0.36f, 0.48f, 0.60f, 0.72f, 0.84f, 0.94f, 1f)

private fun androidx.compose.ui.graphics.drawscope.DrawScope.oval(color: Color, center: Offset, rx: Float, ry: Float, alpha: Float) {
    withTransform({ scale(1f, ry / rx, pivot = center) }) {
        // плавное (гауссово) затухание от центра к краю: без видимой границы пятна
        val stops = FLOAT_STOPS.map { t -> t to color.copy(alpha = alpha * kotlin.math.exp(-3.4f * t * t) * (1f - t * t * t)) }.toTypedArray()
        drawCircle(Brush.radialGradient(colorStops = stops, center = center, radius = rx), radius = rx, center = center)
    }
}
