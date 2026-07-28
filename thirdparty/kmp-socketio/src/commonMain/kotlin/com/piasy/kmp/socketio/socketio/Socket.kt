package com.piasy.kmp.socketio.socketio

import com.piasy.kmp.socketio.emitter.Emitter
import com.piasy.kmp.socketio.engineio.*
import com.piasy.kmp.xlog.Logging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.io.bytestring.ByteString
import kotlinx.serialization.json.*
import org.hildan.socketio.EngineIOPacket
import org.hildan.socketio.PayloadElement
import org.hildan.socketio.SocketIOPacket

class Socket(
    val io: Manager,
    val nsp: String,
    private val auth: Map<String, String>,
    private val scope: CoroutineScope,
) : Emitter() {
    /**
     * Whether this socket namespace is currently connected.
     */
    var connected = false
        private set
    private val subs = ArrayList<On.Handle>()
    private val ack = HashMap<Int, Ack>()
    private var ackId = 0

    private val sendBuffer = ArrayList<EngineIOPacket<*>>()
    private val recvBuffer = ArrayList<ArrayList<Any>>()
    private var reconstructor: BinaryPacketReconstructor? = null

    var id = ""
        private set

    /**
     * Connects the socket.
     */
    @CallerThread
    fun open() {
        scope.launch {
            Logging.info(TAG, "open: connected $connected, io reconnecting ${io.reconnecting}")
            if (connected || io.reconnecting) {
                return@launch
            }

            subEvents()
            io.open()
            if (io.state == State.OPEN) {
                onOpen()
            }
        }
    }

    /**
     * Disconnects the socket.
     */
    @CallerThread
    fun close() {
        scope.launch {
            Logging.info(TAG, "close: connected $connected")
            if (connected) {
                io.packets(listOf(EngineIOPacket.Message(SocketIOPacket.Disconnect(nsp))))
            }
            destroy()
            if (connected) {
                onClose("io client disconnect")
            }
        }
    }

    /**
     * Send `message` with args.
     * @param args only accepts String/Boolean/Number/JsonElement/ByteString
     */
    @CallerThread
    fun send(vararg args: Any): Socket {
        return emit(EVENT_MESSAGE, *args) as Socket
    }

    /**
     * emit custom event with args.
     * @param args only accepts String/Boolean/Number/JsonElement/ByteString
     */
    @CallerThread
    override fun emit(event: String, vararg args: Any): Emitter {
        if (RESERVED_EVENTS.contains(event)) {
            onError("emit reserved event: $event")
            return this
        }
        scope.launch {
            if (args.isNotEmpty() && args.last() is Ack) {
                val arr = Array(args.size - 1) {
                    args[it]
                }
                emitWithAck(event, arr, args.last() as Ack)
            } else {
                emitWithAck(event, args, null)
            }
        }
        return this
    }

    @WorkThread
    private fun emitWithAck(event: String, args: Array<out Any>, ack: Ack?) {
        Logging.debug(TAG) { "emitWithAck: $event, ${args.joinToString()}, ack $ack" }
        val ackId = if (ack != null) this.ackId else null
        if (ack != null && ackId != null) {
            Logging.info(TAG, "emit with ack id $ackId")
            if (ack is AckWithTimeout) {
                ack.schedule(scope) {
                    // remove the ack from the map (to prevent an actual acknowledgement)
                    this.ack.remove(ackId)
                    // remove the packet from the buffer (if applicable)
                    val iterator = sendBuffer.iterator()
                    while (iterator.hasNext()) {
                        val pktAckId = when (val pkt = iterator.next()) {
                            is EngineIOPacket.Message<*> -> {
                                when (val payload = pkt.payload) {
                                    is SocketIOPacket.Event -> payload.ackId
                                    is SocketIOPacket.BinaryAck -> payload.ackId
                                    else -> null
                                }
                            }

                            else -> null
                        }
                        if (pktAckId == ackId) {
                            iterator.remove()
                            break
                        }
                    }
                }
            }

            this.ack[ackId] = ack
            this.ackId++
        }

        val packets = if (args.hasBinary()) {
            binaryPackets(args) { payloads, nAttachments ->
                SocketIOPacket.BinaryEvent(nsp, ackId, buildList {
                    add(PayloadElement.Json(JsonPrimitive(event)))
                    addAll(payloads)
                }, nAttachments)
            }
        } else {
            listOf(EngineIOPacket.Message(SocketIOPacket.Event(nsp, ackId, buildJsonArray {
                add(JsonPrimitive(event))
                args.forEach { add(toJson(it)) }
            })))
        }

        if (connected) {
            io.packets(packets)
        } else {
            sendBuffer.addAll(packets)
        }
    }

    @WorkThread
    private fun binaryPackets(
        args: Array<out Any>,
        creator: (List<PayloadElement>, Int) -> SocketIOPacket
    ): List<EngineIOPacket<*>> {
        val payloads = ArrayList<PayloadElement>()
        val buffers = ArrayList<ByteString>()
        args.forEach {
            when (it) {
                is JsonElement -> payloads.add(PayloadElement.Json(it))
                is ByteString -> {
                    payloads.add(PayloadElement.AttachmentRef(buffers.size))
                    buffers.add(it)
                }

                else -> payloads.add(PayloadElement.Json(toJson(it)))
            }
        }

        val packets = ArrayList<EngineIOPacket<*>>()
        packets.add(EngineIOPacket.Message(creator(payloads, buffers.size)))
        buffers.forEach {
            packets.add(EngineIOPacket.BinaryData(it))
        }
        return packets
    }

    @WorkThread
    private fun destroy() {
        subs.forEach { it.destroy() }
        subs.clear()
        io.destroy()
    }

    @WorkThread
    private fun subEvents() {
        Logging.info(TAG, "subEvents: subs ${subs.size}")
        if (subs.isNotEmpty()) {
            return
        }
        subs.add(On.on(io, Manager.EVENT_OPEN, object : Listener {
            override fun call(vararg args: Any) {
                onOpen()
            }
        }))
        subs.add(On.on(io, Manager.EVENT_PACKET, object : Listener {
            override fun call(vararg args: Any) {
                if (args.isNotEmpty()) {
                    when (val pkt = args[0]) {
                        is SocketIOPacket -> onPacket(pkt)
                        is ByteString -> {
                            if (reconstructor == null) {
                                onError("Receive binary buffer while not reconstructing binary packet")
                            }
                            reconstructor?.add(pkt)
                        }
                    }
                }
            }
        }))
        subs.add(On.on(io, Manager.EVENT_ERROR, object : Listener {
            override fun call(vararg args: Any) {
                onManagerError(if (args.isNotEmpty() && args[0] is String) args[0] as String else "Manager error")
            }
        }))
        subs.add(On.on(io, Manager.EVENT_CLOSE, object : Listener {
            override fun call(vararg args: Any) {
                onClose(if (args.isNotEmpty() && args[0] is String) args[0] as String else "Manager close")
            }
        }))
    }

    @WorkThread
    private fun onOpen() {
        Logging.info(TAG, "onOpen")
        val auth = if (auth.isEmpty()) {
            null
        } else {
            Json.encodeToJsonElement(auth) as JsonObject
        }
        io.packets(listOf(EngineIOPacket.Message(SocketIOPacket.Connect(nsp, auth))))
    }

    @WorkThread
    private fun onPacket(packet: SocketIOPacket) {
        Logging.debug(TAG) { "onPacket: nsp $nsp, $packet" }
        if (nsp != packet.namespace) {
            return
        }

        when (packet) {
            is SocketIOPacket.Connect -> {
                val sid = packet.payload?.get(EngineSocket.SID)
                if (sid is JsonPrimitive) {
                    onConnect(sid.content)
                }
            }

            is SocketIOPacket.Disconnect -> onDisconnect()
            is SocketIOPacket.ConnectError -> {
                destroy()
                val data = packet.errorData ?: JsonObject(emptyMap())
                super.emit(EVENT_CONNECT_ERROR, data)
            }

            is SocketIOPacket.Event -> {
                Logging.debug(TAG) { "onEvent $packet" }
                onEvent(packet.ackId, ArrayList(packet.payload))
            }

            is SocketIOPacket.Ack -> onAck(packet.ackId, ArrayList(packet.payload))
            is SocketIOPacket.BinaryEvent,
            is SocketIOPacket.BinaryAck -> {
                if (reconstructor != null) {
                    onError("Receive binary event/ack while reconstructing binary packet, $packet")
                    // let's just reconstruct a new binary packet
                }
                Logging.info(TAG, "start reconstructing binary packet, $packet")
                reconstructor =
                    BinaryPacketReconstructor(packet as SocketIOPacket.BinaryMessage) { isAck, ackId, data ->
                        Logging.info(TAG, "finish reconstructing binary packet, isAck $isAck, ackId $ackId")
                        if (isAck) {
                            onAck(ackId!!, data)
                        } else {
                            onEvent(ackId, data)
                        }
                        reconstructor = null
                    }
            }
        }
    }

    @WorkThread
    private fun onError(msg: String) {
        Logging.error(TAG, msg)
        super.emit(EVENT_ERROR, msg)
    }

    @WorkThread
    private fun onConnect(id: String) {
        Logging.info(TAG, "onConnect sid $id")
        connected = true
        this.id = id

        recvBuffer.forEach { fireEvent(it) }
        recvBuffer.clear()

        if (sendBuffer.isNotEmpty()) {
            io.packets(sendBuffer)
            sendBuffer.clear()
        }

        super.emit(EVENT_CONNECT)
    }

    @WorkThread
    private fun onDisconnect() {
        Logging.info(TAG, "onDisconnect")
        destroy()
        onClose("io server disconnect")
    }

    @WorkThread
    private fun onEvent(eventId: Int?, data: ArrayList<Any>) {
        if (eventId != null) {
            Logging.debug(TAG) { "attaching ack callback to event" }
            data.add(createAck(eventId))
        }
        if (data.isEmpty()) {
            return
        }
        if (connected) {
            fireEvent(data)
        } else {
            recvBuffer.add(data)
        }
    }

    @WorkThread
    private fun fireEvent(data: ArrayList<Any>) {
        val ev = when (val event = data.removeAt(0)) {
            is String -> event
            is JsonPrimitive -> event.content
            else -> {
                onError("bad event $event")
                return
            }
        }
        val args = Array(data.size) {
            if (data[it] is JsonElement) {
                (data[it] as JsonElement).flatPrimitive()
            } else {
                data[it]
            }
        }
        super.emit(ev, *args)
    }

    @WorkThread
    private fun createAck(ackId: Int) = object : Ack {
        private var sent = false

        override fun call(vararg args: Any) {
            scope.launch {
                if (sent) {
                    return@launch
                }
                sent = true
                Logging.info(TAG, "sending ack: id $ackId ${args.joinToString()}")

                val packets = if (args.hasBinary()) {
                    binaryPackets(args) { payloads, nAttachments ->
                        SocketIOPacket.BinaryAck(nsp, ackId, payloads, nAttachments)
                    }
                } else {
                    listOf(EngineIOPacket.Message(SocketIOPacket.Ack(nsp, ackId, buildJsonArray {
                        args.forEach { add(toJson(it)) }
                    })))
                }
                io.packets(packets)
            }
        }
    }

    @WorkThread
    private fun onAck(ackId: Int, data: ArrayList<Any>) {
        val fn = this.ack.remove(ackId)
        if (fn != null) {
            Logging.info(TAG, "calling ack $ackId with $data")
            val args = Array(data.size) {
                when (val elem = data[it]) {
                    is JsonElement -> elem.flatPrimitive()
                    else -> elem
                }
            }
            fn.call(*args)
        } else {
            Logging.info(TAG, "bad ack $ackId")
        }
    }

    @WorkThread
    private fun onManagerError(error: String) {
        Logging.error(TAG, "onManagerError: `$error`")
        if (!connected) {
            super.emit(EVENT_CONNECT_ERROR, error)
        }
    }

    @WorkThread
    private fun onClose(reason: String) {
        Logging.info(TAG, "onClose: `$reason`")
        connected = false
        id = ""
        super.emit(EVENT_DISCONNECT, reason)
        clearAck()
    }

    /**f
     * Clears the acknowledgement handlers upon disconnection,
     * since the client will never receive an acknowledgement from
     * the server.
     */
    @WorkThread
    private fun clearAck() {
        ack.values.forEach {
            if (it is AckWithTimeout) {
                it.onTimeout()
            }
            // note: basic Ack objects have no way to report an error,
            // so they are simply ignored here
        }
        ack.clear()
    }

    @WorkThread
    internal fun active() = subs.isNotEmpty()

    private fun toJson(primitive: Any) = when (primitive) {
        is String -> JsonPrimitive(primitive)
        is Boolean -> JsonPrimitive(primitive)
        is Number -> JsonPrimitive(primitive)
        is JsonElement -> primitive
        else -> JsonPrimitive(primitive.toString())
    }

    companion object {
        internal const val TAG = "Socket"

        /**
         * Called on a connection.
         */
        const val EVENT_CONNECT = "connect"

        /**
         * Called on a disconnection.
         */
        const val EVENT_DISCONNECT = "disconnect"

        /**
         * Called on a connection error.
         *
         *
         * Parameters:
         *
         *  * (Exception) error data.
         *
         */
        const val EVENT_CONNECT_ERROR = "connect_error"

        const val EVENT_MESSAGE = "message"
        const val EVENT_ERROR = Manager.EVENT_ERROR

        private val RESERVED_EVENTS = setOf(
            EVENT_CONNECT,
            EVENT_CONNECT_ERROR,
            EVENT_DISCONNECT,
            // used on the server-side
            "disconnecting",
            "newListener",
            "removeListener",
        )
    }
}

private fun JsonElement.flatPrimitive(): Any {
    return when (this) {
        is JsonPrimitive -> {
            return when {
                isString -> content
                this is JsonNull -> "null"
                else -> {
                    val boolVal = jsonPrimitive.booleanOrNull
                    if (boolVal != null) {
                        return boolVal
                    }
                    val intVal = jsonPrimitive.intOrNull
                    if (intVal != null) {
                        return intVal
                    }
                    val longVal = jsonPrimitive.longOrNull
                    if (longVal != null) {
                        return longVal
                    }
                    val floatVal = jsonPrimitive.floatOrNull
                    if (floatVal != null) {
                        return floatVal
                    }
                    val doubleVal = jsonPrimitive.doubleOrNull
                    if (doubleVal != null) {
                        return doubleVal
                    }
                    Logging.error("JSON", "bad json primitive $this")
                    return 0
                }
            }
        }

        else -> this
    }
}

private fun <T> Array<T>.hasBinary(): Boolean {
    forEach {
        if (it is ByteString) {
            return true
        }
    }
    return false
}
