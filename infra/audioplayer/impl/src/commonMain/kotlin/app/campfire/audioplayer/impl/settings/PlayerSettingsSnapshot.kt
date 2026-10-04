// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.impl.settings

import app.campfire.core.app.AppInitializer
import app.campfire.core.audio.EqualizerProfile
import app.campfire.core.di.AppScope
import app.campfire.core.di.qualifier.ForScope
import app.campfire.settings.api.EqualizerSettings
import app.campfire.settings.api.PlaybackSettings
import app.campfire.settings.api.SleepSettings
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * The settings players, the media session and the sleep timer read while handling a callback, which can't wait
 * for a setting to be read. They're kept current in the background: [PlayerSettingsInitializer] creates this at
 * startup so the stored values are in before the first player needs them, and until then each holds the
 * setting's default.
 */
@SingleIn(AppScope::class)
@Inject
class PlayerSettingsSnapshot(
  playbackSettings: PlaybackSettings,
  equalizerSettings: EqualizerSettings,
  sleepSettings: SleepSettings,
  @ForScope(AppScope::class) private val scope: CoroutineScope,
) {

  val forwardTime: StateFlow<Duration> = playbackSettings.observeForwardTimeMs()
    .map { it.milliseconds }
    .current(PlaybackSettings.DefaultForwardTime)

  val backwardTime: StateFlow<Duration> = playbackSettings.observeBackwardTimeMs()
    .map { it.milliseconds }
    .current(PlaybackSettings.DefaultBackwardTime)

  val trackResetThreshold: StateFlow<Duration> = playbackSettings.observeTrackResetThreshold()
    .current(PlaybackSettings.DefaultTrackResetThreshold)

  val playbackRates: StateFlow<List<Float>> = playbackSettings.observePlaybackRates()
    .current(PlaybackSettings.DefaultPlaybackRates)

  val remoteNextPrevSkipsChapters: StateFlow<Boolean> = playbackSettings.observeRemoteNextPrevSkipsChapters()
    .current(PlaybackSettings.DEFAULT_REMOTE_NEXT_PREV_SKIPS_CHAPTERS)

  val mp3IndexSeeking: StateFlow<Boolean> = playbackSettings.observeMp3IndexSeeking()
    .current(PlaybackSettings.DEFAULT_MP3_INDEX_SEEKING)

  /** The equalizer profile for items without a per-item override. */
  val equalizerProfile: StateFlow<EqualizerProfile> = equalizerSettings.observeEqualizerProfile()
    .current(EqualizerProfile())

  val autoSleepTimerEnabled: StateFlow<Boolean> = sleepSettings.observeAutoSleepTimerEnabled()
    .current(SleepSettings.DEFAULT_AUTO_SLEEP_TIMER_ENABLED)

  val autoSleepTimer: StateFlow<SleepSettings.AutoSleepTimer> = sleepSettings.observeAutoSleepTimer()
    .current(SleepSettings.AutoSleepTimer.Default)

  val autoSleepStart = sleepSettings.observeAutoSleepStart().current(SleepSettings.DefaultAutoSleepStart)

  val autoSleepEnd = sleepSettings.observeAutoSleepEnd().current(SleepSettings.DefaultAutoSleepEnd)

  val shakeToResetEnabled: StateFlow<Boolean> = sleepSettings.observeShakeToResetEnabled()
    .current(SleepSettings.DEFAULT_SHAKE_TO_RESET_ENABLED)

  val shakeSensitivity: StateFlow<SleepSettings.ShakeSensitivity> = sleepSettings.observeShakeSensitivity()
    .current(SleepSettings.ShakeSensitivity.Default)

  val autoRewindEnabled: StateFlow<Boolean> = sleepSettings.observeAutoRewindEnabled()
    .current(SleepSettings.DEFAULT_AUTO_REWIND_ENABLED)

  val autoRewindAmount: StateFlow<Duration> = sleepSettings.observeAutoRewindAmount()
    .current(SleepSettings.DefaultAutoRewindAmount)

  val fadeOutDuration: StateFlow<Duration> = sleepSettings.observeFadeOutDuration()
    .current(SleepSettings.DefaultFadeOutDuration)

  private fun <T> Flow<T>.current(default: T): StateFlow<T> = stateIn(scope, SharingStarted.Eagerly, default)
}

/** Creates the [PlayerSettingsSnapshot] at startup, so its values are read before the first player is. */
@ContributesIntoSet(AppScope::class, binding = binding<AppInitializer>())
@Inject
class PlayerSettingsInitializer(
  private val snapshot: Lazy<PlayerSettingsSnapshot>,
) : AppInitializer {
  override val priority: Int = AppInitializer.HIGHEST_PRIORITY

  override suspend fun onInitialize() {
    snapshot.value
  }
}
