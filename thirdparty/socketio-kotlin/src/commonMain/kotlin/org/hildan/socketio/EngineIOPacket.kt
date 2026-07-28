// Vendored from socketio-kotlin 2.8.0 (https://github.com/joffrey-bion/socketio-kotlin)
// MIT License, Copyright (c) 2023 Joffrey Bion. Unmodified — see SocketIO.kt for why this
// package is vendored.
package org.hildan.socketio

import kotlinx.io.bytestring.ByteString
import kotlinx.serialization.Serializable

/**
 * An Engine.IO packet, as defined in the [Engine.IO protocol](https://socket.io/docs/v4/engine-io-protocol).
 */
sealed interface EngineIOPacket<out T> {

    /**
     * Used during the handshake.
     */
    @Serializable
    data class Open(
        /** The session ID. */
        val sid: String,
        /** The list of available transport upgrades. */
        val upgrades: List<String>,
        /** The ping interval, used in the heartbeat mechanism (in milliseconds). */
        val pingInterval: Int,
        /** The ping timeout, used in the heartbeat mechanism (in milliseconds). */
        val pingTimeout: Int,
        /** Optional, only useful when using long-polling as transport to know how many packets to batch together. */
        val maxPayload: Int? = null,
    ) : EngineIOPacket<Nothing>

    /**
     * Used to indicate that a transport can be closed.
     */
    data object Close : EngineIOPacket<Nothing>

    /**
     * Used during the upgrade process.
     */
    data object Upgrade : EngineIOPacket<Nothing>

    /**
     * Used during the upgrade process.
     */
    data object Noop : EngineIOPacket<Nothing>

    /**
     * Used in the heartbeat mechanism.
     */
    data class Ping(val payload: String?) : EngineIOPacket<Nothing>

    /**
     * Used in the heartbeat mechanism.
     */
    data class Pong(val payload: String?) : EngineIOPacket<Nothing>

    /**
     * Used to send a text payload to the other side.
     */
    data class Message<T>(val payload: T) : EngineIOPacket<T>

    /**
     * Raw binary data sent as a pure binary web socket frame, or as a base64 record in HTTP long-polling mode.
     */
    data class BinaryData(val payload: ByteString) : EngineIOPacket<Nothing>
}
