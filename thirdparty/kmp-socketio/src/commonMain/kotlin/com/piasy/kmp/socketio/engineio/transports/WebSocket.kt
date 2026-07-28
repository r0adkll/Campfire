// CAMPFIRE PATCHES (see the module README): onWsText catches all decode exceptions, and the
// default ioScope survives child failures instead of crashing the app.
package com.piasy.kmp.socketio.engineio.transports

import com.piasy.kmp.socketio.engineio.EngineSocket
import com.piasy.kmp.socketio.engineio.IoThread
import com.piasy.kmp.socketio.engineio.State
import com.piasy.kmp.socketio.engineio.Transport
import com.piasy.kmp.socketio.engineio.WorkThread
import com.piasy.kmp.socketio.global.supervisedScope
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.util.*
import io.ktor.websocket.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.io.bytestring.unsafe.UnsafeByteStringApi
import kotlinx.io.bytestring.unsafe.UnsafeByteStringOperations
import org.hildan.socketio.EngineIO
import org.hildan.socketio.EngineIOPacket
import org.hildan.socketio.SocketIOPacket

open class WebSocket(
    opt: Options,
    scope: CoroutineScope,
    private val ioScope: CoroutineScope = supervisedScope(NAME, Dispatchers.Default),
    private val factory: HttpClientFactory = DefaultHttpClientFactory(
        externalHttpClient = opt.httpClient,
        trustAllCerts = opt.trustAllCerts,
    ),
    rawMessage: Boolean,
) : Transport(opt, scope, NAME, rawMessage) {
    private var ws: WebSocketSession? = null

    @WorkThread
    override fun pause(onPause: () -> Unit) {
        // ws don't need to pause
    }

    @WorkThread
    override fun doOpen() {
        val uri = uri()
        logI("doOpen $uri")

        val requestHeaders = HashMap<String, List<String>>()
        requestHeaders.putAll(opt.extraHeaders)
        emit(EVENT_REQUEST_HEADERS, requestHeaders)

        ioScope.launch {
            try {
                factory.createWs(uri, {
                    headers {
                        putHeaders(this, requestHeaders)
                    }
                }) {
                    ws = this
                    if (this is DefaultClientWebSocketSession) {
                        val respHeaders = call.response.headers.toMap()
                        scope.launch {
                            emit(EVENT_RESPONSE_HEADERS, respHeaders)
                            onOpen()
                        }
                    }

                    listen()
                }
            } catch (e: Exception) {
                scope.launch { onError("ws exception: ${e.message}") }
            }
        }
    }

    @WorkThread
    protected open fun uri() = uri(SECURE_SCHEMA, INSECURE_SCHEMA)

    @IoThread
    private suspend fun listen() {
        while (true) {
            try {
                val frame = ws?.incoming?.receive() ?: break
                logD { "Receive frame: $frame" }
                when (frame) {
                    is Frame.Text -> {
                        onWsText(frame.readText())
                    }

                    is Frame.Binary -> {
                        onWsBinary(frame.readBytes())
                    }

                    is Frame.Close -> {
                        logI("Received Close frame")
                        break
                    }

                    else -> {
                        // ignore
                        //logI("Received unknown frame")
                    }
                }
            } catch (e: Exception) {
                logE("Receive error while reading websocket frame: `${e.message}`")
                break
            }
        }
        scope.launch { onClose() }
    }

    @IoThread
    // CAMPFIRE PATCH: internal (upstream: private) so the decode error handling is testable.
    internal fun onWsText(data: String) {
        scope.launch {
            logD { "onWsText: `$data`" }
            val packet = try {
                if (rawMessage) {
                    EngineIO.decodeWsFrame(data, deserializePayload = { it })
                } else {
                    EngineIO.decodeSocketIO(data)
                }
            } catch (e: Exception) {
                // CAMPFIRE PATCH: upstream catches only InvalidEngineIOPacketException here, so
                // InvalidSocketIOPacketException (and SerializationException from the handshake
                // payload) escaped the coroutine and crashed the app. Catch everything a bad frame
                // can throw and degrade to the transport error path, mirroring PollingXHR's
                // onPollComplete which already catches broadly.
                val log = "onWsText decode error: ${e.message}"
                logE(log)
                onError(log)
                return@launch
            }
            onPacket(packet)
        }
    }

    @OptIn(UnsafeByteStringApi::class)
    @IoThread
    private fun onWsBinary(data: ByteArray) {
        scope.launch {
            logD { "onWsBinary ${data.size} bytes" }
            onPacket(EngineIO.decodeWsFrame(UnsafeByteStringOperations.wrapUnsafe(data)))
        }
    }

    @OptIn(UnsafeByteStringApi::class)
    @WorkThread
    override fun doSend(packets: List<EngineIOPacket<*>>) {
        logD { "doSend ${packets.size} packets start" }
        writable = false

        // Check if this is a probe ping - if so, skip drain event to avoid race condition
        val isProbePing = packets.size == 1
            && packets[0] is EngineIOPacket.Ping
            && (packets[0] as EngineIOPacket.Ping).payload == EngineSocket.PROBE

        ioScope.launch {
            for (pkt in packets) {
                if (state != State.OPEN) {
                    // Ensure we don't try to send anymore packets
                    // if the socket ends up being closed due to an exception
                    break
                }
                try {
                    if (pkt is EngineIOPacket.BinaryData) {
                        UnsafeByteStringOperations.withByteArrayUnsafe(pkt.payload) {
                            logD { "doSend binary: ${it.size} bytes" }
                            ws?.send(it)
                        }
                    } else {
                        val data = if (rawMessage) {
                            EngineIO.encodeWsFrame(pkt, serializePayload = { it.toString() })
                        } else {
                            @Suppress("UNCHECKED_CAST")
                            EngineIO.encodeSocketIO(pkt as EngineIOPacket<SocketIOPacket>)
                        }
                        logD { "doSend: $pkt, `$data`" }
                        ws?.send(data)
                    }
                } catch (e: Exception) {
                    logE("doSend error: `${e.message}`")
                    //break
                    // TODO: send error is ignored, should we handle it?
                }
            }

            scope.launch {
                logD { "doSend ${packets.size} packets finish" }
                writable = true
                // Skip drain event for probe ping to avoid race condition between
                // probe ping drain and upgrade drain - see docs/ut-case-analysis/
                if (!isProbePing) {
                    emit(EVENT_DRAIN, packets.size)
                }
            }
        }
    }

    @WorkThread
    override fun doClose(fromOpenState: Boolean) {
        logI("doClose")
        ioScope.launch {
            ws?.close()
        }
    }

    companion object {
        const val NAME = "websocket"
        const val SECURE_SCHEMA = "wss"
        const val INSECURE_SCHEMA = "ws"
    }
}
