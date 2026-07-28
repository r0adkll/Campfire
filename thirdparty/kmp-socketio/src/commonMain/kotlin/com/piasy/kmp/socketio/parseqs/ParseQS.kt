package com.piasy.kmp.socketio.parseqs

import com.piasy.kmp.socketio.global.Global
import kotlin.jvm.JvmStatic

object ParseQS {
    @JvmStatic
    fun encode(obj: Map<String, String>): String {
        val str = StringBuilder()
        for ((k, v) in obj) {
            if (str.isNotEmpty()) {
                str.append('&')
            }
            str.append(Global.encodeURIComponent(k))
                .append('=')
                .append(Global.encodeURIComponent(v))
        }
        return str.toString()
    }

    @JvmStatic
    fun decode(qs: String): Map<String, String> {
        val qry = HashMap<String, String>()
        for (kv in qs.split('&')) {
            val pair = kv.split('=')
            qry[Global.decodeURIComponent(pair[0])] = if (pair.size == 2) {
                Global.decodeURIComponent(pair[1])
            } else {
                ""
            }
        }
        return qry
    }
}
