package app.campfire.audioplayer.impl.util

import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreMedia.CMTime
import platform.CoreMedia.CMTimeGetSeconds
import platform.CoreMedia.CMTimeMakeWithSeconds
import platform.darwin.NSEC_PER_SEC

@OptIn(ExperimentalForeignApi::class)
fun Duration.asCMTime(): CValue<CMTime> = CMTimeMakeWithSeconds(
  seconds = toDouble(DurationUnit.SECONDS),
  preferredTimescale = NSEC_PER_SEC.toInt(),
)

@OptIn(ExperimentalForeignApi::class)
fun Long.asCMTimeFromMillis(): CValue<CMTime> = milliseconds.asCMTime()

@OptIn(ExperimentalForeignApi::class)
fun Double.asCMTimeSeconds(): CValue<CMTime> = CMTimeMakeWithSeconds(this, NSEC_PER_SEC.toInt())

@OptIn(ExperimentalForeignApi::class)
val CValue<CMTime>.seconds: Duration get() = CMTimeGetSeconds(this).seconds

@OptIn(ExperimentalForeignApi::class)
val ZERO_CM_TIME: CValue<CMTime> get() = CMTimeMakeWithSeconds(0.0, NSEC_PER_SEC.toInt())
