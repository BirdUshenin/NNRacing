package ru.ilyaushenin.nnracing.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Выбор картинки из галереи; результат — квадратный JPEG 256×256 (аватарка). Возвращает функцию «открыть выбор». */
@Composable
expect fun rememberImagePicker(onPicked: (ByteArray) -> Unit): () -> Unit

/** Системная кнопка «назад» (Android). На iOS ничего не делает: там жест от левого края. */
@Composable
expect fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit)

/** true на iOS: возврат свайпом от левого края с «живой» анимацией, как в App Store. На Android работает системная кнопка «назад». */
expect val swipeBackEnabled: Boolean

/** Дополнительный отступ сверху у страниц: на Android под системной строкой состояния контент выглядел слишком высоко, на iOS отступ уже достаточный. */
expect val pageTopExtraDp: Int

/** Номер версии установленного приложения (versionCode на Android); на iPhone окно обновления не показывается. */
@Composable
expect fun currentAppVersion(): Int

/** Открывает ссылку во внешнем приложении (браузер, загрузчик). */
@Composable
expect fun rememberUrlOpener(): (String) -> Unit

/** Показывать ли «что нового» при первом запуске новой версии: пока только на Android. */
expect val onboardingEnabled: Boolean

/** Фото для чата: уменьшено до 1600 px по длинной стороне и сжато в JPEG (на сервер уходит до 3 МБ). */
class PickedPhoto(val bytes: ByteArray, val width: Int, val height: Int)

@Composable
expect fun rememberPhotoPicker(onPicked: (PickedPhoto) -> Unit): () -> Unit
