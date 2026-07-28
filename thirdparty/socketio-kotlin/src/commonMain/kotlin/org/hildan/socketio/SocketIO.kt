// Vendored from socketio-kotlin 2.8.0 (https://github.com/joffrey-bion/socketio-kotlin)
// MIT License, Copyright (c) 2023 Joffrey Bion.
//
// Vendored to patch `packetFormatRegex` below — see the comment on that property. Consumed by the
// vendored :thirdparty:kmp-socketio module in place of the published artifact. Every other file in
// this package is identical to upstream; keep it that way so future re-syncs are trivial diffs.
// Remove this module once upstream ships a release whose payload regex handles large packets.
package org.hildan.socketio

import kotlinx.serialization.*
import kotlinx.serialization.json.*

private object PacketTypes {
    const val Connect = 0
    const val Disconnect = 1
    const val Event = 2
    const val Ack = 3
    const val ConnectError = 4
    const val BinaryEvent = 5
    const val BinaryAck = 6
}

/**
 * The Socket.IO decoder, following the [Socket.IO protocol](https://socket.io/docs/v4/socket-io-protocol).
 */
object SocketIO {

    // CAMPFIRE PATCH: upstream matches the payload with `(.|[^.])*` because RegexOption.DOT_MATCHES_ALL
    // and inline `(?s)` aren't available in common code. That alternation backtracks per character, which
    // blows up on large payloads (~70KB+ `user_updated`/`item_updated` events from Audiobookshelf):
    // StackOverflowError on the desktop JVM, and a silent match failure on Android's ICU regex engine
    // that surfaced as InvalidSocketIOPacketException and crashed the app
    // (Crashlytics issue 4350da0dd8d479a67b4004bddebb06da). `[\s\S]` matches any character — including
    // the line terminators that motivated upstream's `(.|[^.])*` — without alternation, on all platforms.
    private val packetFormatRegex = Regex(
        """(?<packetType>\d)((?<nBinaryAttachments>\d+)-)?((?<namespace>/[^,]+),)?(?<ackId>\d+)?(?<payload>[\s\S]*)?""",
    )

    /**
     * Decodes the given [encodedData] into a [SocketIOPacket].
     *
     * The [encodedData] must be the pure Socket.IO packet data, not wrapped in an Engine.IO packet.
     *
     * Binary packet types are not supported. Binary attachments are passed in subsequent web socket frames
     * (1 frame per attachment), and thus cannot be handled in this single-frame decoding function.
     *
     * @throws InvalidSocketIOPacketException if the given [encodedData] is not a valid Socket.IO packet
     */
    fun decode(encodedData: String): SocketIOPacket = parseRawPacket(encodedData).toSocketIOPacket()

    private fun parseRawPacket(encodedData: String): RawPacket {
        val match = packetFormatRegex.matchEntire(encodedData)
            ?: throw InvalidSocketIOPacketException(encodedData, "Invalid Socket.IO packet: $encodedData")
        return RawPacket(
            encodedData = encodedData,
            packetType = match.groups["packetType"]?.value?.toInt()
                ?: error("Internal error: Socket.IO format regex was matched but the packetType group is missing"),
            nBinaryAttachments = match.groups["nBinaryAttachments"]?.value?.toInt() ?: 0,
            namespace = match.groups["namespace"]?.value ?: "/",
            ackId = match.groups["ackId"]?.value?.toInt(),
            payload = match.groups["payload"]?.value?.takeIf { it.isNotBlank() }?.let { parsePayload(encodedData, it) }
        )
    }

    private fun parsePayload(encodedData: String, payload: String) = try {
        Json.parseToJsonElement(payload)
    } catch (e: SerializationException) {
        throw InvalidSocketIOPacketException(encodedData, "The payload is not valid JSON: $payload", cause = e)
    }

    fun encode(packet: SocketIOPacket): String = packet.toRawPacket().encodeToString()

    private fun RawPacket.encodeToString() = buildString {
        append(packetType)
        if (nBinaryAttachments > 0) {
            append(nBinaryAttachments)
            append('-')
        }
        if (namespace != "/") {
            append(namespace)
            append(',')
        }
        if (ackId != null) {
            append(ackId)
        }
        if (payload != null) {
            append(Json.encodeToString(payload))
        }
    }
}

private data class RawPacket(
    val encodedData: String,
    val packetType: Int,
    val nBinaryAttachments: Int,
    val namespace: String,
    val ackId: Int?,
    val payload: JsonElement?,
)

private fun RawPacket.toSocketIOPacket(): SocketIOPacket = when (packetType) {
    PacketTypes.Connect -> SocketIOPacket.Connect(namespace, payload = connectPayload())
    PacketTypes.Disconnect -> SocketIOPacket.Disconnect(namespace)
    PacketTypes.Event -> SocketIOPacket.Event(namespace, ackId, payload = nonEmptyMessagePayload())
    PacketTypes.Ack -> SocketIOPacket.Ack(namespace, ackId = mandatoryAckId(), payload = messagePayload())
    PacketTypes.ConnectError -> SocketIOPacket.ConnectError(namespace, errorData = payload)
    PacketTypes.BinaryEvent -> SocketIOPacket.BinaryEvent(namespace, ackId, payload = nonEmptyMessagePayload().withPlaceholders(), nBinaryAttachments)
    PacketTypes.BinaryAck -> SocketIOPacket.BinaryAck(namespace, ackId = mandatoryAckId(), payload = messagePayload().withPlaceholders(), nBinaryAttachments)
    else -> invalid("Unknown Socket.IO packet type $packetType")
}

