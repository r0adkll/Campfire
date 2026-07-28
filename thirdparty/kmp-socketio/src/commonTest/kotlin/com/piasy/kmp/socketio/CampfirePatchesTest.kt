// CAMPFIRE PATCH — this file is not part of upstream kmp-socketio. It guards the local patches
// documented in the module README; if an upstream re-sync reverts them, these tests fail instead
// of the app crashing in the field (Crashlytics issue 4350da0dd8d479a67b4004bddebb06da).
package com.piasy.kmp.socketio

import com.piasy.kmp.socketio.engineio.Transport
import com.piasy.kmp.socketio.engineio.transports.HttpClientFactory
import com.piasy.kmp.socketio.engineio.transports.WebSocket
import com.piasy.kmp.socketio.global.supervisedScope
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.statement.HttpResponse
import io.ktor.websocket.WebSocketSession
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest

class CampfirePatchesTest {

    /**
     * Guards the supervisedScope patch (IO.kt, WebSocket.kt, PollingXHR.kt): a coroutine failure
     * inside the library's internal scopes must neither crash the process nor cancel the scope.
     * With upstream's bare `CoroutineScope(dispatcher)` this test fails twice over: the thrown
     * exception escapes to the test harness, and the second launch never runs because the scope's
     * implicit job was cancelled by the first failure.
     */
    @Test
    fun supervisedScopeSurvivesChildFailure() = runTest {
        val scope = supervisedScope("test", StandardTestDispatcher(testScheduler))

        scope.launch { throw IllegalStateException("boom") }
        testScheduler.advanceUntilIdle()

        var ranAfterFailure = false
        scope.launch { ranAfterFailure = true }
        testScheduler.advanceUntilIdle()

        assertTrue(scope.isActive, "scope must survive a child failure")
        assertTrue(ranAfterFailure, "scope must keep accepting work after a child failure")
    }

    /**
     * Guards the WebSocket.onWsText patch: a frame whose Engine.IO envelope is valid but whose
     * Socket.IO payload is not (upstream only caught InvalidEngineIOPacketException) must surface
     * as a transport error event, not an exception escaping the socket scope. If the narrow catch
     * comes back in a re-sync, the InvalidSocketIOPacketException escapes into the test scope and
     * fails this test.
     */
    @Test
    fun webSocketRoutesSocketIODecodeErrorsToOnError() = runTest {
        val scope = CoroutineScope(StandardTestDispatcher(testScheduler))
        val factory = object : HttpClientFactory {
            override suspend fun createWs(
                url: String,
                request: HttpRequestBuilder.() -> Unit,
                block: suspend WebSocketSession.() -> Unit,
            ) = throw UnsupportedOperationException("not used in this test")

            override suspend fun httpRequest(
                url: String,
                block: HttpRequestBuilder.() -> Unit,
            ): HttpResponse = throw UnsupportedOperationException("not used in this test")
        }
        val ws = WebSocket(Transport.Options(), scope, factory = factory, rawMessage = false)
        val errors = mutableListOf<String>()
        ws.on(Transport.EVENT_ERROR) { args -> errors.add(args.firstOrNull().toString()) }

        // Engine.IO Message ('4') wrapping an invalid Socket.IO packet: upstream's narrow catch
        // let the resulting InvalidSocketIOPacketException crash the app.
        ws.onWsText("4x[not-a-socketio-packet")
        testScheduler.advanceUntilIdle()

        assertEquals(1, errors.size, "decode failure must surface exactly one transport error")
        assertTrue(errors[0].contains("decode error"), "unexpected error message: ${errors[0]}")
    }
}
