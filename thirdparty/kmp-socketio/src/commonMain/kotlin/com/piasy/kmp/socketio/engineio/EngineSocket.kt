package com.piasy.kmp.socketio.engineio

import com.piasy.kmp.socketio.emitter.Emitter
import com.piasy.kmp.socketio.engineio.transports.DefaultTransportFactory
import com.piasy.kmp.socketio.engineio.transports.PollingXHR
import com.piasy.kmp.socketio.engineio.transports.TransportFactory
import com.piasy.kmp.socketio.engineio.transports.WebSocket
import com.piasy.kmp.xlog.Logging
import io.ktor.http.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.hildan.socketio.EngineIOPacket
import kotlin.jvm.JvmField

class EngineSocket(
    uri: String,
    @JvmField internal val opt: Options,
    internal val scope: CoroutineScope,
    private val factory: TransportFactory = DefaultTransportFactory,
    private val rawMessage: Boolean = false,
) : Emitter() {
    open class Options : Transport.Options() {
        /**
         * List of transport names.
         */
        @JvmField
        var transports: List<String> = emptyList()

        /**
         * Whether to upgrade the transport. Defaults to `true`.
         */
        @JvmField
        var upgrade = true

        @JvmField
        var rememberUpgrade = false

        @JvmField
        var transportOptions: Map<String, Transport.Options> = emptyMap()
    }

    internal var disablePingTimeout = false // to help unit test
    private var state = State.INIT
    internal var id = ""
    private var upgrades = emptyList<String>()
    private var pingInterval = 0
    private var pingTimeout = 0
    internal var transport: Transport? = null
    private val subs = ArrayList<On.Handle>()
    private var upgrading = false

    internal val writeBuffer = ArrayDeque<EngineIOPacket<*>>()
    private var prevBufferLen = 0

    private val heartbeatListener = object : Listener {
        override fun call(vararg args: Any) {
            onHeartBeat()
        }
    }
    private var pingTimeoutJob: Job? = null

    init {
        val url = Url(uri)
        opt.secure = url.protocol == URLProtocol.HTTPS
                || url.protocol == URLProtocol.WSS

        var hostname = url.host
        if (hostname.split(':').size > 2) {
            val start = hostname.indexOf('[')
            if (start != -1) {
                hostname = hostname.substring(start + 1)
            }
            val end = hostname.lastIndexOf(']')
            if (end != -1) {
                hostname = hostname.substring(0, end)
            }
        }
        opt.hostname = hostname
        opt.port = url.port
        if (opt.path.isEmpty()) {
            opt.path = "/engine.io/"
        }

        if (opt.timestampParam.isEmpty()) {
            opt.timestampParam = "t"
        }

        if (!url.parameters.isEmpty()) {
            val query = HashMap<String, String>()
            url.parameters.entries().forEach {
                query[it.key] = it.value[0]
            }
            opt.query = query
        }

        if (opt.transports.isEmpty()) {
            opt.transports = listOf(PollingXHR.NAME, WebSocket.NAME)
        }
    }

    /**
     * Connects the client.
     *
     * @return a reference to this object.
     */
    @WorkThread
    fun open() {
        Logging.info(TAG, "open: state $state")
        if (state != State.INIT && state != State.CLOSED) {
            Logging.error(TAG, "open at wrong state: $state")
            return
        }

        val name = if (opt.rememberUpgrade && priorWebsocketSuccess
            && opt.transports.contains(WebSocket.NAME)
        ) {
            WebSocket.NAME
        } else {
            // transports won't be empty
            opt.transports[0]
        }

        state = State.OPENING
        val transport = createTransport(name)
        setTransport(transport)
        transport.open()
    }

    /**
     * Sends a packet.
     *
     * @param packet
     */
    @WorkThread
    fun send(packet: EngineIOPacket<*>) {
        sendPackets(listOf(packet))
    }

    @WorkThread
    fun send(packets: List<EngineIOPacket<*>>) {
        sendPackets(packets)
    }

    /**
     * Disconnects the client.
     *
     * @return a reference to this object.
     */
    @WorkThread
    fun close() {
        Logging.info(TAG, "close: state $state, writeBuffer.size ${writeBuffer.size}, upgrading: $upgrading")
        if (state != State.OPENING && state != State.OPEN) {
            Logging.info(TAG, "close at wrong state: $state")
            return
        }
        state = State.CLOSING

        val closeAction = {
            Logging.info(TAG, "socket closing - telling transport to close")
            onClose("force close")
        }
        val cleanupAndClose = object : Listener {
            override fun call(vararg args: Any) {
                Logging.info(TAG, "close waiting upgrade success")
                off(EVENT_UPGRADE, this)
                off(EVENT_UPGRADE_ERROR, this)
                closeAction()
            }
        }
        val waitForUpgrade = {
            Logging.info(TAG, "close waiting upgrade")
            // wait for upgrade to finish since we can't
            // send packets while pausing transport.
            once(EVENT_UPGRADE, cleanupAndClose)
            once(EVENT_UPGRADE_ERROR, cleanupAndClose)
        }

        if (writeBuffer.isNotEmpty()) {
            Logging.info(TAG, "close waiting drain event")
            once(EVENT_DRAIN, object : Listener {
                override fun call(vararg args: Any) {
                    Logging.info(TAG, "close waiting drain success")
                    if (upgrading) {
                        waitForUpgrade()
                    } else {
                        closeAction()
                    }
                }
            })
        } else if (upgrading) {
            waitForUpgrade()
        } else {
            closeAction()
        }
    }

    @WorkThread
    private fun createTransport(name: String): Transport {
        Logging.info(TAG, "createTransport $name")
        val query = HashMap(opt.query)
        query["EIO"] = "4"
        query["transport"] = name
        if (id.isNotEmpty()) {
            query[SID] = id
        }

        val options = opt.transportOptions[name]
        val opts = Transport.Options()
        opts.query = query

        opts.secure = options?.secure ?: opt.secure
        opts.hostname = options?.hostname ?: opt.hostname
        opts.port = options?.port ?: opt.port
        opts.path = options?.path ?: opt.path
        opts.timestampRequests = options?.timestampRequests ?: opt.timestampRequests
        opts.timestampParam = options?.timestampParam ?: opt.timestampParam
        opts.extraHeaders = opt.extraHeaders
        opts.trustAllCerts = opt.trustAllCerts
        opts.httpClient = options?.httpClient ?: opt.httpClient

        val transport = factory.create(name, opts, scope, rawMessage)
        emit(EVENT_TRANSPORT, transport)
        return transport
    }

    @WorkThread
    private fun setTransport(transport: Transport) {
        Logging.info(TAG, "setTransport ${transport.name}")
        val oldTransport = this.transport
        if (oldTransport != null) {
            Logging.info(TAG, "clearing existing transport ${oldTransport.name}")
            for (sub in subs) {
                sub.destroy()
            }
            subs.clear()
            // old transport is always Polling, and it's already paused.
            // so we don't need to close it.
        }

        this.transport = transport

        subs.add(On.on(transport, Transport.EVENT_DRAIN, object : Listener {
            override fun call(vararg args: Any) {
                if (args.isNotEmpty() && args[0] is Int) {
                    onDrain(args[0] as Int)
                } else {
                    Logging.error(TAG, "onDrain with wrong args: `${args.joinToString()}`")
                }
            }
        }))
        subs.add(On.on(transport, Transport.EVENT_PACKET, object : Listener {
            override fun call(vararg args: Any) {
                val packet = args.firstOrNull() ?: return
                Logging.debug(TAG) { "transport on packet $packet" }
                if (packet is EngineIOPacket<*>) {
                    onPacket(packet)
                }
            }
        }))
        subs.add(On.on(transport, Transport.EVENT_ERROR, object : Listener {
            override fun call(vararg args: Any) {
                val msg = args.firstOrNull() ?: ""
                if (msg is String) {
                    onError(msg)
                }
            }
        }))
        subs.add(On.on(transport, Transport.EVENT_CLOSE, object : Listener {
            override fun call(vararg args: Any) {
                onClose("transport close")
            }
        }))
    }

    @WorkThread
    private fun onDrain(len: Int) {
        Logging.debug(TAG) { "onDrain: prevBufferLen $prevBufferLen, writeBuffer.size ${writeBuffer.size}, len $len" }
        for (i in 1..len) {
            writeBuffer.removeAt(0)
        }
        prevBufferLen -= len

        if (writeBuffer.isEmpty()) {
            Logging.debug(TAG) { "onDrain fire socket drain event" }
            emit(EVENT_DRAIN)
        } else if (writeBuffer.size > prevBufferLen) {
            flush()
        }
    }

    @WorkThread
    private fun sendPackets(packets: List<EngineIOPacket<*>>) {
        Logging.debug(TAG) { "sendPackets: state $state, $packets" }
        if (state != State.OPENING && state != State.OPEN) {
            Logging.error(TAG, "sendPackets at wrong state: $state")
            return
        }

        emit(EVENT_PACKET_CREATE, packets.size)
        writeBuffer.addAll(packets)
        flush()
    }

    @WorkThread
    private fun onPacket(packet: EngineIOPacket<*>) {
        Logging.debug(TAG) { "onPacket $packet" }
        if (inactive()) {
            Logging.error(TAG, "packet received at wrong state: $state")
            return
        }

        emit(EVENT_PACKET, packet)
        emit(EVENT_HEARTBEAT)

        when (packet) {
            is EngineIOPacket.Open -> onHandshake(packet)
            is EngineIOPacket.Ping -> {
                emit(EVENT_PING)
                sendPackets(listOf(EngineIOPacket.Pong(null)))
            }

            is EngineIOPacket.BinaryData -> emit(EVENT_DATA, packet.payload)
            is EngineIOPacket.Message<*> -> {
                val data = packet.payload
                if (data != null) {
                    emit(EVENT_DATA, data)
                }
            }

            else -> {}
        }
    }

    @WorkThread
    private fun onHandshake(pkt: EngineIOPacket.Open) {
        emit(EVENT_HANDSHAKE, pkt)
        id = pkt.sid
        transport?.opt?.query?.set(SID, id)
        upgrades = filterUpgrades(pkt.upgrades)
        pingInterval = pkt.pingInterval
        pingTimeout = pkt.pingTimeout
        onOpen()

        // In case open handler closes socket
        if (state != State.OPEN) {
            Logging.info(TAG, "onHandshake skip: state $state")
            return
        }

        onHeartBeat()
        off(EVENT_HEARTBEAT, heartbeatListener)
        on(EVENT_HEARTBEAT, heartbeatListener)
    }

    internal fun filterUpgrades(upgrades: List<String>): List<String> =
        upgrades.filter { opt.transports.contains(it) && it != transport?.name }

    @WorkThread
    private fun onOpen() {
        Logging.info(TAG, "onOpen")
        state = State.OPEN
        priorWebsocketSuccess = transport?.name == WebSocket.NAME
        emit(EVENT_OPEN)
        // this flush call will always be ignored, because `emit(EVENT_OPEN)` will trigger
        // sending Connect packet, which will trigger flush.
        //flush()

        if (opt.upgrade && transport?.name == PollingXHR.NAME && upgrades.isNotEmpty()) {
            Logging.info(TAG, "starting upgrade probes")
            for (upgrade in upgrades) {
                probe(upgrade)
            }
        }
    }

    @WorkThread
    private fun flush() {
        Logging.debug(TAG) {
            "flush: state $state, writable ${transport?.writable}, " +
                    "upgrading $upgrading, prevBufferLen $prevBufferLen, " +
                    "writeBuffer.size ${writeBuffer.size}"
        }
        if (state != State.CLOSED
            && transport?.writable == true
            && !upgrading
            && writeBuffer.size > prevBufferLen
        ) {
            val packets = writeBuffer.subList(prevBufferLen, writeBuffer.size)
            prevBufferLen = writeBuffer.size
            transport?.send(ArrayList(packets))
            emit(EVENT_FLUSH)
        } else {
            Logging.info(
                TAG, "flush ignored: state $state, transport.writable ${transport?.writable}, " +
                        "upgrading $upgrading, writeBuffer.size ${writeBuffer.size}, prevBufferLen $prevBufferLen"
            )
        }
    }

    @WorkThread
    private fun probe(name: String) {
        Logging.info(TAG, "probing transport '$name'")
        val transport = createTransport(name)
        var failed = false
        priorWebsocketSuccess = false

        val cleanUp = ArrayList<() -> Unit>()
        var cleaned = false

        val onTransportOpen = object : Listener {
            override fun call(vararg args: Any) {
                Logging.info(TAG, "probe transport $name opened, failed: $failed")
                if (failed) {
                    return
                }

                transport.send(listOf(EngineIOPacket.Ping(PROBE)))
                transport.once(Transport.EVENT_PACKET, object : Listener {
                    override fun call(vararg args: Any) {
                        if (failed) {
                            return
                        }
                        if (args.isNotEmpty() && args[0] is EngineIOPacket.Pong
                            && (args[0] as EngineIOPacket.Pong).payload == PROBE
                        ) {
                            Logging.info(TAG, "probe transport $name pong")
                            upgrading = true
                            emit(EVENT_UPGRADING, transport)
                            if (cleaned) {
                                return
                            }

                            priorWebsocketSuccess = transport.name == WebSocket.NAME
                            val currentTransport = this@EngineSocket.transport ?: return
                            Logging.info(TAG, "pausing current transport ${currentTransport.name}")
                            currentTransport.pause {
                                if (failed || state == State.CLOSED) {
                                    return@pause
                                }
                                Logging.info(TAG, "changing transport and sending upgrade packet")
                                cleanUp[0]()
                                transport.once(EVENT_DRAIN, object : Listener {
                                    override fun call(vararg args: Any) {
                                        Logging.info(TAG, "upgrade packet send success")
                                        emit(EVENT_UPGRADE, transport)
                                        setTransport(transport)
                                        cleaned = true
                                        upgrading = false
                                        flush()
                                    }
                                })
                                transport.send(listOf(EngineIOPacket.Upgrade))
                            }
                        } else {
                            Logging.error(TAG, "probe transport $name failed")
                            emit(EVENT_UPGRADE_ERROR, PROBE_ERROR)
                        }
                    }
                })
            }
        }
        val freezeTransport = object : Listener {
            override fun call(vararg args: Any) {
                if (failed) {
                    return
                }
                failed = true
                cleanUp[0]()
                transport.close()
                cleaned = true
            }
        }
        // Handle any error that happens while probing
        val onTransportError = object : Listener {
            override fun call(vararg args: Any) {
                freezeTransport.call()
                Logging.error(TAG, "probe transport $name failed because of error: ${args.joinToString()}")
                emit(EVENT_UPGRADE_ERROR, PROBE_ERROR)
            }
        }
        val onTransportClose = object : Listener {
            override fun call(vararg args: Any) {
                Logging.error(TAG, "probe transport $name, but transport closed")
                onTransportError.call("transport closed")
            }
        }
        val onClose = object : Listener {
            override fun call(vararg args: Any) {
                Logging.error(TAG, "probe transport $name, but socket closed")
                onTransportError.call("socket closed")
            }
        }
        val onUpgrade = object : Listener {
            override fun call(vararg args: Any) {
                if (args.isNotEmpty() && args[0] is Transport) {
                    val to = args[0] as Transport
                    if (to.name != transport.name) {
                        Logging.info(TAG, "probe but ${to.name} works, abort ${transport.name}")
                        freezeTransport.call()
                    }
                }
            }
        }

        cleanUp.add {
            transport.off(Transport.EVENT_OPEN, onTransportOpen)
            transport.off(Transport.EVENT_ERROR, onTransportError)
            transport.off(Transport.EVENT_CLOSE, onTransportClose)
            off(EVENT_CLOSE, onClose)
            off(EVENT_UPGRADING, onUpgrade)
        }

        transport.once(Transport.EVENT_OPEN, onTransportOpen)
        transport.once(Transport.EVENT_ERROR, onTransportError)
        transport.once(Transport.EVENT_CLOSE, onTransportClose)
        once(EVENT_CLOSE, onClose)
        once(EVENT_UPGRADING, onUpgrade)

        transport.open()
    }

    @WorkThread
    private fun onHeartBeat() {
        if (disablePingTimeout) {
            return
        }
        pingTimeoutJob?.cancel()
        pingTimeoutJob = scope.launch {
            delay((pingInterval + pingTimeout).toLong())
            if (!inactive()) {
                onClose("ping timeout")
            }
        }
    }

    @WorkThread
    private fun onError(msg: String) {
        val log = "transport onError: `$msg`"
        Logging.error(TAG, log)
        priorWebsocketSuccess = false
        emit(EVENT_ERROR, msg)
        onClose(log)
    }

    @WorkThread
    private fun onClose(reason: String) {
        if (inactive()) {
            return
        }

        Logging.info(TAG, "onClose $reason")
        pingTimeoutJob?.cancel()

        // stop event from firing again for transport
        transport?.off(EVENT_CLOSE)
        // ensure transport won't stay open
        transport?.close()
        // ignore further transport communication
        // transport?.off() // this will cause missing event
        for (sub in subs) {
            sub.destroy()
        }
        subs.clear()

        state = State.CLOSED
        id = ""

        emit(EVENT_CLOSE, reason)

        // clear buffers after, so users can still
        // grab the buffers on `close` event
        writeBuffer.clear()
        prevBufferLen = 0
    }

    private fun inactive() = state != State.OPENING
            && state != State.OPEN
            && state != State.CLOSING

    companion object {
        private const val TAG = "EngineSocket"
        private const val PROBE_ERROR = "probe error"
        private var priorWebsocketSuccess = false

        internal const val PROBE = "probe"
        internal const val SID = "sid"

        /**
         * Called on successful connection.
         */
        const val EVENT_OPEN = "open"

        /**
         * Called on disconnection.
         */
        const val EVENT_CLOSE = "close"

        /**
         * Called when data is received from the server.
         */
        const val EVENT_DATA = "data"

        /**
         * Called when an error occurs.
         */
        const val EVENT_ERROR = "error"


        const val EVENT_UPGRADE_ERROR = "upgradeError"

        /**
         * Called on completing a buffer flush.
         */
        const val EVENT_FLUSH = "flush"

        /**
         * Called after `drain` event of transport if writeBuffer is empty.
         */
        const val EVENT_DRAIN = "drain"


        const val EVENT_HANDSHAKE = "handshake"
        const val EVENT_UPGRADING = "upgrading"
        const val EVENT_UPGRADE = "upgrade"
        const val EVENT_PACKET = "packet"
        const val EVENT_PACKET_CREATE = "packetCreate"
        const val EVENT_HEARTBEAT = "heartbeat"
        const val EVENT_PING = "ping"

        /**
         * Called on new transport is created.
         */
        const val EVENT_TRANSPORT = "transport"

    }
}
