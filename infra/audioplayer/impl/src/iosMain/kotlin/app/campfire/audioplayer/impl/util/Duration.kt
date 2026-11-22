// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.impl.util

import kotlin.time.Duration
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

/**
 * This time as a [Duration], or null when it isn't a finite number — an indefinite or invalid
 * `CMTime` (an item that failed to load, a live stream) reads as NaN, which [Duration] rejects.
 */
@OptIn(ExperimentalForeignApi::class)
fun CValue<CMTime>.durationOrNull(): Duration? = CMTimeGetSeconds(this)
  .takeIf { it.isFinite() }
  ?.seconds
