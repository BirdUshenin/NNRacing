package ru.ilyaushenin.nnracing.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import kotlinx.coroutines.delay
import ru.ilyaushenin.nnracing.data.BASE_URL
import ru.ilyaushenin.nnracing.ui.Rc
import ru.ilyaushenin.nnracing.ui.Sheet
import kotlin.math.roundToInt

/** Стикеры: имя → подпись. Файлы лежат на сервере: /icons/stickers/<имя>.webp, спрайт 4×4 кадра. */
val STICKERS = listOf(
    "horse" to "Конь", "bull" to "Бык", "cheer" to "Ура", "neon-academy" to "Academy", "neon-nring" to "NRing", "neon-fdrive" to "F-Drive",
    "heart" to "Сердце", "bolid" to "Болид",
    "gg" to "Ржу", "fire" to "Горим", "flag" to "Финиш", "trophy" to "Победа", "kart" to "Поехали",
    "like" to "Класс", "love" to "Люблю", "angry" to "Злюсь", "sleep" to "Сплю", "stopwatch" to "Время",
)

/** Анимированный стикер: спрайт загружается один раз, кадры (по 70 мс) сменяются сдвигом картинки внутри обрезанного квадрата. */
@Composable
fun StickerView(name: String, size: Dp, modifier: Modifier = Modifier) {
    var frame by remember(name) { mutableIntStateOf(0) }
    LaunchedEffect(name) { while (true) { delay(70); frame = (frame + 1) % 16 } }
    Box(modifier.size(size).clipToBounds()) {
        AsyncImage(
            model = "$BASE_URL/icons/stickers/$name.webp?v=2", contentDescription = null, contentScale = ContentScale.FillBounds,
            // картинка в 4 раза больше окна и по центру: сдвигаем на (1,5 − столбец) и (1,5 − строка) размеров кадра
            modifier = Modifier.requiredSize(size * 4).offset { IntOffset(((1.5f - frame % 4) * size.toPx()).roundToInt(), ((1.5f - frame / 4) * size.toPx()).roundToInt()) },
        )
    }
}

@Composable
fun StickerSheet(onPick: (String) -> Unit, onClose: () -> Unit) {
    Sheet("Стикеры", onClose, half = true) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 8.dp)) {
            STICKERS.chunked(3).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { (name, _) ->
                        Box(
                            Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(Rc.Surface2).clickable { onPick(name) }.padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center,
                        ) { StickerView(name, 96.dp) }
                    }
                    repeat(3 - row.size) { Box(Modifier.weight(1f)) }
                }
            }
        }
    }
}
