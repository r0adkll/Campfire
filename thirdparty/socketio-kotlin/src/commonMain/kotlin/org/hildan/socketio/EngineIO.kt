// Vendored from socketio-kotlin 2.8.0 (https://github.com/joffrey-bion/socketio-kotlin)
// MIT License, Copyright (c) 2023 Joffrey Bion. Unmodified — see SocketIO.kt for why this
// package is vendored.
package org.hildan.socketio

import kotlinx.io.bytestring.*
import kotlinx.serialization.json.*
import kotlin.io.encoding.*

/**
 * The Engine.IO decoder, following the [Engine.IO protocol](https://socket.io/docs/v4/engine-io-protocol).
 */
object EngineIO {

    /**
     * Decodes the given [textFrame] as a single [EngineIOPacket].
     *
     * If this packet is a [EngineIOPacket.Message] packet, the payload text is deserialized as a [SocketIOPacket].
     *
     * This is meant to be used in web socket mode, where each web socket frame contains a single Engine.IO packet.
     * When using HTTP long-polling with batched packets, use [decodeHttpBatch] instead.
     *
     * @throws InvalidEngineIOPacketException if the given [textFrame] is not a valid Engine.IO packet
     * @throws InvalidSocketIOPacketException if the given [textFrame] does not contain a valid Socket.IO packet
     */
    fun decodeSocketIO(textFrame: String): EngineIOPacket<SocketIOPacket> = decodeWsFrame(textFrame, SocketIO::decode)

    /**
     * Decodes the given web socket [text] frame as a single [EngineIOPacket].
     *
     * If this packet is a [EngineIOPacket.Message] packet, the payload text is deserialized using the provided
     * [deserializePayload] function.
     *
     * This is meant to be used in web socket mode, where each web socket frame contains a single Engine.IO packet.
     * When using HTTP long-polling with batched packets, use [decodeHttpBatch] instead.
     *
     * @throws InvalidEngineIOPacketException if the given [text] is not a valid Engine.IO packet
     */
    fun <T> decodeWsFrame(text: String, deserializePayload: (String) -> T): EngineIOPacket<T> = decodeSinglePacket(
        encodedData = text,
        deserializePayload = deserializePayload,
    ).also {
        if (it is EngineIOPacket.BinaryData) {
            // Base64-encoded binary payloads are supported with 'b' prefix in long-polling mode.
            // In web socket mode, binary messages must be sent as separate binary frames.
            throw InvalidEngineIOPacketException(text, "Unexpected binary payload in web socket text frame")
        }
    }

    /**
     * Decodes the given binary web socket frame's [bytes] as a single [EngineIOPacket.BinaryData].
     *
     * As specified by the protocol, binary messages are just sent as-is, so there is no real decoding involved in this
     * function, it just wraps the bytes in a packet type for consistency.
     *
     * This is meant to be used in web socket mode, where each web socket frame contains a single Engine.IO packet.
     * When using HTTP long-polling with batched packets, use [decodeHttpBatch] instead.
     */
    fun decodeWsFrame(bytes: ByteString): EngineIOPacket.BinaryData = EngineIOPacket.BinaryData(bytes)

    /**
     * Decodes the given [batch] text as a batch of [EngineIOPacket]s.
     *
     * Individual packets in [batch] must be delimited by the "record separator" (U+001E) character, as defined in
     * the [specification](https://socket.io/docs/v4/engine-io-protocol#http-long-polling-1).
     *
     * If a packet is a [EngineIOPacket.Message] packet, the payload text is deserialized using the provided
     * [deserializePayload] function.
     *
     * This is meant to be used in HTTP long-polling mode, where packets are batched in a single HTTP response.
     * When using web sockets, use [decodeWsFrame] on each frame instead.
     *
     * @throws InvalidEngineIOPacketException if the given [batch] contains an invalid Engine.IO packet
     */
    fun <T> decodeHttpBatch(batch: String, deserializePayload: (String) -> T): List<EngineIOPacket<T>> {
        // Splitting on the "record-separator" character as defined by the protocol:
        // https://socket.io/docs/v4/engine-io-protocol#http-long-polling-1
        return batch.split("\u001e").map {
            decodeSinglePacket(it, deserializePayload)
        }
    }

