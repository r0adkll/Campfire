// CAMPFIRE PATCH — this file is not part of upstream kmp-socketio.
package com.piasy.kmp.socketio.global

import com.piasy.kmp.xlog.Logging
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

/**
 * A [CoroutineScope] for the library's internal workers that survives child failures.
 *
 * Upstream uses bare `CoroutineScope(dispatcher)`, which fails in two ways when a launched
 * coroutine throws: the exception reaches the platform's default uncaught-exception handler and
 * crashes the app (this is how Crashlytics issue 4350da0dd8d479a67b4004bddebb06da surfaced), and
 * the scope's implicit job is cancelled, silently killing every future socket coroutine. Known
 * decode errors are caught at their source (see WebSocket.onWsText), and this scope backstops
 * anything unanticipated: the failure degrades to a logged error while the socket keeps running.
 */
internal fun supervisedScope(name: String, context: CoroutineContext): CoroutineScope =
    CoroutineScope(
        context + SupervisorJob() + CoroutineExceptionHandler { _, e ->
            Logging.error(name, "Uncaught exception in socket coroutine: $e")
        },
    )
