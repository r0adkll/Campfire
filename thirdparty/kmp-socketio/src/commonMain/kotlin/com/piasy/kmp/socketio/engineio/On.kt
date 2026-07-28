package com.piasy.kmp.socketio.engineio

import com.piasy.kmp.socketio.emitter.Emitter

object On {
    fun on(emitter: Emitter, event: String, listener: Emitter.Listener): Handle {
        emitter.on(event, listener)
        return object : Handle {
            override fun destroy() {
                emitter.off(event, listener)
            }
        }
    }

    interface Handle {
        fun destroy()
    }
}
