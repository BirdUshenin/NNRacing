package ru.ilyaushenin.nnracing.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Иконки сайта в виде штриховых контуров 24×24. Флаги дуг пишем через пробел: компактная запись («100-8») парсером Compose читается неверно. */
object Ic {
    const val Flag = "M5 21V4m0 0c4-2 6 2 10 0v9c-4 2-6-2-10 0"
    const val Home = "M3 11l9-8 9 8M5 10v10h5v-6h4v6h5V10"
    const val User = "M12 12a4 4 0 1 0 0-8 4 4 0 0 0 0 8zM4 20c0-3.5 3.6-6 8-6s8 2.5 8 6"
    const val Plus = "M12 5v14M5 12h14"
    const val X = "M6 6l12 12M18 6L6 18"
    const val Star = "M12 3l2.7 5.6 6.1.9-4.4 4.3 1 6.1L12 17l-5.4 2.9 1-6.1L3.2 9.5l6.1-.9z"
    const val Trash = "M4 7h16M10 11v6M14 11v6M6 7l1 13h10l1-13M9 7V4h6v3"
    const val Chevron = "M9 6l6 6-6 6"
    const val Back = "M15 6l-6 6 6 6"
    const val Edit = "M4 20h4L19 9l-4-4L4 16z"
    const val Logout = "M10 4H5v16h5M15 8l4 4-4 4M19 12H9"
    const val Camera = "M4 8h3l2-3h6l2 3h3v11H4zM12 17a4 4 0 1 0 0-8 4 4 0 0 0 0 8z"
    const val Chat = "M4 5h16v11H9l-5 4z"
    const val Users = "M16 20v-1.5a3.5 3.5 0 0 0-3.5-3.5h-5A3.5 3.5 0 0 0 4 18.5V20M10 12a3.5 3.5 0 1 0 0-7 3.5 3.5 0 0 0 0 7zM17 5.2a3.5 3.5 0 0 1 0 6.6M20 20v-1.5a3.5 3.5 0 0 0-2.5-3.3"
    const val Search = "M11 18a7 7 0 1 0 0-14 7 7 0 0 0 0 14zM20 20l-4-4"
    const val Send = "M12 19V5M5 12l7-7 7 7"
    const val Check = "M5 12.5l4.5 4.5L19 7.5"
    const val Clock = "M12 21a9 9 0 1 0 0-18 9 9 0 0 0 0 18zM12 7v5l3 2"
    const val Eye = "M2 12s4-7 10-7 10 7 10 7-4 7-10 7-10-7-10-7zM12 15a3 3 0 1 0 0-6 3 3 0 0 0 0 6z"
    const val EyeOff = "M2 12s4-7 10-7c2 0 3.7.6 5 1.5M22 12s-4 7-10 7c-2 0-3.7-.6-5-1.5M3 3l18 18M10 10a3 3 0 0 0 4 4"
    const val Sticker = "M12 3.5a8.5 8.5 0 1 0 0 17c2.5 0 3.7-.4 5.3-1.9l1.8-1.8c1.5-1.6 1.9-2.8 1.9-5.3A8.5 8.5 0 0 0 12 3.5zM20.5 12h-3a3 3 0 0 0-3 3v3M9 10v.5M15 10v.5M8.5 14.5c1.9 1.8 4.1 1.8 5.5 0"
    const val Poll = "M5.5 4h12a2 2 0 0 1 0 4h-12a2 2 0 0 1 0-4zM5.5 10h7a2 2 0 0 1 0 4h-7a2 2 0 0 1 0-4zM5.5 16h9.5a2 2 0 0 1 0 4h-9.5a2 2 0 0 1 0-4z"
    const val Compose = "M11 4H6.5A2.5 2.5 0 0 0 4 6.5v11A2.5 2.5 0 0 0 6.5 20h11a2.5 2.5 0 0 0 2.5-2.5V13M17 3.8l3.2 3.2L11.5 15.7l-3.9.7.7-3.9z"
    const val Download = "M12 4v11M7 11l5 5 5-5M5 20h14"
    const val Ban = "M12 4a8 8 0 1 0 0 16 8 8 0 0 0 0-16zM6.3 6.3l11.4 11.4"
    const val Bell = "M6 17V11a6 6 0 0 1 12 0v6l1.6 2.2H4.4zM10 21.5h4"
    const val Reply = "M9 14L4 9l5-5M4 9h10a6 6 0 0 1 6 6v3"
    const val Copy = "M9 9h11v11H9zM5 15V4h11"
    const val Lock = "M6 11h12v9H6zM8 11V8a4 4 0 0 1 8 0v3"
}

@Composable
fun Icon24(path: String, tint: Color, size: Dp = 22.dp, strokeWidth: Float = 2f, filled: Boolean = false, modifier: Modifier = Modifier) {
    val parsed = remember(path) { PathParser().parsePathString(path).toPath() }
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension / 24f
        withTransform({ scale(s, s, pivot = Offset.Zero) }) {
            if (filled) drawPath(parsed, tint)
            drawPath(parsed, tint, style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}
