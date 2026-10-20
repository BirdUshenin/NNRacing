package ru.ilyaushenin.nnracing.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import ru.ilyaushenin.nnracing.AppState
import ru.ilyaushenin.nnracing.data.ApiException
import ru.ilyaushenin.nnracing.ui.AppButton
import ru.ilyaushenin.nnracing.ui.AppField
import ru.ilyaushenin.nnracing.ui.AppLogo
import ru.ilyaushenin.nnracing.ui.AuthArt
import ru.ilyaushenin.nnracing.ui.BtnKind
import ru.ilyaushenin.nnracing.ui.FieldLabel
import ru.ilyaushenin.nnracing.ui.Ic
import ru.ilyaushenin.nnracing.ui.Rc
import ru.ilyaushenin.nnracing.ui.Segmented
import ru.ilyaushenin.nnracing.ui.TrackBackdrop
import ru.ilyaushenin.nnracing.ui.Txt
import ru.ilyaushenin.nnracing.ui.rememberImagePicker

@Composable
fun AuthScreen(app: AppState) {
    var register by remember { mutableStateOf(false) }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var invite by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var avatar by remember { mutableStateOf<ByteArray?>(null) }
    val pickAvatar = rememberImagePicker { avatar = it }
    val scope = rememberCoroutineScope()

    fun submit() {
        if (busy) return
        error = ""; busy = true
        scope.launch {
            try {
                var u = if (register) app.api.register(username, password, name, invite.ifBlank { null }) else app.api.login(username, password)
                if (register && avatar != null) runCatching { u = app.api.setAvatar(avatar!!) }
                app.signedIn(u)
            } catch (e: ApiException) { error = e.message }
            busy = false
        }
    }

    Box(Modifier.fillMaxSize()) {
        TrackBackdrop()
        Column(
            Modifier.fillMaxSize().statusBarsPadding().imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            val shape = RoundedCornerShape(28.dp)
            Column(
                Modifier.fillMaxWidth().clip(shape).background(Color(0x8C101015)).border(1.dp, Rc.LineStrong, shape).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Column(Modifier.fillMaxWidth().padding(bottom = 6.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    AppLogo(72.dp)
                    Txt("NN Racing", size = 24.sp, weight = FontWeight.ExtraBold)
                }
                Segmented(listOf("Вход", "Регистрация"), if (register) 1 else 0, { register = it == 1; error = "" })
                Column { FieldLabel("Логин"); AppField(username, { username = it }, placeholder = "pilot.ivan", imeAction = ImeAction.Next) }
                Column { FieldLabel("Пароль"); AppField(password, { password = it }, placeholder = "••••••", password = true, imeAction = if (register) ImeAction.Next else ImeAction.Go, onAction = { if (!register) submit() }) }
                if (register) {
                    if (app.inviteRequired) Column { FieldLabel("Код приглашения"); AppField(invite, { invite = it }, placeholder = "Код приглашения") }
                    Column { FieldLabel("Как вас зовут"); AppField(name, { name = it }, placeholder = "Иван", maxLength = 40) }
                    AppButton(if (avatar == null) "Выбрать аватарку" else "Аватарка выбрана ✓", pickAvatar, Modifier.fillMaxWidth(), BtnKind.Ghost, icon = Ic.Camera)
                }
                if (error.isNotEmpty()) Txt(error, color = Rc.Bad, size = 14.sp)
                AppButton(if (register) "Создать аккаунт" else "Войти", ::submit, Modifier.fillMaxWidth(), enabled = !busy)
            }
            Spacer(Modifier.height(8.dp))
            Txt("", align = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
    }
}
