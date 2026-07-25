package app.campfire.socket.impl

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.hildan.socketio.SocketIO
import org.hildan.socketio.SocketIOPacket

/**
 * Guards the forced socketio-kotlin upgrade in this module's build file.
 *
 * JavaScript's JSON.stringify leaves the line-terminator characters U+2028, U+2029, and U+0085
 * unescaped inside strings, and the Audiobookshelf server emits them verbatim in socket payloads
 * (e.g. podcast descriptions). socketio-kotlin 2.6.0 — the version kmp-socketio 1.4.4 pulls in —
 * parses packets with a `.*` regex that stops at line terminators, so such payloads threw
 * [org.hildan.socketio.InvalidSocketIOPacketException] and crashed the app
 * (Crashlytics issue 4350da0dd8d479a67b4004bddebb06da). If a dependency change ever rolls the
 * library back below 2.8.0, this test fails instead of the app crashing in the field.
 */
class SocketIOLineTerminatorTest {

  @Test
  fun decodesEventPayloadContainingLineTerminatorCharacters() {
    val description = "We open at 6.\u2028Suddenly there is a diner.\u2029Somewhere new\u0085in the cosmos."
    val encoded = """2["item_updated",{"id":"item-1","description":"$description"}]"""

    val packet = SocketIO.decode(encoded) as SocketIOPacket.Event

    assertThat(packet.payload[0].jsonPrimitive.content).isEqualTo("item_updated")
    val item = packet.payload[1].jsonObject
    assertThat(item.getValue("id").jsonPrimitive.content).isEqualTo("item-1")
    assertThat(item.getValue("description").jsonPrimitive.content).isEqualTo(description)
  }
}
