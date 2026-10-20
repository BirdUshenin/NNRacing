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
