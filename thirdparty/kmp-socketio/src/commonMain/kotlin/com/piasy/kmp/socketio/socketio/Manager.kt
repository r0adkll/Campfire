package com.piasy.kmp.socketio.socketio

import com.piasy.kmp.socketio.emitter.Emitter
import com.piasy.kmp.socketio.engineio.EngineSocket
import com.piasy.kmp.socketio.engineio.On
import com.piasy.kmp.socketio.engineio.State
import com.piasy.kmp.socketio.engineio.WorkThread
import com.piasy.kmp.xlog.Logging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.hildan.socketio.EngineIOPacket
import kotlin.jvm.JvmField

class Manager(
    private val uri: String,
    private val opt: Options,
    private val scope: CoroutineScope,
) : Emitter() {
    open class Options : EngineSocket.Options() {
        internal lateinit var backoff: Backoff

        @JvmField
        var reconnection = true

        @JvmField
        var reconnectionAttempts: Int = Int.MAX_VALUE
        var reconnectionDelay: Long = 1000
            set(value) {
                if (::backoff.isInitialized) {
                    backoff.min = value
                }
                field = value
            }
        var reconnectionDelayMax: Long = 5000
            set(value) {
                if (::backoff.isInitialized) {
                    backoff.max = value
                }
                field = value
            }
        var randomizationFactor: Double = 0.5
            set(value) {
                if (::backoff.isInitialized) {
                    backoff.jitter = value
                }
                field = value
            }
        @JvmField
        var auth: Map<String, String> = emptyMap()

        /**
         * Connection timeout (ms). Set -1 to disable.
         */
        @JvmField
        var timeout: Long = 20000
    }

    internal var state = State.INIT
        private set
    private val subs = ArrayList<On.Handle>()
    internal var engine: EngineSocket? = null
    internal val nsps = HashMap<String, Socket>()

    private var skipReconnect = false
    internal var reconnecting = false
        private set

    init {
        if (opt.path.isEmpty()) {
            opt.path = "/socket.io/"
        }
        opt.backoff = Backoff(opt.reconnectionDelay, opt.reconnectionDelayMax, opt.randomizationFactor)
    }

    fun reconnectionDelay(delay: Long) {
        opt.reconnectionDelay = delay
    }

    fun reconnectionDelayMax(delayMax: Long) {
        opt.reconnectionDelayMax = delayMax
    }

    fun randomizationFactor(factor: Double) {
        opt.randomizationFactor = factor
    }

    /**
     * Connects the client.
     *
     * @param callback callback.
     */
    @WorkThread
    fun open(callback: ((String) -> Unit)? = null) {
        Logging.info(TAG, "open, state $state, uri $uri")
        if (state != State.INIT && state != State.CLOSED) {
            return
        }

        val socket = EngineSocket(uri, opt, scope)
        engine = socket
        state = State.OPENING
        skipReconnect = false

        socket.on(EngineSocket.EVENT_TRANSPORT, object : Listener {
            override fun call(vararg args: Any) {
                emit(EVENT_TRANSPORT, *args)
            }
        })

        val openSub = On.on(socket, EngineSocket.EVENT_OPEN, object : Listener {
            override fun call(vararg args: Any) {
                onOpen()
                callback?.invoke("")
            }
        })
        val errorSub = On.on(socket, EngineSocket.EVENT_ERROR, object : Listener {
            override fun call(vararg args: Any) {
                Logging.error(TAG, "open connect_error")
                cleanUp()
                state = State.CLOSED
                emit(EVENT_ERROR, *args)
                if (callback != null) {
                    callback("Connection error")
                } else {
                    // Only do this if there is no fn to handle the error
                    maybeReconnectOnOpen()
                }
            }
        })

        val onTimeout = {
            Logging.error(TAG, "connect attempt timed out after ${opt.timeout}")
            openSub.destroy()
            socket.close()
            socket.emit(EngineSocket.EVENT_ERROR, "timeout")
        }
        if (opt.timeout == 0L) {
            onTimeout()
            return
        } else {
            val job = scope.launch {
                delay(opt.timeout)
                onTimeout()
            }
            subs.add(object : On.Handle {
                override fun destroy() {
                    job.cancel()
                }
            })
        }

        subs.add(openSub)
        subs.add(errorSub)
        socket.open()
    }

    @WorkThread
    private fun onOpen() {
        Logging.info(TAG, "onOpen, state $state")
        if (state != State.OPENING) {
            return
        }
        cleanUp()
        state = State.OPEN
        emit(EVENT_OPEN)

        val socket = engine ?: return
        subs.add(On.on(socket, EngineSocket.EVENT_DATA, object : Listener {
            override fun call(vararg args: Any) {
                if (args.isNotEmpty()) {
                    Logging.debug(TAG) { "on EngineSocket data ${args[0]::class}" }
                    emit(EVENT_PACKET, args[0])
                }
            }
        }))
        subs.add(On.on(socket, EngineSocket.EVENT_ERROR, object : Listener {
            override fun call(vararg args: Any) {
                if (args.isNotEmpty() && args[0] is String) {
                    onError(args[0] as String)
                } else {
                    onError("EngineSocket error")
                }
            }
        }))
        subs.add(On.on(socket, EngineSocket.EVENT_CLOSE, object : Listener {
            override fun call(vararg args: Any) {
                if (args.isNotEmpty() && args[0] is String) {
                    onClose(args[0] as String)
                } else {
                    onClose("EngineSocket close")
                }
            }
        }))
    }

    @WorkThread
    private fun cleanUp() {
        Logging.info(TAG, "cleanUp")
        for (sub in subs) {
            sub.destroy()
        }
        subs.clear()
    }

    @WorkThread
    private fun onError(error: String) {
        Logging.error(TAG, "onError `$error`")
        emit(EVENT_ERROR, error)
    }

    @WorkThread
    internal fun close() {
        Logging.info(TAG, "close")
        skipReconnect = true
        reconnecting = false
        cleanUp()
        opt.backoff.reset()
        state = State.CLOSED
        engine?.close()
        engine = null
    }

    @WorkThread
    private fun onClose(reason: String) {
        Logging.info(TAG, "onClose `$reason`, reconnection ${opt.reconnection}, skipReconnect $skipReconnect")
        cleanUp()
        opt.backoff.reset()
        state = State.CLOSED
        emit(EVENT_CLOSE, reason)

        if (opt.reconnection && !skipReconnect) {
            reconnect()
        }
    }

    @WorkThread
    private fun maybeReconnectOnOpen() {
        // Only try to reconnect if it's the first time we're connecting
        Logging.info(
            TAG, "maybeReconnectOnOpen: reconnecting $reconnecting, " +
                    "reconnection ${opt.reconnection}, attempts ${opt.backoff.attempts}"
        )
        if (!reconnecting && opt.reconnection && opt.backoff.attempts == 0) {
            reconnect()
        }
    }

    @WorkThread
    private fun reconnect() {
        Logging.info(TAG, "reconnect: reconnecting $reconnecting, skipReconnect $skipReconnect")
        if (reconnecting || skipReconnect) {
            return
        }
        if (opt.backoff.attempts >= opt.reconnectionAttempts) {
            Logging.error(TAG, "reconnect failed")
            opt.backoff.reset()
            emit(EVENT_RECONNECT_FAILED)
            reconnecting = false
        } else {
            val delay = opt.backoff.duration
            Logging.info(TAG, "reconnect will wait $delay ms before attempt")
            reconnecting = true
            val job = scope.launch {
                delay(delay)
                if (skipReconnect) {
                    Logging.info(TAG, "reconnect skip after delay")
                    return@launch
                }
                Logging.info(TAG, "reconnect attempting")
                emit(EVENT_RECONNECT_ATTEMPT, opt.backoff.attempts)

                // check again for the case socket closed in above events
                if (skipReconnect) {
                    Logging.info(TAG, "reconnect skip after EVENT_RECONNECT_ATTEMPT")
                    return@launch
                }

                open {
                    reconnecting = false
                    if (it.isEmpty()) {
                        Logging.info(TAG, "reconnect success")
                        emit(EVENT_RECONNECT, opt.backoff.reset())
                    } else {
                        Logging.error(TAG, "reconnect attempt error")
                        reconnect()
                        emit(EVENT_RECONNECT_ERROR, it)
                    }
                }
            }
            subs.add(object : On.Handle {
                override fun destroy() {
                    job.cancel()
                }
            })
        }
    }

    /**
     * Initializes {@link Socket} instances for each namespace.
     *
     * @param nsp namespace.
     * @param auth auth info.
     * @return a socket instance for the namespace.
     */
    @WorkThread
    internal fun socket(nsp: String, auth: Map<String, String>): Socket {
        Logging.info(TAG, "socket: nsp $nsp, auth $auth")
        return nsps.getOrElse(nsp) {
            val sock = Socket(this, nsp, auth, scope)
            nsps[nsp] = sock
            sock
        }
    }

    @WorkThread
    internal fun packets(packets: List<EngineIOPacket<*>>) {
        Logging.debug(TAG) { "send packets $packets" }
        engine?.send(packets)
    }

    @WorkThread
    internal fun destroy() {
        Logging.info(TAG, "destroy")
        for ((nsp, sock) in nsps) {
            if (sock.active()) {
                Logging.info(TAG, "destroy with socket in $nsp still active, skip")
                return
            }
        }
        close()
    }

    companion object {

        /**
         * Called on a successful connection.
         */
        const val EVENT_OPEN = EngineSocket.EVENT_OPEN

        /**
         * Called on a disconnection.
         */
        const val EVENT_CLOSE = EngineSocket.EVENT_CLOSE

        const val EVENT_PACKET = EngineSocket.EVENT_PACKET
        const val EVENT_ERROR = EngineSocket.EVENT_ERROR

        /**
         * Called on a successful reconnection.
         */
        const val EVENT_RECONNECT = "reconnect"

        /**
         * Called on a reconnection attempt error.
         */
        const val EVENT_RECONNECT_ERROR = "reconnect_error"
        const val EVENT_RECONNECT_FAILED = "reconnect_failed"
        const val EVENT_RECONNECT_ATTEMPT = "reconnect_attempt"

        /**
         * Called when new transport is created. (experimental)
         */
        const val EVENT_TRANSPORT = EngineSocket.EVENT_TRANSPORT

        private const val TAG = "Manager"
    }
}
