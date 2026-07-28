// CAMPFIRE PATCH (see the module README): the default ioScope survives child failures instead of
// crashing the app.
package com.piasy.kmp.socketio.engineio.transports

import com.piasy.kmp.socketio.engineio.IoThread
import com.piasy.kmp.socketio.engineio.State
import com.piasy.kmp.socketio.engineio.Transport
import com.piasy.kmp.socketio.engineio.WorkThread
import com.piasy.kmp.socketio.global.supervisedScope
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.util.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.hildan.socketio.*

open class PollingXHR(
    opt: Options,
    scope: CoroutineScope,
    private val ioScope: CoroutineScope = supervisedScope(NAME, Dispatchers.Default),
    private val factory: HttpClientFactory = DefaultHttpClientFactory(
        externalHttpClient = opt.httpClient,
        trustAllCerts = opt.trustAllCerts,
    ),
    rawMessage: Boolean,
) : Transport(opt, scope, NAME, rawMessage) {
    private var polling = false

    @WorkThread
    override fun pause(onPause: () -> Unit) {
        logI("pause")
        state = State.PAUSED
        val paused = {
            logI("paused")
            state = State.PAUSED
            onPause()
        }

        if (polling || !writable) {
            var counter = 0
            val waitJob: (String, String) -> Unit = { event, job ->
                logI("pause: wait $job")
                counter++
                once(event, object : Listener {
                    override fun call(vararg args: Any) {
                        logI("pause: pre-pause $job complete")
                        counter--
                        if (counter == 0) {
                            paused()
                        }
                    }
                })
            }

            if (polling) {
                waitJob(EVENT_POLL_COMPLETE, "polling")
            }
            if (!writable) {
                waitJob(EVENT_DRAIN, "writing")
            }
        } else {
            paused()
        }
    }

    @WorkThread
    override fun doOpen() {
        poll()
    }

    @WorkThread
    private fun poll() {
        logD { "poll start" }
        polling = true

        val method = HttpMethod.Get
        val headers = prepareRequestHeaders(method)
        ioScope.launch {
            doRequest(uri(), method, headers, onResponse = { onPollComplete(it) })
        }
        emit(EVENT_POLL)
    }

    @WorkThread
    private fun prepareRequestHeaders(method: HttpMethod): Map<String, List<String>> {
        val requestHeaders = HashMap<String, List<String>>()
        requestHeaders.putAll(opt.extraHeaders)
        if (method == HttpMethod.Post) {
            requestHeaders["Content-type"] = listOf("text/plain;charset=UTF-8")
        }
        requestHeaders["Accept"] = listOf("*/*")
        emit(EVENT_REQUEST_HEADERS, requestHeaders)
        return requestHeaders
    }

    @IoThread
    private suspend fun doRequest(
        uri: String,
        method: HttpMethod,
        requestHeaders: Map<String, List<String>>,
        data: String? = null,
        onResponse: (String) -> Unit = {},
        onSuccess: () -> Unit = {},
    ) {
        logD { "doRequest ${method.value} $uri, data $data, headers $requestHeaders" }
        val resp = try {
            factory.httpRequest(uri) {
                this.method = method

                headers {
                    putHeaders(this, requestHeaders)
                }

                if (data != null) {
                    setBody(data)
                }
            }
        } catch (e: Exception) {
            scope.launch { onError("http exception: ${e.message}") }
            return
        }

        logD { "doRequest ${method.value} $uri response: ${resp.status}" }
        scope.launch {
            emit(EVENT_RESPONSE_HEADERS, resp.headers.toMap())
        }
        if (resp.status.isSuccess()) {
            val body = resp.bodyAsText()
            scope.launch {
                onResponse(body)
                onSuccess()
            }
        } else {
            scope.launch {
                onError("HTTP error: ${resp.status}")
            }
        }
    }

    @WorkThread
    private fun onPollComplete(data: String) {
        logD { "onPollComplete: state $state, `$data`" }
        val packets = try {
            if (rawMessage) {
                EngineIO.decodeHttpBatch(data, deserializePayload = { it })
            } else {
                EngineIO.decodeHttpBatch(data, SocketIO::decode)
            }
        } catch (e: Exception) { // InvalidSocketIOPacketException | InvalidEngineIOPacketException
            val log = "onPollComplete decode error: ${e.message}"
            logE(log)
            onError(log)
            return
        }
        for (pkt in packets) {
            if ((state == State.OPENING || state == State.CLOSING) && pkt is EngineIOPacket.Open) {
                onOpen()
            }

            if (pkt is EngineIOPacket.Close) {
                onClose()
                break
            }

            onPacket(pkt)
        }

        if (state != State.CLOSED) {
            polling = false
            emit(EVENT_POLL_COMPLETE)

            if (state == State.OPEN) {
                poll()
            } else {
                logI("onPollComplete ignore poll, state $state")
            }
        }
    }

    @WorkThread
    override fun doSend(packets: List<EngineIOPacket<*>>) {
        writable = false
        val data = if (rawMessage) {
            EngineIO.encodeHttpBatch(packets, serializePayload = { it.toString() })
        } else {
            EngineIO.encodeHttpBatch(packets, serializePayload = { SocketIO.encode(it as SocketIOPacket) })
        }

        val method = HttpMethod.Post
        val headers = prepareRequestHeaders(method)
        ioScope.launch {
            doRequest(uri(), method, headers, data) {
                writable = true
                emit(EVENT_DRAIN, packets.size)
            }
        }
    }

    @WorkThread
    override fun doClose(fromOpenState: Boolean) {
        val doCloseAction: () -> Unit = {
            logI("doClose writing close packet")
            once(EVENT_DRAIN, object : Listener {
                override fun call(vararg args: Any) {
                    onClose()
                }
            })
            doSend(listOf(EngineIOPacket.Close))
        }

        if (fromOpenState) {
            logI("doClose on OPEN state, closing")
            doCloseAction()
        } else {
            logI("doClose on OPENING state, deferring close")
            once(EVENT_OPEN, object : Listener {
                override fun call(vararg args: Any) {
                    doCloseAction()
                }
            })
        }
    }

    @WorkThread
    protected open fun uri() = uri(SECURE_SCHEMA, INSECURE_SCHEMA)

    companion object {
        const val NAME = "polling"
        const val SECURE_SCHEMA = "https"
        const val INSECURE_SCHEMA = "http"

        const val EVENT_POLL = "poll"
        const val EVENT_POLL_COMPLETE = "pollComplete"
    }
}
