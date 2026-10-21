package ru.ilyaushenin.nnracing.ui

actual object PushBridge {
    /** Результат запроса токена доставляется сюда; активность подставляет себя в [requester]. */
    var requester: (() -> Unit)? = null
    var permissionCheck: (() -> Boolean)? = null
    var settingsOpener: (() -> Unit)? = null
    private var callback: ((String) -> Unit)? = null
    private var deniedCallback: (() -> Unit)? = null

    actual fun enable(onDenied: () -> Unit, onToken: (String) -> Unit) {
        callback = onToken
        deniedCallback = onDenied
        requester?.invoke()
    }

    actual val supported: Boolean = true
    actual fun permissionGranted(): Boolean = permissionCheck?.invoke() ?: true
    actual fun openSettings() { settingsOpener?.invoke() }

    fun deliver(token: String) { callback?.invoke(token) }
    fun denied() { deniedCallback?.invoke() }
}
