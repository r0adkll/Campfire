package com.piasy.kmp.socketio.socketio

import io.ktor.util.date.GMTDate
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.max
import kotlin.math.pow
import kotlin.random.Random

/**
 * Initialize backoff timer with `opts`.
 * - `min` initial timeout in milliseconds [100]
 * - `max` max timeout [10000]
 * - `jitter` [0]
 * - `factor` [2]
 */
class Backoff(
    var min: Long = 100,
    var max: Long = 10000,
    jitter: Double = 0.0,
    private val factor: Double = 2.0,
) {
    private val rand = Random(GMTDate().timestamp)

    init {
        checkValidJitter(jitter)
    }

    var attempts: Int = 0
        private set

    var jitter: Double = jitter
        set(value) {
            checkValidJitter(value)
            field = value
        }

    /** Return the backoff duration. */
    val duration: Long
        get() {
            var ms = min * factor.pow(attempts++)
            if (jitter > 0.0) {
                val rand = rand.nextDouble()
                val deviation = rand * jitter * ms
                ms = if (((floor(rand * 10).toInt()).and(1)) == 0) (ms - deviation) else (ms + deviation)
            }
            return max(min(ms.toLong(), max), min)
        }

    /** Reset the number of attempts. */
    fun reset(): Int {
        val oldAttempts = attempts
        attempts = 0
        return oldAttempts
    }

    private fun checkValidJitter(jitter: Double) {
        require(jitter in 0.0..1.0) {
            throw IllegalArgumentException("jitter should be between 0.0 and 1.0, but $jitter")
        }
    }
}