    // Base64-encoded binary payloads are supported with 'b' prefix in long-polling mode.
    // In web socket mode, binary messages should be sent as separate binary frames.
    @OptIn(ExperimentalEncodingApi::class)
    private fun <T> decodeSinglePacket(encodedData: String, deserializePayload: (String) -> T): EngineIOPacket<T> {
        if (encodedData.isBlank()) {
            throw InvalidEngineIOPacketException(encodedData, "The Engine.IO packet is empty")
        }
        val payload = encodedData.drop(1)
        return when (val packetType = encodedData[0]) {
            '0' -> Json.decodeFromString<EngineIOPacket.Open>(payload)
            '1' -> EngineIOPacket.Close
            '2' -> EngineIOPacket.Ping(payload = payload.takeIf { it.isNotEmpty() })
            '3' -> EngineIOPacket.Pong(payload = payload.takeIf { it.isNotEmpty() })
            '4' -> EngineIOPacket.Message(payload = deserializePayload(payload))
            '5' -> EngineIOPacket.Upgrade
            '6' -> EngineIOPacket.Noop
            'b' -> EngineIOPacket.BinaryData(payload = Base64.decodeToByteString(payload))
            else -> throw InvalidEngineIOPacketException(encodedData, "Unknown Engine.IO packet type '$packetType'")
        }
    }

    /**
     * Encodes the given SocketIO [packet] (wrapped in an EngineIO packet) as a string.
     *
     * The result can be directly used as a web socket text frame, unless it's a binary message.
     * If the packet is a binary message, the resulting text can be sent as a web socket text frame, but must be
     * followed by as many binary frames as there are binary attachments, in the order defined by their indices.
     *
     * This is meant to be used in web socket mode, where each web socket frame contains a single Engine.IO packet.
     */
    fun encodeSocketIO(packet: EngineIOPacket<SocketIOPacket>): String = encodeWsFrame(
        packet = packet,
        serializePayload = { SocketIO.encode(it) }
    )

    /**
     * Encodes the given list of EngineIO [packets] to a text, which can be used as the body of an HTTP batch request.
     *
     * For each [EngineIOPacket.Message] packet, the payload is serialized using the provided [serializePayload]
     * function.
     *
     * This is meant to be used in HTTP long-polling mode, where packets are batched in a single HTTP request.
     * When using web sockets, use [encodeWsFrame] on each frame instead.
     */
    fun <T> encodeHttpBatch(packets: List<EngineIOPacket<T>>, serializePayload: (T) -> String): String =
        // Joining with the "record-separator" character as defined by the protocol:
        // https://socket.io/docs/v4/engine-io-protocol#http-long-polling-1
        packets.joinToString("\u001e") {
            encodeSinglePacket(it, serializePayload)
        }

    /**
     * Encodes the given [EngineIOPacket] to a string, which can be used as a text web socket frame body.
     *
     * If this packet is a [EngineIOPacket.Message] packet, the payload is serialized using the provided
     * [serializePayload] function.
     *
     * This is meant to be used in web socket mode, where each web socket frame contains a single Engine.IO packet.
     * Binary packets are not allowed because the raw data should just be sent directly as a binary web socket frame.
     */
    @OptIn(ExperimentalEncodingApi::class)
    fun <T> encodeWsFrame(packet: EngineIOPacket<T>, serializePayload: (T) -> String): String {
        require(packet !is EngineIOPacket.BinaryData) {
            "In web socket mode, binary data must be transferred directly as a binary web socket frame, not " +
                "encoded in a text frame. Use EngineIOPacket.BinaryData when calling encodeHttpBatch()."
        }
        return encodeSinglePacket(packet = packet, serializePayload = serializePayload)
    }

    /**
     * Encodes the given [EngineIOPacket] to a [ByteString], which can be used as a binary web socket frame body.
     *
     * This is meant to be used in web socket mode, where the raw data is sent directly as a binary web socket frame.
     * Binary data is only encoded as base64 when using the HTTP long-polling mode (see [encodeHttpBatch]).
     */
    @OptIn(ExperimentalEncodingApi::class)
    fun encodeWsFrame(packet: EngineIOPacket.BinaryData): ByteString = packet.payload

    @OptIn(ExperimentalEncodingApi::class)
    private fun <T> encodeSinglePacket(packet: EngineIOPacket<T>, serializePayload: (T) -> String): String =
        when (packet) {
            is EngineIOPacket.Open -> "0${Json.encodeToString(packet)}"
            is EngineIOPacket.Close -> "1"
            is EngineIOPacket.Ping -> "2${packet.payload ?: ""}"
            is EngineIOPacket.Pong -> "3${packet.payload ?: ""}"
            is EngineIOPacket.Message<T> -> "4${serializePayload(packet.payload)}"
            is EngineIOPacket.BinaryData -> "b${Base64.encode(packet.payload)}"
            is EngineIOPacket.Upgrade -> "5"
            is EngineIOPacket.Noop -> "6"
        }
}

/**
 * An exception thrown when encoded data doesn't represent a valid Engine.IO packet as defined by the
 * [Engine.IO protocol](https://socket.io/docs/v4/engine-io-protocol).
 */
class InvalidEngineIOPacketException(val encodedData: String, message: String) : Exception(message)
