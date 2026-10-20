package ru.ilyaushenin.nnracing.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import ru.ilyaushenin.nnracing.data.BASE_URL

val AccentBrush get() = Brush.linearGradient(listOf(Rc.Accent, Rc.Accent2))

@Composable
fun Txt(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Rc.Text,
    size: TextUnit = 16.sp,
    weight: FontWeight = FontWeight.Normal,
    align: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    mono: Boolean = false,
) {
    Text(
        text, modifier = modifier, color = color, fontSize = size, fontWeight = weight, textAlign = align,
        maxLines = maxLines, overflow = TextOverflow.Ellipsis, letterSpacing = letterSpacing,
        style = if (mono) TextStyle(fontFeatureSettings = "tnum") else TextStyle.Default,
    )
}

/** Заголовок раздела: «БЛИЖАЙШИЕ ЗАЕЗДЫ». */
@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) =
    Txt(text.uppercase(), modifier, Rc.Muted, 13.sp, FontWeight.Bold, letterSpacing = 1.5.sp)

@Composable
fun Eyebrow(text: String, modifier: Modifier = Modifier) =
    Txt(text.uppercase(), modifier, Rc.Accent, 12.sp, FontWeight.ExtraBold, letterSpacing = 2.sp)

@Composable
fun PageTitle(text: String, modifier: Modifier = Modifier) = Txt(text, modifier, size = 30.sp, weight = FontWeight.ExtraBold)

@Composable
fun Card(
    modifier: Modifier = Modifier,
    padding: Dp = 16.dp,
    border: Color = Rc.Line,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(22.dp)
    Column(
        modifier.fillMaxWidth().clip(shape)
            .background(Brush.verticalGradient(listOf(Rc.Surface2, Rc.Surface)))
            .border(1.dp, border, shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(padding),
        content = content,
    )
}

@Composable
fun Chip(text: String, modifier: Modifier = Modifier, color: Color = Rc.Muted, background: Color = Rc.Surface3, onClick: (() -> Unit)? = null) {
    Box(
        modifier.clip(RoundedCornerShape(50)).background(background)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 10.dp, vertical = 5.dp),
    ) { Txt(text, color = color, size = 12.sp, weight = FontWeight.Bold, mono = true) }
}

@Composable
fun BestChip(text: String, modifier: Modifier = Modifier) = Chip(text, modifier, Rc.Best, Rc.Best.copy(alpha = 0.16f))

enum class BtnKind { Primary, Ghost, Danger }

@Composable
fun AppButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    kind: BtnKind = BtnKind.Primary,
    small: Boolean = false,
    enabled: Boolean = true,
    icon: String? = null,
) {
    val shape = RoundedCornerShape(if (small) 12.dp else 16.dp)
    val (fg, bgMod) = when (kind) {
        BtnKind.Primary -> Color.White to Modifier.background(AccentBrush)
        BtnKind.Ghost -> Rc.Text to Modifier.background(Rc.Surface3)
        BtnKind.Danger -> Rc.Bad to Modifier.background(Rc.Bad.copy(alpha = 0.12f))
    }
    Row(
        modifier.height(if (small) 38.dp else 50.dp).clip(shape).then(bgMod)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = if (small) 14.dp else 22.dp),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) { Icon24(icon, fg, if (small) 16.dp else 20.dp); Spacer(Modifier.width(8.dp)) }
        Txt(text, color = if (enabled) fg else fg.copy(alpha = 0.5f), size = if (small) 14.sp else 16.sp, weight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
fun IconBtn(path: String, onClick: () -> Unit, modifier: Modifier = Modifier, tint: Color = Rc.Text, background: Color = Rc.Surface3, size: Dp = 42.dp) {
    Box(
        modifier.size(size).clip(RoundedCornerShape(14.dp)).background(background).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon24(path, tint, 20.dp) }
}

@Composable
fun Avatar(name: String, url: String?, size: Dp = 44.dp, modifier: Modifier = Modifier, guest: Boolean = false) {
    val shape = RoundedCornerShape(size * 0.34f)
    Box(
        modifier.size(size).clip(shape)
            .then(if (guest) Modifier.background(Rc.Surface3) else Modifier.background(AccentBrush)),
        contentAlignment = Alignment.Center,
    ) {
        if (url != null) {
            AsyncImage(model = BASE_URL + url, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            Txt(name.trim().take(1).uppercase().ifEmpty { "?" }, color = if (guest) Rc.Muted else Color.White, size = (size.value * 0.4f).sp, weight = FontWeight.ExtraBold)
        }
    }
}

/** Текстовое поле в стиле сайта. */
@Composable
fun AppField(
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    password: Boolean = false,
    keyboard: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Default,
    onAction: (() -> Unit)? = null,
    singleLine: Boolean = true,
    height: Dp = 52.dp,
    maxLength: Int = Int.MAX_VALUE,
    textAlign: TextAlign = TextAlign.Start,
) {
    var focused by remember { mutableStateOf(false) }
    var shown by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(16.dp)
    BasicTextField(
        value = value,
        onValueChange = { if (it.length <= maxLength) onChange(it) },
        modifier = modifier.fillMaxWidth().defaultMinSize(minHeight = height).clip(shape).background(Rc.Surface)
            .border(1.dp, if (focused) Rc.Accent.copy(alpha = 0.7f) else Rc.Line, shape)
            .onFocusChanged { focused = it.isFocused },
        singleLine = singleLine,
        textStyle = TextStyle(color = Rc.Text, fontSize = 16.sp, textAlign = textAlign),
        cursorBrush = SolidColor(Rc.Accent),
        visualTransformation = if (password && !shown) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = keyboard, imeAction = imeAction),
        keyboardActions = KeyboardActions(onAny = { onAction?.invoke() }),
        decorationBox = { inner ->
            Row(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f), contentAlignment = Alignment.TopStart, propagateMinConstraints = true) {
                    if (value.isEmpty()) Txt(placeholder, color = Rc.Hint, align = textAlign, modifier = if (textAlign == TextAlign.Center) Modifier.fillMaxWidth() else Modifier)
                    inner()
                }
                if (password) Box(Modifier.clickableNoRipple { shown = !shown }.padding(start = 10.dp)) { Icon24(if (shown) Ic.EyeOff else Ic.Eye, Rc.Muted, 22.dp) }
            }
        },
    )
}

