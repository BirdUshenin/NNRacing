package ru.ilyaushenin.nnracing.ui

/** На iPhone push из собственного приложения требует платного аккаунта Apple Developer: пока без уведомлений. */
actual object PushBridge {
    actual fun enable(onDenied: () -> Unit, onToken: (String) -> Unit) = Unit
    actual val supported: Boolean = false
    actual fun permissionGranted(): Boolean = false
    actual fun openSettings() = Unit
}
