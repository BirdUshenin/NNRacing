package ru.ilyaushenin.nnracing.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch
import ru.ilyaushenin.nnracing.AppState
import ru.ilyaushenin.nnracing.data.BASE_URL
import ru.ilyaushenin.nnracing.ui.Ic
import ru.ilyaushenin.nnracing.ui.Icon24
import ru.ilyaushenin.nnracing.ui.PlatformBackHandler
import ru.ilyaushenin.nnracing.ui.Rc
import ru.ilyaushenin.nnracing.ui.Txt

private class Slide(val glow: Color, val title: String, val text: String, val art: @Composable () -> Unit)

@Composable
private fun Reaction(key: String) {
    if (key.startsWith(":")) AsyncImage(model = "$BASE_URL/icons/reactions/${key.trim(':')}.png?v=3", contentDescription = null, modifier = Modifier.size(54.dp))
    else Txt(key, size = 40.sp)
}

private val SLIDES = listOf(
    Slide(Color(0xFFFFC83A), "Достижения", "Получайте кубки за первое, второе и третье место на каждой трассе!") {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(model = "$BASE_URL/icons/trophies/2-f-drive.webp?v=6", contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.size(width = 110.dp, height = 190.dp))
            AsyncImage(model = "$BASE_URL/icons/trophies/1-academy-speedway.webp?v=6", contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.size(width = 150.dp, height = 250.dp))
            AsyncImage(model = "$BASE_URL/icons/trophies/3-nring.webp?v=6", contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.size(width = 110.dp, height = 190.dp))
        }
    },
    Slide(Color(0xFFFF3B24), "Свечение профиля", "Для активации первого свечения необходимо поставить фото профиля, а затем выбрать цвет. С ростом числа заездов открываются новые огни.") {
        AsyncImage(model = "$BASE_URL/icons/trophies/runs-100.png?v=6", contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.size(width = 190.dp, height = 240.dp))
    },
    Slide(Color(0xFF3D8BFF), "Уведомления", "О новых сообщениях, приглашениях на заезды и заявках в друзья. Включить и выключить можно в настройках профиля.") {
        Box(Modifier.size(150.dp).clip(RoundedCornerShape(44.dp)).background(Rc.Surface3), contentAlignment = Alignment.Center) { Icon24(Ic.Bell, Color.White, 84.dp, strokeWidth = 1.7f) }
    },
    Slide(Color(0xFF19D9C0), "Стикеры и опросы", "Анимированные стикеры, добавление фото и опросов") {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            StickerView("horse", 110.dp)
            StickerView("heart", 130.dp)
            StickerView("bolid", 110.dp)
        }
    },
    Slide(Color(0xFFFF4FA3), "Реакции", "Зажмите сообщение и выберите реакцию. А двойной тап по сообщению сразу ставит ❤️. Свайп влево — ответ.") {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) { Reaction("❤️"); Reaction("🔥"); Reaction("👍") }
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) { Reaction(":ferrari:"); Reaction(":helmet-hamilton:"); Reaction(":flags:") }
        }
    },
)

/** Экран «что нового» при первом запуске новой версии: по одной теме на странице, листается свайпом. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingLayer(app: AppState) {
    if (!app.showOnboarding) return
    val pager = rememberPagerState { SLIDES.size }
    val scope = rememberCoroutineScope()
    val last = pager.currentPage == SLIDES.lastIndex
    PlatformBackHandler(enabled = true) { app.finishOnboarding() }
    Box(Modifier.fillMaxSize().background(Rc.Bg).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}) {
        val glow = SLIDES[pager.currentPage].glow
        // цвет неона за картинкой меняется вместе со страницей
        Box(Modifier.fillMaxSize().drawBehind {
            val c = Offset(size.width / 2, size.height * 0.34f)
            drawCircle(Brush.radialGradient(listOf(glow.copy(alpha = 0.40f), glow.copy(alpha = 0.14f), Color.Transparent), center = c, radius = size.width * 0.85f), radius = size.width * 0.85f, center = c)
        })
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Txt("Что нового", color = Rc.Muted, size = 14.sp, weight = FontWeight.ExtraBold)
                if (!last) Txt("Пропустить", color = Rc.Muted, size = 15.sp, weight = FontWeight.SemiBold, modifier = Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { app.finishOnboarding() }.padding(6.dp))
            }
            HorizontalPager(pager, Modifier.weight(1f).fillMaxWidth()) { page ->
                val s = SLIDES[page]
                Column(Modifier.fillMaxSize().padding(horizontal = 30.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Box(Modifier.weight(1.2f).fillMaxWidth(), contentAlignment = Alignment.Center) { s.art() }
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Txt(s.title, size = 28.sp, weight = FontWeight.ExtraBold, align = TextAlign.Center)
                        Txt(s.text, color = Rc.Muted, size = 16.sp, align = TextAlign.Center)
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(bottom = 18.dp), horizontalArrangement = Arrangement.Center) {
                repeat(SLIDES.size) { i ->
                    Box(Modifier.padding(horizontal = 4.dp).size(if (i == pager.currentPage) 22.dp else 8.dp, 8.dp).clip(CircleShape).background(if (i == pager.currentPage) Color.White else Color.White.copy(alpha = 0.25f)))
                }
            }
            Box(Modifier.fillMaxWidth().padding(bottom = 22.dp), contentAlignment = Alignment.Center) {
                Txt(
                    if (last) "Начать" else "Далее", color = Color.White, size = 18.sp, weight = FontWeight.Bold,
                    modifier = Modifier.clip(RoundedCornerShape(16.dp)).border(1.5.dp, Color.White, RoundedCornerShape(16.dp))
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                            if (last) app.finishOnboarding() else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
                        }.padding(horizontal = 44.dp, vertical = 14.dp),
                )
            }
        }
    }
}
