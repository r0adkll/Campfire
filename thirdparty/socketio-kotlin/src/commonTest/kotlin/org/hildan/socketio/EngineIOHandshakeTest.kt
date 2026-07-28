// CAMPFIRE PATCH — this file is not part of upstream socketio-kotlin. It guards this module's
// build wiring: EngineIOPacket.Open is the vendored code's only @Serializable class, and its
// serializer only exists if the kotlinx-serialization compiler plugin is applied to this module.
// Without the plugin everything still compiles and every other test passes, but the Engine.IO
// handshake ('0' packet, the first frame of every connection) fails at runtime with a
// "serializer not found" error — surfacing only in the field, and looking like an R8/minification
// problem in release builds.
package org.hildan.socketio

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class EngineIOHandshakeTest {

    @Test
    fun decodesOpenHandshakePacket() {
        val encoded =
            """0{"sid":"lv_VI97HAXpY6yYWAAAC","upgrades":["websocket"],"pingInterval":25000,"pingTimeout":20000,"maxPayload":1000000}"""

        val packet = EngineIO.decodeSocketIO(encoded)

        val open = assertIs<EngineIOPacket.Open>(packet)
        assertEquals("lv_VI97HAXpY6yYWAAAC", open.sid)
        assertEquals(listOf("websocket"), open.upgrades)
        assertEquals(25000, open.pingInterval)
        assertEquals(20000, open.pingTimeout)
        assertEquals(1000000, open.maxPayload)
    }

    @Test
    fun encodesOpenHandshakePacket() {
        val open = EngineIOPacket.Open(
            sid = "abc123",
            upgrades = emptyList(),
            pingInterval = 25000,
            pingTimeout = 20000,
        )

        val encoded = EngineIO.encodeSocketIO(open)

        assertEquals(open, EngineIO.decodeSocketIO(encoded))
    }
}
