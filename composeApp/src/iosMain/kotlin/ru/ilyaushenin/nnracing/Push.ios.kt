package ru.ilyaushenin.nnracing.ui

/** На iPhone push из собственного приложения требует платного аккаунта Apple Developer: пока без уведомлений. */
actual object PushBridge {
    actual fun enable(onToken: (String) -> Unit) = Unit
}
