package ru.ilyaushenin.nnracing.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.ilyaushenin.nnracing.AppState
import ru.ilyaushenin.nnracing.data.BASE_URL
import ru.ilyaushenin.nnracing.ui.AppButton
import ru.ilyaushenin.nnracing.ui.Ic
import ru.ilyaushenin.nnracing.ui.Icon24
import ru.ilyaushenin.nnracing.ui.PlatformBackHandler
import ru.ilyaushenin.nnracing.ui.Rc
import ru.ilyaushenin.nnracing.ui.Txt
import ru.ilyaushenin.nnracing.ui.currentAppVersion
import ru.ilyaushenin.nnracing.ui.rememberUrlOpener

/**
 * Окно «Доступна новая версия»: появляется, когда на сервере включён флаг и его номер выше установленной версии.
 * Необязательное можно отложить («Позже»), обязательное закрыть нельзя.
 */
@Composable
fun UpdatePrompt(app: AppState) {
    val info = app.update ?: return
    val current = currentAppVersion()
    if (info.version <= current) return
    if (!info.required && app.updateDismissed >= info.version) return
    val open = rememberUrlOpener()
    // «назад» для обязательного обновления ничего не закрывает; для необязательного — откладывает
    PlatformBackHandler(enabled = true) { if (!info.required) app.updateDismissed = info.version }
    Box(
        Modifier.fillMaxSize().background(Color(0xCC05050A)).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
        contentAlignment = Alignment.Center,
    ) {
        val shape = RoundedCornerShape(28.dp)
        Column(
            Modifier.padding(horizontal = 28.dp).fillMaxWidth().clip(shape).background(Rc.Surface).border(1.dp, Rc.LineStrong, shape).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(Modifier.size(64.dp).clip(RoundedCornerShape(20.dp)).background(Rc.Surface3), contentAlignment = Alignment.Center) { Icon24(Ic.Download, Rc.Accent, 32.dp, strokeWidth = 2.2f) }
            Txt("Доступна новая версия", size = 22.sp, weight = FontWeight.ExtraBold, align = TextAlign.Center)
            Txt(info.message.ifBlank { "Мы улучшили приложение. Скачайте обновление, чтобы пользоваться всеми новыми возможностями." }, color = Rc.Muted, size = 15.sp, align = TextAlign.Center)
            AppButton("Скачать", { open(if (info.url.startsWith("http")) info.url else BASE_URL + info.url) }, Modifier.fillMaxWidth().padding(top = 6.dp))
            if (!info.required) Txt(
                "Позже", color = Rc.Muted, size = 16.sp, weight = FontWeight.SemiBold,
                modifier = Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { app.updateDismissed = info.version }.padding(10.dp),
            )
        }
    }
}
