// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.engine

import app.campfire.audioplayer.impl.settings.PlayerSettingsSnapshot
import app.campfire.settings.api.EqualizerSettings
import app.campfire.settings.api.PlaybackSettings
import app.campfire.settings.api.SleepSettings
import app.campfire.settings.test.FakeEqualizerSettings
import app.campfire.settings.test.FakePlaybackSettings
import app.campfire.settings.test.FakeSleepSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

/** A [PlayerSettingsSnapshot] over fakes that follows their changes as soon as they're made. */
internal fun testPlayerSettings(
  playback: PlaybackSettings = FakePlaybackSettings(),
  equalizer: EqualizerSettings = FakeEqualizerSettings(),
  sleep: SleepSettings = FakeSleepSettings(),
) = PlayerSettingsSnapshot(playback, equalizer, sleep, CoroutineScope(Dispatchers.Unconfined))
