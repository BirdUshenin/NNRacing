package ru.ilyaushenin.nnracing.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Отступ снизу под нижнюю панель: сама панель (≈76 dp) + системная навигация + запас, чтобы последний элемент можно было прокрутить выше неё. */
@Composable
fun bottomBarClearance(extra: Int = 28): androidx.compose.ui.unit.Dp =
    76.dp + extra.dp + androidx.compose.foundation.layout.WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

/** Прокручиваемая страница с отступами под статус-бар и нижнюю навигацию. */
@Composable
fun Page(modifier: Modifier = Modifier, bottomSpace: Int = 28, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(start = 18.dp, end = 18.dp, top = (18 + pageTopExtraDp).dp, bottom = bottomBarClearance(bottomSpace)),
        content = content,
    )
}

@Composable
fun BackLink(label: String, onClick: () -> Unit) {
    Row(Modifier.padding(bottom = 8.dp).clickableNoRipple(onClick), verticalAlignment = Alignment.CenterVertically) {
        Icon24(Ic.Back, Rc.Muted, 18.dp)
        Txt(label, color = Rc.Muted, weight = androidx.compose.ui.text.font.FontWeight.Bold, modifier = Modifier.padding(start = 6.dp))
    }
}

/** Загрузка с заглушками и кнопкой «Повторить» при ошибке. */
@Composable
fun <T> LoadedContent(loaded: Loaded<T>, content: @Composable (T) -> Unit) {
    val v = loaded.value
    when {
        v != null -> content(v)
        loaded.error != null -> Empty(loaded.error) { AppButton("Повторить", loaded.reload, kind = BtnKind.Ghost) }
        else -> Box(Modifier.fillMaxWidth().padding(vertical = 60.dp), contentAlignment = Alignment.Center) { LoadingLogo(64.dp) }
    }
}

