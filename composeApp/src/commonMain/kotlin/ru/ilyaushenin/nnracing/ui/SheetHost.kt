package ru.ilyaushenin.nnracing.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

/**
 * Нижние шторки рисуются прямо в дереве приложения, а не системными слоями (ModalBottomSheet/Dialog):
 * на iPhone такие слои после закрытия оставляли «призрак», который мелькал при показе следующего окна,
 * а высокая шторка заезжала под статус-бар. Здесь шторка всегда плавно уезжает перед удалением и не выходит за безопасную область.
 */
class SheetEntry(var title: String, var onClose: () -> Unit, var half: Boolean, var content: @Composable ColumnScope.() -> Unit) {
    var open by mutableStateOf(true)
}

class SheetHost {
    internal val entries = mutableStateListOf<SheetEntry>()
}

val LocalSheetHost = compositionLocalOf<SheetHost> { error("SheetHost не подключён") }

/** Нижняя шторка с заголовком и крестиком; закрывается крестиком, нажатием на затемнение, свайпом вниз и кнопкой «назад». */
@Composable
fun Sheet(title: String, onClose: () -> Unit, half: Boolean = false, content: @Composable ColumnScope.() -> Unit) {
    val host = LocalSheetHost.current
    val entry = remember { SheetEntry(title, onClose, half, content) }
    SideEffect { entry.title = title; entry.onClose = onClose; entry.content = content }
    // шторка живёт, пока вызывающий экран её показывает; после удаления её анимированно убирает слой SheetLayer
    DisposableEffect(entry) {
        host.entries.add(entry)
        onDispose { entry.open = false }
    }
}

@Composable
fun SheetLayer(host: SheetHost) {
    host.entries.forEach { entry -> key(entry) { SheetView(entry) { host.entries.remove(entry) } } }
}

@Composable
private fun SheetView(entry: SheetEntry, onGone: () -> Unit) {
    val progress = remember { Animatable(0f) }
    val drag = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    var height by remember { mutableIntStateOf(0) }
    // «половинная» шторка (стикеры) открывается на 55% высоты экрана; потянув за ручку вверх, её можно раскрыть почти целиком
    var screenH by remember { mutableIntStateOf(0) }
    val fraction = remember { Animatable(0.55f) }
    var expanded by remember { mutableStateOf(false) }
    LaunchedEffect(entry.open) {
        if (entry.open) progress.animateTo(1f, tween(300, easing = FastOutSlowInEasing))
        else { progress.animateTo(0f, tween(220, easing = FastOutSlowInEasing)); onGone() }
    }
    PlatformBackHandler(enabled = entry.open) { entry.onClose() }
    Box(Modifier.fillMaxSize().onSizeChanged { screenH = it.height }) {
        Box(
            Modifier.fillMaxSize().background(Color(0xFF000000).copy(alpha = 0.62f * progress.value))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { if (entry.open) entry.onClose() },
        )
        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().statusBarsPadding().padding(top = 28.dp).imePadding()   // и при полном раскрытии шторка остаётся ниже статус-бара
                .then(if (entry.half) Modifier.height(with(androidx.compose.ui.platform.LocalDensity.current) { (screenH * fraction.value).toDp() }) else Modifier)
                .graphicsLayer { translationY = if (height == 0) 20000f else (1f - progress.value) * height + drag.value }
                .onSizeChanged { height = it.height }
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)).background(Rc.Surface)
                // нажатия внутри шторки не должны доходить до затемнения
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
        ) {
            // верх шторки (ручка и заголовок): за него шторку можно потянуть вниз, чтобы закрыть
            Column(
                Modifier.fillMaxWidth().pointerInput(Unit) {
                    var total = 0f
                    detectVerticalDragGestures(
                        onDragStart = { total = 0f },
                        onDragEnd = {
                            scope.launch {
                                when {
                                    // вверх за ручку: раскрыть; вниз у раскрытой — вернуть к половине; вниз у половинной — закрыть
                                    entry.half && !expanded && total < -40.dp.toPx() -> { expanded = true; fraction.animateTo(0.88f, tween(250)) }
                                    entry.half && expanded && total > 60.dp.toPx() -> { expanded = false; fraction.animateTo(0.55f, tween(250)); drag.animateTo(0f, tween(180)) }
                                    drag.value > 90.dp.toPx() && !expanded -> entry.onClose()
                                    else -> drag.animateTo(0f, tween(180))
                                }
                            }
                        },
                        onDragCancel = { scope.launch { drag.animateTo(0f, tween(180)) } },
                    ) { change, amount ->
                        change.consume(); total += amount
                        if (!expanded) scope.launch { drag.snapTo((drag.value + amount).coerceAtLeast(0f)) }
                    }
                },
            ) {
                Box(Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 6.dp), contentAlignment = Alignment.Center) {
                    Box(Modifier.size(width = 40.dp, height = 4.dp).clip(CircleShape).background(Rc.LineStrong))
                }
                Row(Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, top = 6.dp, bottom = 14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Txt(entry.title, size = 22.sp, weight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
                    IconBtn(Ic.X, { if (entry.open) entry.onClose() })
                }
            }
            Column(Modifier.then(if (entry.half) Modifier.weight(1f) else Modifier).fillMaxWidth().padding(horizontal = 18.dp).navigationBarsPadding().verticalScroll(rememberScrollState())) {
                entry.content(this)
                Spacer(Modifier.height(18.dp))
            }
        }
    }
}
