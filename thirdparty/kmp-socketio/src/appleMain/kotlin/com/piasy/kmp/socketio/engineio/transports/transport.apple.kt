package com.piasy.kmp.socketio.engineio.transports

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.darwin.Darwin
import platform.Foundation.NSURLCredential
import platform.Foundation.create
import platform.Foundation.serverTrust
import platform.Security.SecTrustRef

actual fun httpClient(trustAllCerts: Boolean, config: HttpClientConfig<*>.() -> Unit): HttpClient = HttpClient(Darwin) {
    config(this)
    engine {
        if (trustAllCerts) {
            handleChallenge { session, task, challenge, completionHandler ->
                val serverTrust: SecTrustRef? = challenge.protectionSpace.serverTrust
                if (serverTrust != null) {
                    val credential = NSURLCredential.create(trust = serverTrust)
                    completionHandler(0, credential)
                } else {
                    completionHandler(1, null)
                }
            }
        }
    }
}
