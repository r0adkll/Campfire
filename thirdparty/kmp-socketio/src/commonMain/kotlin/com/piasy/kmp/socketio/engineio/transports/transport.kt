package com.piasy.kmp.socketio.engineio.transports

import com.piasy.kmp.socketio.engineio.Transport
import io.ktor.client.HttpClientConfig
import io.ktor.client.HttpClient
import io.ktor.client.plugins.logging.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.websocket.WebSocketSession
import kotlinx.coroutines.CoroutineScope

expect fun httpClient(trustAllCerts: Boolean = false, config: HttpClientConfig<*>.() -> Unit = {}): HttpClient

internal fun putHeaders(
    builder: HeadersBuilder,
    headers: Map<String, List<String>>
) {
    headers.forEach {
        it.value.forEach { v ->
            builder.append(it.key, v)
        }
    }
}

interface TransportFactory {
    fun create(
        name: String,
        opt: Transport.Options,
        scope: CoroutineScope,
        rawMessage: Boolean,
    ): Transport
}

object DefaultTransportFactory : TransportFactory {
    override fun create(
        name: String,
        opt: Transport.Options,
        scope: CoroutineScope,
        rawMessage: Boolean,
    ) = when (name) {
        WebSocket.NAME -> WebSocket(opt, scope, rawMessage = rawMessage)
        PollingXHR.NAME -> PollingXHR(opt, scope, rawMessage = rawMessage)
        else -> throw RuntimeException("Bad transport name: $name")
    }
}

interface HttpClientFactory {
    suspend fun createWs(
        url: String,
        request: HttpRequestBuilder.() -> Unit,
        block: suspend WebSocketSession.() -> Unit,
    )

    suspend fun httpRequest(
        url: String,
        block: HttpRequestBuilder.() -> Unit
    ): HttpResponse
}

class DefaultHttpClientFactory(
    externalHttpClient: HttpClient? = null,
    trustAllCerts: Boolean = false,
): HttpClientFactory {
    private val wsClient = externalHttpClient ?: run {
        httpClient(
            trustAllCerts = trustAllCerts,
        ) {
            install(Logging) {
                logger = object : Logger {
                    override fun log(message: String) {
                        com.piasy.kmp.xlog.Logging.info("Net", message)
                    }
                }
                level = LogLevel.ALL
            }
            install(WebSockets) {
                pingIntervalMillis = 20_000
            }
        }
    }
    // Linux curl engine doesn't work for simultaneous websocket and http request.
    // see https://youtrack.jetbrains.com/issue/KTOR-8259/
    // But it's fixed in 3.2.0.
    private val httpClient: HttpClient = wsClient

    override suspend fun createWs(
        url: String,
        request: HttpRequestBuilder.() -> Unit,
        block: suspend WebSocketSession.() -> Unit,
    ) = wsClient.webSocket(url, request, block)

    override suspend fun httpRequest(
        url: String,
        block: HttpRequestBuilder.() -> Unit
    ) = httpClient.request(url, block)
}