@Composable
fun FieldLabel(text: String, modifier: Modifier = Modifier) = Txt(text.uppercase(), modifier.padding(bottom = 6.dp), Rc.Muted, 12.sp, FontWeight.Bold, letterSpacing = 1.2.sp)

/** Переключатель «Вход / Регистрация», «Прямая / Реверс» и т. п. */
@Composable
fun Segmented(options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(16.dp)
    Row(modifier.fillMaxWidth().clip(shape).background(Rc.Surface).border(1.dp, Rc.Line, shape).padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        options.forEachIndexed { i, label ->
            Box(
                Modifier.weight(1f).height(40.dp).clip(RoundedCornerShape(12.dp))
                    .background(if (i == selected) Rc.Surface3 else Color.Transparent)
                    .clickable { onSelect(i) },
                contentAlignment = Alignment.Center,
            ) { Txt(label, color = if (i == selected) Rc.Text else Rc.Muted, size = 14.sp, weight = FontWeight.Bold, maxLines = 1) }
        }
    }
}

/** Нижняя шторка с заголовком и крестиком. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Sheet(title: String, onClose: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onClose, sheetState = state,
        containerColor = Rc.Surface, contentColor = Rc.Text, scrimColor = Color(0x9E000000),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp).navigationBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(bottom = 14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Txt(title, size = 22.sp, weight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
                IconBtn(Ic.X, onClose)
            }
            Column(Modifier.fillMaxWidth().imePadding().verticalScroll(rememberScrollState())) {
                content()
                Spacer(Modifier.height(18.dp))
            }
        }
    }
}

@Composable
fun Divider() = Box(Modifier.fillMaxWidth().height(1.dp).background(Rc.Line))

@Composable
fun Empty(text: String, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit = {}) {
    Card(modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(vertical = 18.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Txt(text, color = Rc.Muted, align = TextAlign.Center)
            content()
        }
    }
}

@Composable
fun Skeleton(modifier: Modifier = Modifier, height: Dp = 120.dp) {
    Box(modifier.fillMaxWidth().height(height).clip(RoundedCornerShape(22.dp)).background(Rc.Surface))
}

@Composable
fun Medal(position: Int?, text: String, size: TextUnit = 20.sp, modifier: Modifier = Modifier) {
    Txt(text, modifier, color = Rc.medal(position) ?: Rc.Text, size = size, weight = FontWeight.ExtraBold, mono = true)
}

@Composable
fun Dot(color: Color, size: Dp = 8.dp) = Box(Modifier.size(size).clip(CircleShape).background(color))

@Composable
fun FullBox(content: @Composable () -> Unit) = Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { content() }

/** Нажатие без ряби и подсветки. */
fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier = this.clickable(
    interactionSource = MutableInteractionSource(), indication = null, onClick = onClick,
)

@Composable
fun ConfirmSheet(title: String, text: String, action: String, onConfirm: () -> Unit, onClose: () -> Unit) {
    Sheet(title, onClose) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Txt(text, color = Rc.Muted)
            AppButton(action, onConfirm, Modifier.fillMaxWidth(), BtnKind.Danger)
            AppButton("Отмена", onClose, Modifier.fillMaxWidth(), BtnKind.Ghost)
        }
    }
}

/** Выбираемая «таблетка»: трасса, погода и т. п. */
@Composable
fun PickChip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(50)
    Box(
        modifier.clip(shape).background(if (selected) Rc.Accent.copy(alpha = 0.16f) else Rc.Surface3)
            .border(1.dp, if (selected) Rc.Accent.copy(alpha = 0.5f) else Rc.Line, shape)
            .clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 9.dp),
    ) { Txt(text, color = if (selected) Rc.Accent2 else Rc.Muted, size = 14.sp, weight = FontWeight.Bold, maxLines = 1) }
}

/** Кнопка-поле: показывает значение и открывает выбор (дата, время). */
@Composable
fun PickerField(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, placeholder: Boolean = false) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier.fillMaxWidth().height(52.dp).clip(shape).background(Rc.Surface).border(1.dp, Rc.Line, shape).clickable(onClick = onClick).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) { Txt(text, color = if (placeholder) Rc.Hint else Rc.Text) }
}
