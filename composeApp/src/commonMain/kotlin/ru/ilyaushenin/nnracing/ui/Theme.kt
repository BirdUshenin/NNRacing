package ru.ilyaushenin.nnracing.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Палитра повторяет сайт: тёмный фон, красно-оранжевый акцент, фиолетовый лучший круг. */
object Rc {
    val Bg = Color(0xFF07070A)
    val Surface = Color(0xFF101015)
    val Surface2 = Color(0xFF17171E)
    val Surface3 = Color(0xFF20202A)
    val Line = Color(0x14FFFFFF)
    val LineStrong = Color(0x29FFFFFF)
    val Text = Color(0xFFF4F4F7)
    val Muted = Color(0xFF8D8D9B)
    val Hint = Color(0xFF5B5B68)
    val Accent = Color(0xFFFF3B24)
    val Accent2 = Color(0xFFFF7A3D)
    val Best = Color(0xFFB67CFF)
    val Good = Color(0xFF34D399)
    val Bad = Color(0xFFFF6B6B)
    val Gold = Color(0xFFF5C542)
    val Silver = Color(0xFFC3CAD6)
    val Bronze = Color(0xFFCD7F32)

    fun track(id: String): Color = when (id) {
        "academy-speedway" -> Color(0xFFFF3B24)
        "nring" -> Color(0xFF12D6A8)
        "f-drive" -> Color(0xFF4A80D8)
        else -> Muted
    }

    fun medal(position: Int?): Color? = when (position) {
        1 -> Gold
        2 -> Silver
        3 -> Bronze
        else -> null
    }
}

@Composable
fun NNTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Rc.Accent, onPrimary = Color.White, background = Rc.Bg, onBackground = Rc.Text,
            surface = Rc.Surface, onSurface = Rc.Text, surfaceVariant = Rc.Surface2, onSurfaceVariant = Rc.Muted,
            surfaceContainer = Rc.Surface2, surfaceContainerHigh = Rc.Surface2, surfaceContainerHighest = Rc.Surface3,
            outline = Rc.LineStrong, error = Rc.Bad,
        ),
        content = content,
    )
}