private fun SocketIOPacket.toRawPacket(): RawPacket = when (this) {
    is SocketIOPacket.Connect -> RawPacket(
        packetType = PacketTypes.Connect,
        nBinaryAttachments = 0,
        namespace = namespace,
        ackId = null,
        payload = payload,
        encodedData = "",
    )
    is SocketIOPacket.Disconnect -> RawPacket(
        packetType = PacketTypes.Disconnect,
        nBinaryAttachments = 0,
        namespace = namespace,
        ackId = null,
        payload = null,
        encodedData = "",
    )
    is SocketIOPacket.Event -> RawPacket(
        packetType = PacketTypes.Event,
        nBinaryAttachments = 0,
        namespace = namespace,
        ackId = ackId,
        payload = payload,
        encodedData = "",
    )
    is SocketIOPacket.Ack -> RawPacket(
        packetType = PacketTypes.Ack,
        nBinaryAttachments = 0,
        namespace = namespace,
        ackId = ackId,
        payload = payload,
        encodedData = "",
    )
    is SocketIOPacket.ConnectError -> RawPacket(
        packetType = PacketTypes.ConnectError,
        nBinaryAttachments = 0,
        namespace = namespace,
        ackId = null,
        payload = errorData,
        encodedData = "",
    )
    is SocketIOPacket.BinaryEvent -> RawPacket(
        packetType = PacketTypes.BinaryEvent,
        nBinaryAttachments = nBinaryAttachments,
        namespace = namespace,
        ackId = ackId,
        payload = payload.toJsonArray(),
        encodedData = "",
    )
    is SocketIOPacket.BinaryAck -> RawPacket(
        packetType = PacketTypes.BinaryAck,
        nBinaryAttachments = nBinaryAttachments,
        namespace = namespace,
        ackId = ackId,
        payload = payload.toJsonArray(),
        encodedData = "",
    )
}

private fun JsonArray.withPlaceholders(): List<PayloadElement> = map { it.toPayloadElement() }

private fun JsonElement.toPayloadElement(): PayloadElement = if (isBinaryPlaceholder) {
    val attachmentIndex = jsonObject.getValue("num").jsonPrimitive.int
    PayloadElement.AttachmentRef(attachmentIndex)
} else {
    PayloadElement.Json(this)
}

private val JsonElement.isBinaryPlaceholder: Boolean
    get() = this is JsonObject
        && size == 2
        && (get("_placeholder") as? JsonPrimitive)?.booleanOrNull == true
        && (get("num") as? JsonPrimitive)?.intOrNull != null

private fun List<PayloadElement>.toJsonArray(): JsonArray = JsonArray(map { it.toJsonElement() })

private fun PayloadElement.toJsonElement(): JsonElement = when (this) {
    is PayloadElement.Json -> jsonElement
    is PayloadElement.AttachmentRef -> buildJsonObject {
        put("_placeholder", true)
        put("num", JsonPrimitive(attachmentIndex))
    }
}

// This is not clearly documented in the protocol spec, but the payload for CONNECT has been added in v5 and the
// official socket.io-parser v4 (for protocol v5) has the following validation which expects JSON object in this case:
// https://github.com/socketio/socket.io-parser/blob/164ba2a11edc34c2f363401e9768f9a8541a8b89/lib/index.ts#L285-L305
private fun RawPacket.connectPayload(): JsonObject? {
    if (payload == null) {
        return null
    }
    return payload as? JsonObject ?: invalid("The payload for CONNECT packets must be a JSON object")
}

// Reference:
// https://socket.io/docs/v4/socket-io-protocol#sending-and-receiving-data
// https://socket.io/docs/v4/socket-io-protocol#acknowledgement
private fun RawPacket.messagePayload(): JsonArray {
    if (payload == null) {
        invalid("The payload for EVENT and ACK packets is mandatory")
    }
    if (payload !is JsonArray) {
        invalid("The payload for EVENT and ACK packets must be a JSON array")
    }
    return payload
}

// Reference: https://socket.io/docs/v4/socket-io-protocol#sending-and-receiving-data
private fun RawPacket.nonEmptyMessagePayload(): JsonArray {
    val payload = messagePayload()
    if (payload.isEmpty()) {
        invalid("The array payload for EVENT packets must not be empty")
    }
    return payload
}

private fun RawPacket.mandatoryAckId() = ackId ?: invalid("ACK packet without an Ack ID")

private fun RawPacket.invalid(message: String): Nothing = throw InvalidSocketIOPacketException(encodedData, message)

/**
 * An exception thrown when some encoded data doesn't represent a valid Socket.IO packet as defined by the
 * [Socket.IO protocol](https://socket.io/docs/v4/socket-io-protocol).
 */
class InvalidSocketIOPacketException(
    val encodedData: String,
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)
