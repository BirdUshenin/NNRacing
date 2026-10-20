package ru.ilyaushenin.nnracing.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Время одного круга в виде трёх частей: минуты, секунды, сотые. */
@Stable
class LapInput(m: String = "", s: String = "", c: String = "") {
    var m by mutableStateOf(m)
    var s by mutableStateOf(s)
    var c by mutableStateOf(c)

    val isEmpty get() = m.isEmpty() && s.isEmpty() && c.isEmpty()

    /** Миллисекунды; null, если поле пустое. Сотые дописываются справа: «5» это «.50». */
    fun toMs(): Int? {
        if (isEmpty) return null
        return (m.ifEmpty { "0" }.toInt() * 60 + s.ifEmpty { "0" }.toInt()) * 1000 + c.padEnd(2, '0').toInt() * 10
    }

    /** Ошибка ввода или null: секунд не больше 59, круг не короче 5 секунд. */
    fun error(): String? {
        val ms = toMs() ?: return null
        if (s.ifEmpty { "0" }.toInt() > 59) return "секунд не больше 59"
        if (ms < 5000) return "круг короче 5 секунд"
        return null
    }

    companion object {
        fun fromMs(ms: Int): LapInput {
            val cs = (ms + 5) / 10
            fun p(n: Int) = n.toString().padStart(2, '0')
            return LapInput(p(cs / 6000), p(cs / 100 % 60), p(cs % 100))
        }
    }
}

private val Mask = Color(0xFF5B5B68)

/** Поле времени 00m : 00s : 00ms — каждая часть отдельная, две цифры, и курсор сам прыгает дальше. */
@Composable
fun LapTimeField(lap: LapInput, modifier: Modifier = Modifier, onDone: () -> Unit = {}) {
    val requesters = remember { List(3) { FocusRequester() } }
    val shape = RoundedCornerShape(16.dp)
    val bad = lap.error() != null && lap.c.length == 2
    // активна только одна часть: курсор и подсветка рисуются у неё одной
    var active by remember { mutableStateOf<Int?>(null) }
    Row(
        modifier.fillMaxWidth().height(52.dp).clip(shape).background(Rc.Surface)
            .border(1.dp, if (bad) Rc.Bad else if (active != null) Rc.Accent.copy(alpha = 0.7f) else Rc.Line, shape).padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
    ) {
        Part(lap.m, "m", requesters, 0, active, { idx, f -> if (f) active = idx else if (active == idx) active = null }, { lap.m = it }, onDone, padLeft = true)
        Txt(":", color = Rc.Muted, size = 20.sp, modifier = Modifier.padding(horizontal = 2.dp))
        Part(lap.s, "s", requesters, 1, active, { idx, f -> if (f) active = idx else if (active == idx) active = null }, { lap.s = it }, onDone, padLeft = true)
        Txt(":", color = Rc.Muted, size = 20.sp, modifier = Modifier.padding(horizontal = 2.dp))
        Part(lap.c, "ms", requesters, 2, active, { idx, f -> if (f) active = idx else if (active == idx) active = null }, { lap.c = it }, onDone, padLeft = false)
    }
}

@Composable
private fun Part(
    value: String, unit: String, requesters: List<FocusRequester>, k: Int,
    active: Int?, onActive: (Int, Boolean) -> Unit, onChange: (String) -> Unit, onDone: () -> Unit, padLeft: Boolean,
) {
    var tf by remember { mutableStateOf(TextFieldValue(value)) }
    val focused = active == k
    if (tf.text != value && !focused) tf = TextFieldValue(value)
    LaunchedEffect(focused) { if (focused) tf = tf.copy(selection = TextRange(0, tf.text.length)) }
    Row(
        Modifier.clip(RoundedCornerShape(10.dp)).background(if (focused) Rc.Accent.copy(alpha = 0.16f) else Color.Transparent)
            .padding(horizontal = 4.dp, vertical = 5.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        BasicTextField(
            value = tf,
            onValueChange = { new ->
                val digits = new.text.filter { it.isDigit() }.take(2)
                tf = new.copy(text = digits, selection = TextRange(digits.length))
                onChange(digits)
                if (digits.length == 2 && k < 2) requesters[k + 1].requestFocus()
            },
            modifier = Modifier.width(30.dp).focusRequester(requesters[k])
                .onFocusChanged {
                    onActive(k, it.isFocused)
                    if (!it.isFocused && tf.text.isNotEmpty()) {
                        val padded = if (padLeft) tf.text.padStart(2, '0') else tf.text.padEnd(2, '0')
                        if (padded != tf.text) { tf = TextFieldValue(padded); onChange(padded) }
                    }
                }
                .onPreviewKeyEvent { e ->
                    if (e.type == KeyEventType.KeyDown && e.key == Key.Backspace && tf.text.isEmpty() && k > 0) { requesters[k - 1].requestFocus(); true } else false
                },
            singleLine = true,
            textStyle = TextStyle(color = Rc.Text, fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, fontFeatureSettings = "tnum"),
            cursorBrush = SolidColor(if (focused) Rc.Accent else Color.Transparent),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = if (k < 2) ImeAction.Next else ImeAction.Done),
            keyboardActions = KeyboardActions(onNext = { requesters[k + 1].requestFocus() }, onDone = { onDone() }),
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.Center) {
                    if (tf.text.isEmpty()) Txt("00", color = Mask, size = 20.sp, weight = FontWeight.Bold, mono = true)
                    inner()
                }
            },
        )
        Txt(unit, color = Mask, size = 12.sp, weight = FontWeight.Bold, modifier = Modifier.padding(start = 1.dp, bottom = 3.dp))
    }
}
