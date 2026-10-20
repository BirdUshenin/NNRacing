# NN Racing — приложение для Android и iOS (Kotlin Multiplatform)

Тонкая оболочка над сайтом https://racing.ilyaushenin.ru: Compose Multiplatform + WebView (Android) / WKWebView (iOS).
Весь функционал (вход, заезды, друзья, чат) живёт на сервере и в сайте, поэтому обновления сайта доходят до приложений сразу.

- `composeApp/` — общий код (`App.kt`, `expect WebScreen`) и платформенные реализации (`androidMain`, `iosMain`).
- `iosApp/` — Xcode-проект (Kotlin-фреймворк собирается скриптом Gradle). Для телефона нужна подпись вашей командой в Xcode.
- Подпись Android: `keystore.properties` и ключ `~/.android-keys/nnracing.jks` (в репозиторий не входят; **сохраните копию**:
  без этого ключа обновить установленное приложение нельзя).

Сборка и публикация APK: `./publish_apk.sh` → https://racing.ilyaushenin.ru/download/nn-racing.apk
(кнопка «Скачать на Android» на экране входа сайта; внутри самого приложения она скрыта по метке `NNRacingApp` в User-Agent).
