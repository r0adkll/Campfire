package com.piasy.kmp.socketio.global

import io.ktor.http.*
import kotlin.jvm.JvmStatic

object Global {
    @JvmStatic
    fun encodeURIComponent(str: String): String {
        return str.encodeURLParameter()
            .replace("+", "%20")
            .replace("%21", "!")
            .replace("%27", "'")
            .replace("%28", "(")
            .replace("%29", ")")
            .replace("%7E", "~")
    }

    @JvmStatic
    fun decodeURIComponent(str: String): String {
        return str.decodeURLQueryComponent()
    }
}
