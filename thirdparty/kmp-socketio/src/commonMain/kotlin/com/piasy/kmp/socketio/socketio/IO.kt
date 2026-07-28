// CAMPFIRE PATCH (see the module README): the library-wide work scope survives child failures
// instead of crashing the app and silently dying.
package com.piasy.kmp.socketio.socketio

import com.piasy.kmp.socketio.global.supervisedScope
import com.piasy.kmp.xlog.Logging
import io.ktor.http.Url
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.jvm.JvmField
import kotlin.jvm.JvmStatic

object IO {
    class Options : Manager.Options() {
        @JvmField
        var forceNew = false

        /**
         * Whether to enable multiplexing. Default is true.
         */
        @JvmField
        var multiplex = true
    }

    private const val TAG = "IO"
    private val scope = supervisedScope(TAG, Dispatchers.Default.limitedParallelism(1, "siowkr"))
    private val managers = HashMap<String, Manager>()

    @JvmStatic
    fun socket(uri: String, opt: Options, block: (Socket) -> Unit) {
        scope.launch {
            Logging.info(TAG, "socket: uri $uri, opt $opt")
            val url = Url(uri)
            val id = "${url.protocol}://${url.host}:${url.port}"
            val sameNsp = managers.containsKey(id)
                    && managers[id]?.nsps?.containsKey(url.encodedPath) == true
            val newConn = opt.forceNew || !opt.multiplex || sameNsp

            // url queries will be handled in EngineSocket

            val io = if (newConn) {
                Logging.info(TAG, "socket newConn, sameNsp $sameNsp")
                Manager(uri, opt, scope)
            } else {
                managers.getOrElse(id) {
                    Logging.info(TAG, "socket not newConn, but create one")
                    val manager = Manager(uri, opt, scope)
                    managers[id] = manager
                    manager
                }
            }
            val socket = io.socket(if (url.segments.isEmpty()) "/" else url.encodedPath, opt.auth)
            block(socket)
        }
    }
}
