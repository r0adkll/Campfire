package app.campfire.socket.impl

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.hildan.socketio.SocketIO
import org.hildan.socketio.SocketIOPacket

/**
 * Guards the vendored socketio-kotlin copy in `org.hildan.socketio` (see the vendored SocketIO.kt
 * for why it exists). Both cases here crashed the app in the field with
 * [org.hildan.socketio.InvalidSocketIOPacketException] (Crashlytics issue
 * 4350da0dd8d479a67b4004bddebb06da) under different published versions of the library:
 *
 * 1. JavaScript's JSON.stringify leaves the line-terminator characters U+2028, U+2029, and U+0085
 *    unescaped inside strings, and the Audiobookshelf server emits them verbatim in socket payloads
 *    (e.g. podcast descriptions). socketio-kotlin < 2.7.0 parsed packets with a `.*` regex that
 *    stops at line terminators, so such payloads failed to decode.
 *
 * 2. socketio-kotlin 2.7.0+ fixed that with a `(.|[^.])*` payload regex, but that alternation
 *    backtracks per character: on large payloads (~70KB+ `user_updated`/`item_updated` events for
 *    users with big mediaProgress lists) it throws StackOverflowError on the desktop JVM and
 *    silently fails to match on Android's ICU regex engine, which the library reports as an
 *    invalid packet.
 *
 * The vendored copy's `[\s\S]*` payload regex handles both. If the vendored sources are ever
 * dropped in favor of an upstream release, these tests must still pass against it.
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

  @Test
  fun decodesLargeEventPayload() {
    // Mirrors the field crash: a `user_updated` event for a root user whose mediaProgress list
    // pushes the packet past the size where a backtracking payload regex falls over (~70KB on
    // the JVM). 200KB gives comfortable margin on every platform's regex engine.
    val progressEntries = buildString {
      while (length < 200_000) {
        if (isNotEmpty()) append(',')
        append(
          """{"id":"40a5cc02-076f-48d8-ba67-e6414dcb9ab6","libraryItemId":"59bae628-ed64-4232-b36f-76902289e914",""" +
            """"duration":74966.83,"progress":1,"currentTime":74966.9,"isFinished":true,"lastUpdate":1752043120894}""",
        )
      }
    }
    val encoded = """2["user_updated",{"id":"user-1","username":"tester","mediaProgress":[$progressEntries]}]"""

    val packet = SocketIO.decode(encoded) as SocketIOPacket.Event

    assertThat(packet.payload[0].jsonPrimitive.content).isEqualTo("user_updated")
    val user = packet.payload[1].jsonObject
    assertThat(user.getValue("id").jsonPrimitive.content).isEqualTo("user-1")
    val firstProgress = user.getValue("mediaProgress").jsonArray[0].jsonObject
    assertThat(firstProgress.getValue("isFinished").jsonPrimitive.content).isEqualTo("true")
  }
}
