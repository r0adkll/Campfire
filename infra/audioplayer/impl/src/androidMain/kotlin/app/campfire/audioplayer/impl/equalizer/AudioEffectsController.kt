// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.impl.equalizer

import android.media.audiofx.AudioEffect
import android.media.audiofx.BassBoost
import android.media.audiofx.DynamicsProcessing
import android.media.audiofx.LoudnessEnhancer
import app.campfire.audioplayer.impl.util.AUDIO_TAG
import app.campfire.core.audio.EqualizerBands
import app.campfire.core.audio.EqualizerProfile
import app.campfire.core.logging.Cork
import app.campfire.crashreporting.CrashReporter
import kotlin.math.roundToInt

/**
 * Owns the `android.media.audiofx` effects that implement the equalizer on a single audio
 * session: a [DynamicsProcessing] pre-EQ for the band faders, a [LoudnessEnhancer] for the
 * loudness slider, and a [BassBoost] for the bass slider.
 *
 * Effects attach to the ExoPlayer audio session id delivered by
 * `Player.Listener.onAudioSessionIdChanged` and must be re-created whenever it changes.
 * Vendor DSP implementations can reject any of these effects at construction or
 * configuration time, so every interaction is guarded — a failing effect is dropped in
 * isolation and never interrupts playback.
 */
internal class AudioEffectsController : Cork {

  override val tag: String = AUDIO_TAG
  override val enabled: Boolean = true

  // Strong references are required: audiofx effects detach when garbage collected.
  private var dynamicsProcessing: DynamicsProcessing? = null
  private var loudnessEnhancer: LoudnessEnhancer? = null
  private var bassBoost: BassBoost? = null
  private var attachedSessionId: Int? = null

  private val hasEffects: Boolean
    get() = dynamicsProcessing != null || loudnessEnhancer != null || bassBoost != null

  /**
   * Binds this controller to [audioSessionId], replacing any previous session. Effects are
   * only created while the profile is enabled, so listeners who never touch the equalizer
   * pay no DSP cost.
   */
  fun attach(audioSessionId: Int, profile: EqualizerProfile) {
    release()
    attachedSessionId = audioSessionId
    if (profile.enabled) {
      createEffects(audioSessionId)
      apply(profile)
    }
  }

  private fun createEffects(audioSessionId: Int) {
    dbark { "Attaching audio effects to session $audioSessionId" }

    dynamicsProcessing = guarded("DynamicsProcessing.create") {
      val config = DynamicsProcessing.Config.Builder(
        DynamicsProcessing.VARIANT_FAVOR_FREQUENCY_RESOLUTION,
        CHANNEL_COUNT,
        true, // preEqInUse
        EqualizerBands.BAND_COUNT,
        false, // mbcInUse
        0,
        false, // postEqInUse
        0,
        false, // limiterInUse
      )
        .setPreferredFrameDuration(PREFERRED_FRAME_DURATION_MS)
        .build()
      DynamicsProcessing(EFFECT_PRIORITY, audioSessionId, config)
    }
    loudnessEnhancer = guarded("LoudnessEnhancer.create") {
      LoudnessEnhancer(audioSessionId)
    }
    bassBoost = guarded("BassBoost.create") {
      BassBoost(EFFECT_PRIORITY, audioSessionId)
    }
  }

  /**
   * Pushes [profile] to the effects, lazily creating them the first time the profile is
   * enabled on the bound session. No-ops when no session is bound.
   */
  fun apply(profile: EqualizerProfile) {
    if (!hasEffects) {
      val sessionId = attachedSessionId
      if (!profile.enabled || sessionId == null) return
      createEffects(sessionId)
    }

    dynamicsProcessing?.let { dp ->
      dynamicsProcessing = guardedOrDrop("DynamicsProcessing.apply", dp) {
        profile.bandGainsDb.take(EqualizerBands.BAND_COUNT).forEachIndexed { index, gainDb ->
          val band = DynamicsProcessing.EqBand(
            true,
            EqualizerBands.centerFrequenciesHz[index].toFloat(),
            gainDb.coerceIn(EqualizerBands.BandGainRangeDb),
          )
          dp.setPreEqBandAllChannelsTo(index, band)
        }
        dp.setEnabled(profile.enabled)
      }
    }

    loudnessEnhancer?.let { loudness ->
      loudnessEnhancer = guardedOrDrop("LoudnessEnhancer.apply", loudness) {
        val gainDb = profile.loudnessGainDb.coerceIn(EqualizerBands.LoudnessGainRangeDb)
        loudness.setTargetGain((gainDb * MILLIBELS_PER_DB).roundToInt())
        loudness.setEnabled(profile.enabled && gainDb > 0f)
      }
    }

    bassBoost?.let { bass ->
      bassBoost = guardedOrDrop("BassBoost.apply", bass) {
        val strength = profile.bassBoost.coerceIn(EqualizerBands.BassBoostRange)
        if (bass.strengthSupported) {
          bass.setStrength((strength * MAX_BASS_STRENGTH).roundToInt().toShort())
        }
        bass.setEnabled(profile.enabled && strength > 0f)
      }
    }
  }

  fun release() {
    dynamicsProcessing?.releaseQuietly()
    loudnessEnhancer?.releaseQuietly()
    bassBoost?.releaseQuietly()
    dynamicsProcessing = null
    loudnessEnhancer = null
    bassBoost = null
    attachedSessionId = null
  }

  private inline fun <T : AudioEffect> guarded(operation: String, block: () -> T): T? {
    return try {
      block()
    } catch (e: RuntimeException) {
      wbark(e) { "Audio effect failed: $operation" }
      CrashReporter.record(e)
      null
    }
  }

  /**
   * Runs [block] against [effect], dropping (releasing and returning null for) the effect if
   * the platform DSP rejects it so a broken effect can't repeatedly throw.
   */
  private inline fun <T : AudioEffect> guardedOrDrop(operation: String, effect: T, block: () -> Unit): T? {
    return try {
      block()
      effect
    } catch (e: RuntimeException) {
      wbark(e) { "Audio effect failed: $operation" }
      CrashReporter.record(e)
      effect.releaseQuietly()
      null
    }
  }

  private fun AudioEffect.releaseQuietly() {
    try {
      release()
    } catch (e: RuntimeException) {
      wbark(e) { "Audio effect failed to release" }
    }
  }

  companion object {
    private const val EFFECT_PRIORITY = 0
    private const val CHANNEL_COUNT = 2
    private const val PREFERRED_FRAME_DURATION_MS = 10f
    private const val MILLIBELS_PER_DB = 100f
    private const val MAX_BASS_STRENGTH = 1000f
  }
}
