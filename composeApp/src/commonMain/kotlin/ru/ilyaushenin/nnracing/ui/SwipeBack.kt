package ru.ilyaushenin.nnracing.ui

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import ru.ilyaushenin.nnracing.AppState
import ru.ilyaushenin.nnracing.Route

/**
 * Стек экранов с анимацией как в навигации iOS: новый экран выезжает справа, а возврат — свайпом от левого края,
 * где экран идёт за пальцем, предыдущий подъезжает с параллаксом и затемнением. На Android показывает верхний экран.
 */
@Composable
fun SwipeBackHost(app: AppState, render: @Composable (Route) -> Unit) {
    val stack = app.stack
    val top = stack.last()
    val prev = stack.getOrNull(stack.size - 2)
    if (!swipeBackEnabled) { render(top); return }

    val scope = rememberCoroutineScope()
    var widthPx by remember { mutableFloatStateOf(0f) }
    var offset by remember { mutableFloatStateOf(0f) }     // сдвиг верхнего экрана вправо, пикселей
    var lastSize by remember { mutableIntStateOf(stack.size) }
    var entering by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }

    // экран добавили в стек: он выезжает справа (возврат на вкладку стек сбрасывает, поэтому там анимации нет)
    if (stack.size != lastSize) {
        if (stack.size > lastSize && widthPx > 0f) { offset = widthPx; entering = true }
        lastSize = stack.size
    }
    LaunchedEffect(entering) {
        if (entering) { animate(widthPx, 0f, animationSpec = tween(320)) { v, _ -> offset = v }; entering = false }
    }

    Box(
        Modifier.fillMaxSize().onSizeChanged { widthPx = it.width.toFloat() }.pointerInput(stack.size) {
            if (stack.size <= 1) return@pointerInput
            val edge = 28.dp.toPx()
            val slop = viewConfiguration.touchSlop
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                if (down.position.x > edge || busy) return@awaitEachGesture
                val tracker = VelocityTracker()
                tracker.addPosition(down.uptimeMillis, down.position)
                var dragging = false
                while (true) {
                    val change = awaitPointerEvent(PointerEventPass.Initial).changes.firstOrNull { it.id == down.id } ?: break
                    tracker.addPosition(change.uptimeMillis, change.position)
                    val dx = change.position.x - down.position.x
                    if (!dragging && dx > slop) dragging = true
                    if (dragging) { offset = dx.coerceAtLeast(0f); change.consume() }
                    if (!change.pressed) {
                        if (dragging) {
                            val fast = tracker.calculateVelocity().x > 900f
                            val done = offset > widthPx * 0.35f || fast
                            busy = true
                            scope.launch {
                                animate(offset, if (done) widthPx else 0f, animationSpec = tween(if (done) 200 else 250)) { v, _ -> offset = v }
                                if (done) { app.pop(); offset = 0f }
                                busy = false
                            }
                        }
                        break
                    }
                }
            }
        },
    ) {
        if (prev != null && offset > 0f) {
            val p = (offset / widthPx).coerceIn(0f, 1f)
            Box(Modifier.fillMaxSize().graphicsLayer { translationX = -widthPx * 0.3f * (1f - p) }) { render(prev) }
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f * (1f - p))))
        }
        Box(Modifier.fillMaxSize().graphicsLayer { translationX = offset }.background(Rc.Bg)) { render(top) }
    }
}
