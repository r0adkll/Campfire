package com.piasy.kmp.socketio.engineio

import com.piasy.kmp.socketio.emitter.Emitter
import com.piasy.kmp.xlog.Logging
import com.piasy.kmp.socketio.parseqs.ParseQS
import io.ktor.client.HttpClient
import io.ktor.util.date.*
import kotlinx.coroutines.CoroutineScope
import org.hildan.socketio.EngineIOPacket
import kotlin.jvm.JvmField

abstract class Transport(
    internal val opt: Options,
    protected val scope: CoroutineScope,
    val name: String,
    protected val rawMessage: Boolean,
) : Emitter() {
    open class Options {
        // @JvmField for jvm test code
        @JvmField
        var secure = false

        @JvmField
        var hostname = ""

        @JvmField
        var port = -1

        @JvmField
        var path = ""

        @JvmField
        var timestampRequests = false

        @JvmField
        var timestampParam = ""

        @JvmField
        var query: MutableMap<String, String> = HashMap()

        @JvmField
        var extraHeaders: Map<String, List<String>> = emptyMap()

        @JvmField
        var trustAllCerts: Boolean = false

        /**
         * Optional externally managed ktor HttpClient to reuse.
         */
        @JvmField
        var httpClient: HttpClient? = null
    }

    protected var state = State.INIT
    internal var writable = false

    @WorkThread
    fun open(): Transport {
        logD { "open" }
        if (state == State.CLOSED || state == State.INIT) {
            state = State.OPENING
            doOpen()
        }
        return this
    }

    @WorkThread
    fun send(packets: List<EngineIOPacket<*>>) {
        logD { "send: state $state, ${packets.size} packets" }
        if (state == State.OPEN) {
            doSend(packets)
        } else {
            onError("Transport not open")
        }
    }

    @WorkThread
    fun close(): Transport {
        logI("close, state $state")
        if (state == State.OPENING || state == State.OPEN) {
            val fromOpenState = state == State.OPEN
            state = State.CLOSING
            doClose(fromOpenState)
        }
        return this
    }

    @WorkThread
    abstract fun pause(onPause: () -> Unit)

    @WorkThread
    protected fun onOpen() {
        logI("onOpen, state $state")
        if (state == State.OPENING || state == State.CLOSING) {
            state = State.OPEN
            writable = true
            emit(EVENT_OPEN)
        }
    }

    @WorkThread
    protected fun onPacket(packet: EngineIOPacket<*>) {
        logD { "onPacket $packet" }
        emit(EVENT_PACKET, packet)
    }

    @WorkThread
    internal fun onError(msg: String) {
        logE("onError `$msg`")
        emit(EVENT_ERROR, msg)
    }

    @WorkThread
    protected fun onClose() {
        logI("onClose")
        state = State.CLOSED
        emit(EVENT_CLOSE)
    }

    @WorkThread
    protected abstract fun doOpen()

    @WorkThread
    protected abstract fun doSend(packets: List<EngineIOPacket<*>>)

    @WorkThread
    protected abstract fun doClose(fromOpenState: Boolean)

    @WorkThread
    protected fun uri(secureSchema: String, insecureSchema: String): String {
        val query = HashMap(opt.query)
        val schema = if (opt.secure) secureSchema else insecureSchema
        var port = ""

        if (opt.port > 0 && ((opt.secure && opt.port != 443)
                    || (!opt.secure && opt.port != 80))
        ) {
            port = ":" + opt.port
        }

        if (opt.timestampRequests) {
            // In official Java implementation, the radix is 64, we just use 36
            // here because it's the max radix in kotlin.
            query[opt.timestampParam] = GMTDate().timestamp.toString(36)
        }

        var derivedQuery = ParseQS.encode(query)
        if (derivedQuery.isNotEmpty()) {
            derivedQuery = "?$derivedQuery"
        }

        val hostname = if (opt.hostname.contains(":")) {
            "[" + opt.hostname + "]"
        } else {
            opt.hostname
        }
        return "$schema://$hostname$port${opt.path}$derivedQuery"
    }

    protected fun logD(block: () -> String) {
        if (Logging.debug()) {
            Logging.debug(TAG, "$name@${hashCode()} ${block()}")
        }
    }

    protected fun logI(log: String) {
        Logging.info(TAG, "$name@${hashCode()} $log")
    }

    protected fun logE(log: String) {
        Logging.error(TAG, "$name@${hashCode()} $log")
    }

    companion object {
        // all internal events are emitted from work thread.
        const val EVENT_OPEN: String = "open"
        const val EVENT_CLOSE: String = "close"
        const val EVENT_PACKET: String = "packet"
        const val EVENT_DRAIN: String = "drain"
        const val EVENT_ERROR: String = "error"
        const val EVENT_REQUEST_HEADERS: String = "requestHeaders"
        const val EVENT_RESPONSE_HEADERS: String = "responseHeaders"

        private const val TAG = "Transport"
    }
}
