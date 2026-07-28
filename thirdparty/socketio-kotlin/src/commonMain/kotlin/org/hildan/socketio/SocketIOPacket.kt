// Vendored from socketio-kotlin 2.8.0 (https://github.com/joffrey-bion/socketio-kotlin)
// MIT License, Copyright (c) 2023 Joffrey Bion. Unmodified — see SocketIO.kt for why this
// package is vendored.
package org.hildan.socketio

import kotlinx.serialization.json.*

/**
 * A Socket.IO packet, as defined in the [Socket.IO protocol](https://socket.io/docs/v4/socket-io-protocol).
 *
 * Exact payload types can be checked against the
 * [reference implementation](https://github.com/socketio/socket.io-parser/blob/87236baf87cdbe32ae01e7dc53320474520ce82f/lib/index.ts#L280).
 */
sealed interface SocketIOPacket {

    /**
     * The namespace this packet belongs to, useful when multiplexing.
     * The default namespace is "/".
     */
    val namespace: String

    /**
     * Used during the connection to a namespace.
     */
    data class Connect(
        override val namespace: String,
        /**
         * Since v5, CONNECT messages can have a payload as a JSON object.
         */
        val payload: JsonObject?,
    ) : SocketIOPacket

    /**
     * Used during the connection to a namespace.
     */
    data class ConnectError(
        override val namespace: String,
        /**
         * A description of the error. It was a JSON string literal before v5, and is a JSON object since v5.
         */
        val errorData: JsonElement?,
    ) : SocketIOPacket

    /**
     * Used when disconnecting from a namespace.
     */
    data class Disconnect(
        override val namespace: String,
    ) : SocketIOPacket

    /**
     * A parent for message packet types.
     */
    sealed interface Message : SocketIOPacket {
        /**
         * An ID used to match an [Event] with the corresponding [Ack].
         * When an [ackId] is present in an [Event] packet, it means an [Ack] packet is expected by the sender.
         */
        val ackId: Int?
    }

    /**
     * A parent for packet types having a payload.
     */
    sealed interface JsonMessage : Message {
        /**
         * The payload of the message, which must be a non-empty array.
         * Usually the first element of the array is the event type (string or int), and the rest is the actual data.
         */
        val payload: JsonArray
    }

    /**
     * Used to [send data](https://socket.io/docs/v4/socket-io-protocol#sending-and-receiving-data) to the other side.
     */
    data class Event(
        override val namespace: String,
        override val ackId: Int?,
        override val payload: JsonArray,
    ): JsonMessage

    /**
     * Used to acknowledge the event with the corresponding [ackId].
     */
    data class Ack(
        override val namespace: String,
        override val ackId: Int,
        override val payload: JsonArray,
    ): JsonMessage

    sealed interface BinaryMessage : Message {
        /**
         * The number of binary attachments that follow this message.
         *
         * The [payload] of this message must contain this number of placeholder JSON elements that should be replaced
         * with the binary data from the following binary frames.
         */
        val nBinaryAttachments: Int

        /**
         * The payload of the message, which must be a non-empty array.
         * Usually the first element of the array is the event type (string or int), and the rest is the actual data.
         *
         * This payload contains, in addition to potential other values, [nBinaryAttachments] elements of type
         * [PayloadElement.AttachmentRef] that act as placeholders for the binary data in the [nBinaryAttachments]
         * subsequent binary frames.
         */
        val payload: List<PayloadElement>
    }

    /**
     * Used to send binary data to the other side.
     */
    data class BinaryEvent(
        override val namespace: String,
        override val ackId: Int?,
        override val payload: List<PayloadElement>,
        override val nBinaryAttachments: Int,
    ): BinaryMessage

    /**
     * Used to acknowledge an event (when the response includes binary data).
     */
    data class BinaryAck(
        override val namespace: String,
        override val ackId: Int,
        override val payload: List<PayloadElement>,
        override val nBinaryAttachments: Int,
    ): BinaryMessage
}

sealed interface PayloadElement {
    data class Json(val jsonElement: JsonElement) : PayloadElement
    data class AttachmentRef(val attachmentIndex: Int) : PayloadElement
}
