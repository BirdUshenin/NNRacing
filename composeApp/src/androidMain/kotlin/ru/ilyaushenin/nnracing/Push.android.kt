package ru.ilyaushenin.nnracing.ui

actual object PushBridge {
    /** Результат запроса токена доставляется сюда; активность подставляет себя в [requester]. */
    var requester: (() -> Unit)? = null
    private var callback: ((String) -> Unit)? = null

    actual fun enable(onToken: (String) -> Unit) {
        callback = onToken
        requester?.invoke()
    }

    fun deliver(token: String) { callback?.invoke(token) }
}
